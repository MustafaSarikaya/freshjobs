package com.jobscout.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobscout.api.generated.model.Problem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The public site has no sign-in. Accounts exist only for the admin area
 * (ingestion runs), which needs a staff or superuser session. Everything else
 * is open, with per-visitor state kept in the HTTP session.
 */
@Configuration
public class SecurityConfig {

    private static final AntPathRequestMatcher TRIGGER_RUN =
        new AntPathRequestMatcher("/api/v1/runs", "POST");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ObjectMapper objectMapper,
                                            SecurityContextRepository securityContextRepository)
        throws Exception {
        CookieCsrfTokenRepository csrfRepository =
            CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieName("csrftoken");
        csrfRepository.setHeaderName("X-CSRFToken");
        csrfRepository.setCookieCustomizer(cookie -> cookie.path("/").httpOnly(false));

        // The SPA reads the csrftoken cookie verbatim and echoes it in
        // X-CSRFToken (Django-style double submit). Spring Security 6 defaults
        // to XorCsrfTokenRequestAttributeHandler, which expects an XOR-encoded
        // token instead and would 403 every unsafe request from the SPA.
        CsrfTokenRequestAttributeHandler csrfRequestHandler =
            new CsrfTokenRequestAttributeHandler();

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(csrfRequestHandler)
                // The scheduled ingest job authenticates with a bearer key and
                // has no CSRF cookie; admin sessions still need the token.
                .ignoringRequestMatchers(request ->
                    TRIGGER_RUN.matches(request) && hasBearerToken(request)))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .securityContext(context ->
                context.securityContextRepository(securityContextRepository))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, ex) ->
                    writeProblem(objectMapper, response, HttpStatus.UNAUTHORIZED,
                        "Unauthorized", "Authentication required"))
                .accessDeniedHandler((request, response, ex) ->
                    writeProblem(objectMapper, response, HttpStatus.FORBIDDEN,
                        "Forbidden", "Access denied")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/**").authenticated()
                // RunsController checks for an admin session or the ingest key.
                .requestMatchers(TRIGGER_RUN).permitAll()
                .requestMatchers("/api/v1/runs", "/api/v1/runs/**", "/api/v1/admin/**")
                    .hasAnyRole("STAFF", "SUPERUSER")
                .anyRequest().permitAll())
            .addFilterAfter(csrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
        throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new DjangoPbkdf2PasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private static boolean hasBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ");
    }

    private static OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                throws ServletException, IOException {
                CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrfToken != null) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private static void writeProblem(ObjectMapper objectMapper,
                                     HttpServletResponse response,
                                     HttpStatus status,
                                     String title,
                                     String detail) throws IOException {
        Problem problem = new Problem(URI.create("about:blank"), title, status.value());
        problem.setDetail(detail);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}

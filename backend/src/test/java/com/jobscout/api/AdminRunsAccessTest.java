package com.jobscout.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobscout.domain.Run;
import com.jobscout.ingestion.AsyncIngestionRunner;
import com.jobscout.repository.RunRepository;
import com.jobscout.security.SecurityConfig;
import com.jobscout.session.SessionPreferences;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {RunsController.class, SavedViewsController.class})
@Import({SecurityConfig.class, com.jobscout.service.SavedViewService.class, SessionPreferences.class})
@TestPropertySource(properties = "jobscout.ingest-api-key=secret-key")
class AdminRunsAccessTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RunRepository runs;

    @MockitoBean
    AsyncIngestionRunner runner;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousCannotListRuns() throws Exception {
        mockMvc.perform(get("/api/v1/runs")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminSessionCannotListRuns() throws Exception {
        mockMvc.perform(get("/api/v1/runs").session(sessionWithRoles()))
            .andExpect(status().isForbidden());
    }

    @Test
    void staffCanListRuns() throws Exception {
        when(runs.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/runs").session(sessionWithRoles("ROLE_STAFF")))
            .andExpect(status().isOk());
    }

    @Test
    void superuserCanListRuns() throws Exception {
        when(runs.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/runs").session(sessionWithRoles("ROLE_SUPERUSER")))
            .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotGetARun() throws Exception {
        mockMvc.perform(get("/api/v1/runs/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotTriggerRunWithoutKey() throws Exception {
        // No bearer token means no CSRF exemption, so CSRF rejects it first.
        mockMvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void wrongIngestKeyCannotTriggerRun() throws Exception {
        mockMvc.perform(post("/api/v1/runs")
                .header("Authorization", "Bearer wrong-key")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void ingestKeyCanTriggerRun() throws Exception {
        when(runs.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/runs")
                .header("Authorization", "Bearer secret-key")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isAccepted());
    }

    @Test
    void adminSessionCanTriggerRun() throws Exception {
        when(runs.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/runs")
                .session(sessionWithRoles("ROLE_STAFF"))
                .cookie(new Cookie("csrftoken", "t"))
                .header("X-CSRFToken", "t")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isAccepted());
    }

    @Test
    void adminSessionTriggerStillRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/runs")
                .session(sessionWithRoles("ROLE_STAFF"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminSessionCannotTriggerRun() throws Exception {
        mockMvc.perform(post("/api/v1/runs")
                .session(sessionWithRoles())
                .cookie(new Cookie("csrftoken", "t"))
                .header("X-CSRFToken", "t")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsStayOpen() throws Exception {
        mockMvc.perform(get("/api/v1/views")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/views")
                .cookie(new Cookie("csrftoken", "t"))
                .header("X-CSRFToken", "t")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"a\",\"columns\":[{\"id\":\"title\",\"visible\":true}]}"))
            .andExpect(status().isCreated());
    }

    private static MockHttpSession sessionWithRoles(String... roles) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("admin", "n/a", roles);
        auth.setAuthenticated(true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}

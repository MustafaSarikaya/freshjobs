package com.jobscout.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobscout.security.SecurityConfig;
import com.jobscout.service.SavedViewService;
import com.jobscout.session.SessionPreferences;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SavedViewsController.class)
@Import({SecurityConfig.class, SavedViewService.class, SessionPreferences.class})
class SavedViewsSessionTest {

    private static final String BODY = """
        {"name":"Remote","columns":[{"id":"title","visible":true}]}
        """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousRequestsCanListViews() throws Exception {
        mockMvc.perform(get("/api/v1/views"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void viewsAreScopedToTheHttpSession() throws Exception {
        MockHttpSession mine = new MockHttpSession();
        MockHttpSession theirs = new MockHttpSession();

        mockMvc.perform(withCsrf(post("/api/v1/views").session(mine)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/views").session(mine))
            .andExpect(jsonPath("$[0].name").value("Remote"));
        mockMvc.perform(get("/api/v1/views").session(theirs))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void csrfRejectsWhenHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/views")
                .cookie(new Cookie("csrftoken", "any"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isForbidden());
    }

    @Test
    void csrfRejectsWhenHeaderDoesNotMatchCookie() throws Exception {
        mockMvc.perform(post("/api/v1/views")
                .cookie(new Cookie("csrftoken", "expected"))
                .header("X-CSRFToken", "different")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isForbidden());
    }

    // The SPA echoes the raw csrftoken cookie value in X-CSRFToken
    // (Django-style double submit), so the raw value must be accepted.
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
        withCsrf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) {
        return req.cookie(new Cookie("csrftoken", "token"))
            .header("X-CSRFToken", "token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY);
    }
}

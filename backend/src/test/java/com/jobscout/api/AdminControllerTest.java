package com.jobscout.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobscout.domain.LocationTag;
import com.jobscout.domain.Platform;
import com.jobscout.domain.Source;
import com.jobscout.repository.LocationTagRepository;
import com.jobscout.repository.SourceRepository;
import com.jobscout.security.SecurityConfig;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    private static final String SOURCE_BODY =
        "{\"name\":\"Acme\",\"platform\":\"GREENHOUSE\",\"boardId\":\"acme\",\"isActive\":true}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SourceRepository sources;

    @MockitoBean
    LocationTagRepository locations;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(csrf(post("/api/v1/admin/sources")).content(SOURCE_BODY))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(csrf(post("/api/v1/admin/sources")).session(session()).content(SOURCE_BODY))
            .andExpect(status().isForbidden());
    }

    @Test
    void createsSource() throws Exception {
        when(sources.save(any(Source.class))).thenAnswer(inv -> {
            Source s = inv.getArgument(0);
            s.setId(7L);
            return s;
        });

        mockMvc.perform(csrf(post("/api/v1/admin/sources")).session(admin()).content(SOURCE_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.platform").value("GREENHOUSE"))
            .andExpect(jsonPath("$.boardId").value("acme"));
    }

    @Test
    void createRejectsDuplicateBoard() throws Exception {
        when(sources.findByPlatformAndBoardId(Platform.GREENHOUSE, "acme"))
            .thenReturn(Optional.of(source(3L)));

        mockMvc.perform(csrf(post("/api/v1/admin/sources")).session(admin()).content(SOURCE_BODY))
            .andExpect(status().isConflict());
        verify(sources, never()).save(any());
    }

    @Test
    void createValidatesBody() throws Exception {
        mockMvc.perform(csrf(post("/api/v1/admin/sources")).session(admin())
                .content("{\"name\":\"\",\"platform\":\"GREENHOUSE\",\"boardId\":\"acme\",\"isActive\":true}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updatesSource() throws Exception {
        Source existing = source(3L);
        when(sources.findById(3L)).thenReturn(Optional.of(existing));
        when(sources.findByPlatformAndBoardId(Platform.GREENHOUSE, "acme"))
            .thenReturn(Optional.of(existing));
        when(sources.save(any(Source.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(csrf(put("/api/v1/admin/sources/3")).session(admin())
                .content("{\"name\":\"Acme Inc\",\"platform\":\"GREENHOUSE\",\"boardId\":\"acme\",\"isActive\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Acme Inc"))
            .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void updateRejectsBoardOwnedByAnotherSource() throws Exception {
        when(sources.findById(3L)).thenReturn(Optional.of(source(3L)));
        when(sources.findByPlatformAndBoardId(Platform.GREENHOUSE, "acme"))
            .thenReturn(Optional.of(source(4L)));

        mockMvc.perform(csrf(put("/api/v1/admin/sources/3")).session(admin()).content(SOURCE_BODY))
            .andExpect(status().isConflict());
    }

    @Test
    void updateUnknownSourceIsNotFound() throws Exception {
        when(sources.findById(9L)).thenReturn(Optional.empty());

        mockMvc.perform(csrf(put("/api/v1/admin/sources/9")).session(admin()).content(SOURCE_BODY))
            .andExpect(status().isNotFound());
    }

    @Test
    void deletesSource() throws Exception {
        Source existing = source(3L);
        when(sources.findById(3L)).thenReturn(Optional.of(existing));

        mockMvc.perform(csrf(delete("/api/v1/admin/sources/3")).session(admin()))
            .andExpect(status().isNoContent());
        verify(sources).delete(existing);
    }

    @Test
    void deleteUnknownSourceIsNotFound() throws Exception {
        when(sources.findById(9L)).thenReturn(Optional.empty());

        mockMvc.perform(csrf(delete("/api/v1/admin/sources/9")).session(admin()))
            .andExpect(status().isNotFound());
    }

    @Test
    void updatesLocationTagAndMarksItGeocoded() throws Exception {
        LocationTag tag = new LocationTag();
        tag.setId(5L);
        tag.setName("Toronto, ON");
        tag.setGeocodingFailures(3);
        when(locations.findById(5L)).thenReturn(Optional.of(tag));
        when(locations.save(any(LocationTag.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(csrf(put("/api/v1/admin/locations/5")).session(admin())
                .content("{\"countryCode\":\"ca\",\"regionCode\":\"CA-ON\",\"city\":\" Toronto \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.countryCode").value("CA"))
            .andExpect(jsonPath("$.city").value("Toronto"))
            .andExpect(jsonPath("$.geoKey").value("CA-ON-Toronto"));

        ArgumentCaptor<LocationTag> saved = ArgumentCaptor.forClass(LocationTag.class);
        verify(locations).save(saved.capture());
        assertThat(saved.getValue().getGeocodedAt()).isNotNull();
        assertThat(saved.getValue().getGeocodingFailures()).isZero();
    }

    @Test
    void blankLocationFieldsAreCleared() throws Exception {
        LocationTag tag = new LocationTag();
        tag.setId(5L);
        tag.setName("Remote");
        tag.setCountryCode("US");
        when(locations.findById(5L)).thenReturn(Optional.of(tag));
        when(locations.save(any(LocationTag.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(csrf(put("/api/v1/admin/locations/5")).session(admin())
                .content("{\"countryCode\":null,\"regionCode\":\"\",\"city\":null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.countryCode").doesNotExist())
            .andExpect(jsonPath("$.regionCode").doesNotExist());

        // The geocoder picks tags with no country; a manual clear must stick.
        ArgumentCaptor<LocationTag> saved = ArgumentCaptor.forClass(LocationTag.class);
        verify(locations).save(saved.capture());
        assertThat(saved.getValue().getGeocodingFailures()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void updateUnknownLocationIsNotFound() throws Exception {
        when(locations.findById(9L)).thenReturn(Optional.empty());

        mockMvc.perform(csrf(put("/api/v1/admin/locations/9")).session(admin()).content("{}"))
            .andExpect(status().isNotFound());
    }

    private static Source source(Long id) {
        Source s = new Source();
        s.setId(id);
        s.setName("Acme");
        s.setPlatform(Platform.GREENHOUSE);
        s.setBoardId("acme");
        return s;
    }

    private static MockHttpServletRequestBuilder csrf(MockHttpServletRequestBuilder req) {
        return req.cookie(new Cookie("csrftoken", "t"))
            .header("X-CSRFToken", "t")
            .contentType(MediaType.APPLICATION_JSON);
    }

    private static MockHttpSession admin() {
        return session("ROLE_STAFF");
    }

    private static MockHttpSession session(String... roles) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("u", "n/a", roles);
        auth.setAuthenticated(true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}

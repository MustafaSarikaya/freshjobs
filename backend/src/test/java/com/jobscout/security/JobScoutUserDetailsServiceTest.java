package com.jobscout.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jobscout.domain.User;
import com.jobscout.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class JobScoutUserDetailsServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final JobScoutUserDetailsService service = new JobScoutUserDetailsService(users);

    @Test
    void loadsStaffUsers() {
        when(users.findByUsername("staff")).thenReturn(Optional.of(user("staff", true, false)));

        assertThat(service.loadUserByUsername("staff").getAuthorities())
            .extracting(Object::toString)
            .containsExactly("ROLE_STAFF");
    }

    @Test
    void loadsSuperusers() {
        when(users.findByUsername("root")).thenReturn(Optional.of(user("root", false, true)));

        assertThat(service.loadUserByUsername("root").getUsername()).isEqualTo("root");
    }

    @Test
    void rejectsNonAdminUsers() {
        when(users.findByUsername("plain")).thenReturn(Optional.of(user("plain", false, false)));

        assertThatThrownBy(() -> service.loadUserByUsername("plain"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsUnknownUsers() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    private static User user(String username, boolean staff, boolean superuser) {
        User user = new User();
        user.setUsername(username);
        user.setStaff(staff);
        user.setSuperuser(superuser);
        user.setActive(true);
        return user;
    }
}

package com.jobscout.security;

import com.jobscout.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JobScoutUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public JobScoutUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Accounts exist only for the admin area, so non-admins cannot sign in.
        return users.findByUsername(username)
            .filter(user -> user.isStaff() || user.isSuperuser())
            .map(JobScoutUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));
    }
}

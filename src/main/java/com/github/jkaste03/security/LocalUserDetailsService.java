package com.github.jkaste03.security;

import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.UserRepository;

/**
 * Loads users from the local database for form login and JWT authentication.
 */
@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public LocalUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));

        GrantedAuthority authority = new SimpleGrantedAuthority(AppRoles.ROLE_PREFIX + user.getRole().getName());
        return new UserPrincipal(user.getId(), user.getEmail(), user.getUsername(),
                Collections.singletonList(authority), user.getPassword());
    }

    /**
     * Convenience loader that avoids UsernameNotFoundException by returning null.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        return userRepository.findById(id)
                .map(user -> {
                    GrantedAuthority authority = new SimpleGrantedAuthority(AppRoles.ROLE_PREFIX + user.getRole().getName());
                    return new UserPrincipal(user.getId(), user.getEmail(), user.getUsername(),
                            Collections.singletonList(authority), user.getPassword());
                })
                .orElse(null);
    }
}

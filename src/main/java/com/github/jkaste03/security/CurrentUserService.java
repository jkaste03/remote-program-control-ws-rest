package com.github.jkaste03.security;

import java.util.Set;
import java.util.Collections;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.github.jkaste03.model.Role;

@Component
public class CurrentUserService {

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public Set<Role> getRoles() {
        Object details = getAuthentication().getDetails();
        if (details instanceof UserDetailsImpl ud) {
            return ud.getRoles();
        }
        return Collections.emptySet();
    }
}

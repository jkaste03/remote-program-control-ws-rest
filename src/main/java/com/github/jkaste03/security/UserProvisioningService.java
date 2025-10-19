// com.github.jkaste03.security.UserProvisioningService
package com.github.jkaste03.security;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.UserRepository;

@Service
public class UserProvisioningService {

    private final UserRepository userRepository;

    public UserProvisioningService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User ensureUserExists(String email, String firstName, String lastName) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        User u = new User();
        u.setEmail(email);
        u.setFirstname(firstName);
        u.setLastname(lastName);
        return userRepository.save(u);
    }
}

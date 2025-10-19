// com.github.jkaste03.security.UserProvisioningService
package com.github.jkaste03.security;

import java.util.Objects;
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
			User user = existing.get();
			boolean updated = false;
			if (!Objects.equals(firstName, user.getFirstName())) {
				user.setFirstName(firstName);
				updated = true;
			}
			if (!Objects.equals(lastName, user.getLastName())) {
				user.setLastName(lastName);
				updated = true;
			}
			return updated ? userRepository.save(user) : user;
		}
		User user = new User(email, firstName, lastName);
		return userRepository.save(user);
	}
}

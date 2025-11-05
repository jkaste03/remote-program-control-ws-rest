package com.github.jkaste03.security;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.repository.UserRepository;

@Service
public class UserProvisioningService {

	private final UserRepository userRepo;
	private final RoleRepository roleRepo;
	private static final String DEFAULT_ROLE_NAME = "USER";

	public UserProvisioningService(UserRepository userRepo, RoleRepository roleRepo) {
		this.userRepo = userRepo;
		this.roleRepo = roleRepo;
	}

	@Transactional
	public User ensureUserFromJwt(String issuer,
			String subject,
			String username,
			String email) {

		// 1) Primært: stabil Keycloak-identitet
		Optional<User> maybeUser = userRepo.findByAuthProviderAndAuthSubject(issuer, subject);
		User user;
		if (maybeUser.isPresent()) {
			user = maybeUser.get();
		} else {
			// 2) Opprett ny bruker hvis vi ikke fant noen
			Role role = roleRepo.findByName(DEFAULT_ROLE_NAME)
					.orElseThrow(() -> new IllegalStateException("Default role USER not found"));
			user = new User(email, username, issuer, subject, role);
			return userRepo.save(user);
		}

		// 4) Oppdater email/username hvis de faktisk har endret seg
		if (email != null && !email.equals(user.getEmail())) {
			user.setEmail(email);
		}
		if (username != null && !username.equals(user.getUsername())) {
			user.setUsername(username);
		}

		// eksisterende bruker er allerede managed pga @Transactional
		return user;
	}
}

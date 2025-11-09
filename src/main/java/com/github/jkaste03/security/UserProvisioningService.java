package com.github.jkaste03.security;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.repository.UserRepository;
import com.github.jkaste03.exceptions.ResourceNotFoundException;

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

		// Primært: stabil Keycloak-identitet
		User user = userRepo.findByAuthProviderAndAuthSubject(issuer, subject)
				.orElseGet(() -> tryFindExistingByEmail(email, issuer, subject)
						.orElseGet(() -> createNewUser(email, username, issuer, subject)));
		// Oppdater email/username hvis de faktisk har endret seg
		if (email != null && !email.equals(user.getEmail())) {
			user.setEmail(email);
		}
		if (username != null && !username.equals(user.getUsername())) {
			user.setUsername(username);
		}
		user.getRole().getName(); // Tving innlasting av rolle (for sikkerhetskontekst)
		return user;
	}

	private Optional<User> tryFindExistingByEmail(String email, String issuer, String subject) {
		if (email == null) {
			return Optional.empty();
		}
		return userRepo.findByEmail(email).map(existing -> {
			existing.linkIdentityIfMissing(issuer, subject);
			return existing;
		});
	}

	private User createNewUser(String email, String username, String issuer, String subject) {
		Role role = roleRepo.findByName(DEFAULT_ROLE_NAME)
				.orElseThrow(() -> new ResourceNotFoundException("problem.not-found.role", DEFAULT_ROLE_NAME));
		User user = new User(email, username, issuer, subject, role);
		return userRepo.save(user);
	}
}

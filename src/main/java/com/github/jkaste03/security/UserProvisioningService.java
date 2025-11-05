package com.github.jkaste03.security;

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
		User user = userRepo
				.findByAuthProviderAndAuthSubject(issuer, subject)
				.orElseGet(() -> userRepo.findByEmail(email).orElse(null));

		boolean isNew = false;

		// 2) Opprett ny bruker hvis vi ikke fant noen
		if (user == null) {
			Role role = roleRepo.findByName(DEFAULT_ROLE_NAME)
					.orElseThrow(() -> new IllegalStateException("Default role USER not found"));
			user = new User(email, username, role);
			isNew = true;
		}

		// 3) Sørg for at Keycloak-identitet er linket (idempotent)
		user.linkIdentityIfMissing(issuer, subject);

		// 4) Oppdater email/username hvis de faktisk har endret seg
		if (email != null && !email.equals(user.getEmail())) {
			user.setEmail(email);
		}
		if (username != null && !username.equals(user.getUsername())) {
			user.setUsername(username);
		}

		if (isNew) {
			return userRepo.save(user);
		}
		// eksisterende bruker er allerede managed pga @Transactional
		return user;
	}
}

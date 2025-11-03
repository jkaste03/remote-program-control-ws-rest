package com.github.jkaste03.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.repository.UserRepository;

@Service
public class UserProvisioningService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private static final String DEFAULT_ROLE_NAME = "USER";

	public UserProvisioningService(UserRepository userRepository, RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
	}

	/**
	 * Provisionér/sync bruker ved innlogging.
	 *
	 * @param issuer          JWT iss (Keycloak realm issuer URL)
	 * @param subject         JWT sub (Keycloak user-id, stabil)
	 * @param username        Keycloak preferred_username (kan endres, men ofte
	 *                        stabil)
	 * @param email           e-post (kan endres)
	 * @param first           fornavn
	 * @param last            etternavn
	 * @param primaryRoleName f.eks. "ADMIN" eller "USER" (én rolle per bruker)
	 */
	@Transactional
	public User ensureUserFromJwt(
			String issuer,
			String subject,
			String username,
			String email) {
		// 1) Primært oppslag: stabil IdP-identitet
		User user = userRepository.findByAuthProviderAndAuthSubject(issuer, subject).orElse(null);

		if (user == null) {
			user = userRepository.findByEmail(email).orElse(null);
		}

		boolean isNew = false;

		// 3) Opprett ny hvis ingen funnet
		if (user == null) {
			Role role = roleRepository.findByName(DEFAULT_ROLE_NAME)
					.orElseThrow(() -> new IllegalStateException("Default role USER not found"));
			user = new User(email, username, role);
			isNew = true;
		}

		// 4) Link stabil identitet hvis mangler
		user.linkIdentityIfMissing(issuer, subject);

		// E-post: unik, men kan endres. Sett hvis endret og ikke i konflikt.
		if (!user.getEmail().equals(email)) {
			user.setEmail(email);
		}

		// Username: unik, kan endres. Sett hvis endret og ikke i konflikt.
		if (!user.getUsername().equals(username)) {
			user.setUsername(username);
		}

		// 6) Persist
		if (isNew) {
			return userRepository.save(user);
		}
		return user;
	}
}

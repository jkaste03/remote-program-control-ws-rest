package com.github.jkaste03.security;

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

	/**
	 * Sjekker om bruker finnes basert på ID fra token (UUID).
	 * Hvis ikke, opprettes brukeren "just-in-time".
	 */
	@Transactional
	public User getOrCreateUser(UserPrincipal principal) {
		return userRepo.findById(principal.id())
				.orElseGet(() -> createUser(principal));
	}

	private User createUser(UserPrincipal principal) {
		Role role = roleRepo.findByName(DEFAULT_ROLE_NAME)
				.orElseThrow(() -> new ResourceNotFoundException("problem.not-found.role", DEFAULT_ROLE_NAME));

		// Oppretter User med ID (UUID) direkte fra tokenet
		User user = new User(principal.id(), principal.email(), principal.username(), role);
		return userRepo.save(user);
	}
}

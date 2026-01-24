package com.github.jkaste03.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
	private static final String DEFAULT_ROLE_NAME = AppRoles.USER;

	/*
	 * Vi trenger en referanse til "oss selv" (proxyen) for å kunne kalle
	 * metoder med @Transactional(REQUIRES_NEW) fra innsiden av klassen.
	 * 
	 * @Lazy bryter den sirkulære avhengigheten som ellers ville oppstått.
	 */
	@Autowired
	@Lazy
	private UserProvisioningService self;

	public UserProvisioningService(UserRepository userRepo, RoleRepository roleRepo) {
		this.userRepo = userRepo;
		this.roleRepo = roleRepo;
	}

	/**
	 * Sjekker om bruker finnes basert på ID fra token (UUID).
	 * Hvis ikke, prøver den å opprette brukeren i en isolert transaksjon.
	 * Dette håndterer både "Race Conditions" og sikrer at brukeren blir
	 * lagret selv om operasjonen som kalte oss (f.eks. lagre Device) feiler senere.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW) // Sørger for at bruker lagres hvis device ikke lagres
	public User getOrCreateUser(UserPrincipal principal) {
		// 1. Prøv raskt på ID
		return userRepo.findById(principal.id())
				// 2. Fallback på username/email for å unngå å prøve insert hvis de allerede finnes
				.or(() -> userRepo.findByUsername(principal.username()))
				.or(() -> userRepo.findByEmail(principal.email()))
				.orElseGet(() -> {
					try {
						// VIKTIG: Kaller via 'self' for at transaksjonen skal virke
						return self.createUserInNewTransaction(principal);
					} catch (DataIntegrityViolationException e) {
						// 3. Race condition eller unik-kollisjon: prøv alle kjente nøkler før vi gir opp
						return userRepo.findById(principal.id())
								.or(() -> userRepo.findByUsername(principal.username()))
								.or(() -> userRepo.findByEmail(principal.email()))
								.orElseThrow(() -> new RuntimeException(
										"Unexpected race condition: User creation failed but user still not found", e));
					}
				});
	}

	/**
	 * Denne metoden kjører i sin EGEN transaksjon (REQUIRES_NEW).
	 * Hvis lagring feiler (f.eks. pga unik-indeks konflikt), rulles KUN
	 * denne metoden tilbake, og vi kan fange unntaket i metoden over.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public User createUserInNewTransaction(UserPrincipal principal) {
		// Hvis noen har samme ID/username/email allerede, returner den for å unngå unik-kollisjon
		User existingById = userRepo.findById(principal.id()).orElse(null);
		if (existingById != null) {
			return existingById;
		}

		User existingByUsername = userRepo.findByUsername(principal.username()).orElse(null);
		if (existingByUsername != null) {
			return existingByUsername;
		}

		User existingByEmail = userRepo.findByEmail(principal.email()).orElse(null);
		if (existingByEmail != null) {
			return existingByEmail;
		}

		Role role = roleRepo.findByName(DEFAULT_ROLE_NAME)
				.orElseThrow(() -> new IllegalStateException("System configuration error: Required role '"
						+ DEFAULT_ROLE_NAME + "' not found in database."));

		User user = new User(principal.id(), principal.email(), principal.username(), role);

		// saveAndFlush tvinger databasen til å kjøre INSERT umiddelbart,
		// slik at vi får en eventuell kræsj her og nå (inne i try-blokken).
		return userRepo.saveAndFlush(user);
	}
}

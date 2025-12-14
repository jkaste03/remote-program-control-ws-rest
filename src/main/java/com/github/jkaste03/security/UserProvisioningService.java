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
	private static final String DEFAULT_ROLE_NAME = "USER";

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
		// 1. Prøv å finn bruker raskt
		return userRepo.findById(principal.id())
				.orElseGet(() -> {
					// 2. Hvis ikke funnet, prøv å opprette i isolert transaksjon
					try {
						// VIKTIG: Kaller via 'self' for at transaksjonen skal virke
						return self.createUserInNewTransaction(principal);
					} catch (DataIntegrityViolationException e) {
						// 3. Race condition oppstod: Noen andre opprettet brukeren akkurat nå.
						// Den indre transaksjonen rullet tilbake, men vår ytre er OK.
						// Vi prøver å hente brukeren på nytt.
						return userRepo.findById(principal.id())
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
		Role role = roleRepo.findByName(DEFAULT_ROLE_NAME)
				.orElseThrow(() -> new ResourceNotFoundException("problem.not-found.role", DEFAULT_ROLE_NAME));

		User user = new User(principal.id(), principal.email(), principal.username(), role);

		// saveAndFlush tvinger databasen til å kjøre INSERT umiddelbart,
		// slik at vi får en eventuell kræsj her og nå (inne i try-blokken).
		return userRepo.saveAndFlush(user);
	}
}

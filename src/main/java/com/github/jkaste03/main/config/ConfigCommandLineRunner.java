package com.github.jkaste03.main.config;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Device.Status;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.ProgramRepository;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.repository.UserRepository;

@Component
class ConfigCommandLineRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(ConfigCommandLineRunner.class);

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final ProgramRepository programRepository;
	private final List<String> configuredRoleNames;

	ConfigCommandLineRunner(RoleRepository roleRepository,
			UserRepository userRepository,
			ProgramRepository programRepository,
			@Value("#{'${user.resource.roles}'.split(',')}") List<String> configuredRoleNames) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.programRepository = programRepository;
		this.configuredRoleNames = configuredRoleNames;
	}

	@Override
	@Transactional
	public void run(String... args) {
		Map<String, Role> allRoles = ensureRoles();
		createPrograms(allRoles);
		createUsers(allRoles);
	}

	private Map<String, Role> ensureRoles() {
		Map<String, Role> roles = new HashMap<>();
		for (String roleName : configuredRoleNames) {
			Role role = roleRepository.findByName(roleName);
			if (role == null) {
				role = roleRepository.save(new Role(roleName));
				log.info("Created role {}", roleName);
			}
			roles.put(roleName, role);
		}
		return roles;
	}

	private void createPrograms(Map<String, Role> allRoles) {
		Map<String, Role> onlyAdmin = Map.of(
				"ADMIN", allRoles.get("ADMIN"));

		List<Program> seeds = new ArrayList<>();
		System.out.println("Roles: " + allRoles);
		addProgram("System Monitor", seeds, onlyAdmin);
		addProgram("Remote Desktop", seeds, allRoles);
		addProgram("Media Center", seeds, allRoles);
		addProgram("Smart Lighting Controller", seeds, allRoles);

		if (!seeds.isEmpty()) {
			programRepository.saveAll(seeds);
			log.info("Seeded {} default program(s)", seeds.size());
		}
	}

	private void createUsers(Map<String, Role> allRoles) {
		Map<String, Role> onlyUser = Map.of(
				"USER", allRoles.get("USER"));

		createUser("user1@email.com", "User1_Firstname", "User1_Lastname",
				onlyUser, user -> {
					user.addDevice(new Device("Living Room Media Center", user, Status.ONLINE,
							Instant.now().minus(Duration.ofHours(1)), "AA:BB:CC:DD:EE:01", "192.168.0.10"));
					user.addDevice(new Device("Bedroom Smart Speaker", user, Status.OFFLINE,
							Instant.now().minus(Duration.ofHours(6)), "AA:BB:CC:DD:EE:02", "192.168.0.11"));
				});

		createUser("user2@email.com", "User2_Firstname", "User2_Lastname",
				allRoles, user -> {
					user.addDevice(new Device("Office Workstation", user, Status.ONLINE,
							Instant.now().minus(Duration.ofMinutes(15)), "AA:BB:CC:DD:EE:10", "10.0.0.25"));
				});

		createUser("user3@email.com", "User3_Firstname", "User3_Lastname",
				onlyUser, user -> {
					user.addDevice(new Device("Garage Controller", user, Status.OFFLINE,
							Instant.now().minus(Duration.ofDays(2)), "AA:BB:CC:DD:EE:20", "172.16.0.18"));
				});
	}

	private void addProgram(String name, List<Program> accumulator, Map<String, Role> allowedRoles) {
		Program program = new Program(name);
		allowedRoles.values().forEach(program::addAllowedRole);
		accumulator.add(program);
	}

	private void createUser(String email, String firstName, String lastName,
			Map<String, Role> rolesToAssign, Consumer<User> customizer) {
		User user = new User(email, firstName, lastName);
		rolesToAssign.entrySet().forEach(entry -> user.addRole(entry.getValue()));
		customizer.accept(user);
		userRepository.save(user);
		log.info("Created default user {}", email);
	}
}

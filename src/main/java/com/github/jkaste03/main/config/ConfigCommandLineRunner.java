package com.github.jkaste03.main.config;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.ProgramRepository;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.repository.UserRepository;

@Component
class ConfigCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigCommandLineRunner.class);

    private static final List<ProgramSeed> PROGRAM_SEEDS = List.of(
            new ProgramSeed("System Monitor", "ADMIN"),
            new ProgramSeed("Remote Desktop", "USER"),
            new ProgramSeed("Media Center", "USER"),
            new ProgramSeed("Smart Lighting Controller", "USER"));

    private static final List<UserSeed> USER_SEEDS = List.of(
            new UserSeed("user1@email.com", "User1_Firstname", "User1_Lastname", Set.of("USER"),
                    List.of(
                            new DeviceSeed("Living Room Media Center", Device.Status.ONLINE, Duration.ofHours(1),
                                    "AA:BB:CC:DD:EE:01", "192.168.0.10"),
                            new DeviceSeed("Bedroom Smart Speaker", Device.Status.OFFLINE, Duration.ofHours(6),
                                    "AA:BB:CC:DD:EE:02", "192.168.0.11"))),
            new UserSeed("user2@email.com", "User2_Firstname", "User2_Lastname", Set.of("ADMIN", "USER"),
                    List.of(new DeviceSeed("Office Workstation", Device.Status.ONLINE, Duration.ofMinutes(15),
                            "AA:BB:CC:DD:EE:10", "10.0.0.25"))),
            new UserSeed("user3@email.com", "User3_Firstname", "User3_Lastname", Set.of("USER"),
                    List.of(new DeviceSeed("Garage Controller", Device.Status.OFFLINE, Duration.ofDays(2),
                            "AA:BB:CC:DD:EE:20", "172.16.0.18"))));

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final DeviceRepository deviceRepository;
    private final List<String> configuredRoleNames;
    private final boolean seedingEnabled;

    ConfigCommandLineRunner(RoleRepository roleRepository,
            UserRepository userRepository,
            ProgramRepository programRepository,
            DeviceRepository deviceRepository,
            @Value("#{'${user.resource.roles}'.split(',')}") List<String> configuredRoleNames,
            @Value("${app.seed.enabled:true}") boolean seedingEnabled) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.deviceRepository = deviceRepository;
        this.configuredRoleNames = configuredRoleNames == null ? List.of()
                : configuredRoleNames.stream()
                        .map(name -> name == null ? "" : name.trim())
                        .filter(name -> !name.isBlank())
                        .distinct()
                        .collect(Collectors.toUnmodifiableList());
        this.seedingEnabled = seedingEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedingEnabled) {
            log.info("Skipping bootstrap seeding because app.seed.enabled is false");
            return;
        }

        Map<String, Role> rolesByName = ensureRoles();
        if (rolesByName.isEmpty()) {
            log.warn("No roles configured for seeding; skipping default data bootstrap");
            return;
        }

        seedPrograms(rolesByName);
        seedUsers(rolesByName);
    }

    private Map<String, Role> ensureRoles() {
        Map<String, Role> rolesByName = new HashMap<>();
        for (String configuredName : configuredRoleNames) {
            String normalized = normalize(configuredName);
            if (normalized.isEmpty()) {
                continue;
            }
            Role role = roleRepository.findByName(configuredName);
            if (role == null) {
                role = roleRepository.save(new Role(configuredName));
                log.info("Created missing role '{}'", configuredName);
            }
            rolesByName.put(normalized, role);
        }
        if (configuredRoleNames.isEmpty()) {
            log.warn("No values declared in user.resource.roles - no roles will be seeded");
        }
        return rolesByName;
    }

    private void seedPrograms(Map<String, Role> rolesByName) {
        int created = 0;
        int updated = 0;

        for (ProgramSeed seed : PROGRAM_SEEDS) {
            Role allowedRole = resolveRole(seed.roleName(), rolesByName);
            if (allowedRole == null) {
                log.warn("Skipping program '{}' because role '{}' is not configured", seed.name(), seed.roleName());
                continue;
            }

            Program program = new Program(seed.name(), allowedRole);
            programRepository.save(program);
            created++;
            log.info("Created default program '{}'", seed.name());
        }

        log.info("Program seeding complete: {} created, {} updated", created, updated);
    }

    private void seedUsers(Map<String, Role> rolesByName) {
        int created = 0;
        int updated = 0;

        for (UserSeed seed : USER_SEEDS) {
            Set<Role> rolesToAssign = resolveRoles(seed.roleNames(), rolesByName);
            if (rolesToAssign.isEmpty()) {
                log.warn("Skipping user '{}' because none of its roles are configured", seed.email());
                continue;
            }

            User user = new User(seed.email(), seed.firstName(), seed.lastName());
            rolesToAssign.forEach(user::addRole);
            User persisted = userRepository.save(user);
            created++;
            log.info("Created default user '{}'", seed.email());
            persistDevices(persisted, seed.devices());
        }

        log.info("User seeding complete: {} created, {} updated", created, updated);
    }

    private boolean updateUser(User user, UserSeed seed, Set<Role> rolesToAssign) {
        boolean changed = false;
        if (!Objects.equals(user.getFirstName(), seed.firstName())) {
            user.setFirstName(seed.firstName());
            changed = true;
        }
        if (!Objects.equals(user.getLastName(), seed.lastName())) {
            user.setLastName(seed.lastName());
            changed = true;
        }
        for (Role role : rolesToAssign) {
            if (user.addRole(role)) {
                changed = true;
            }
        }
        return changed;
    }

    private void persistDevices(User owner, List<DeviceSeed> deviceSeeds) {
        if (deviceSeeds.isEmpty()) {
            return;
        }
        List<Device> devices = new ArrayList<>(deviceSeeds.size());
        for (DeviceSeed seed : deviceSeeds) {
            devices.add(seed.toEntity(owner));
        }
        deviceRepository.saveAll(devices);
        log.info("Created {} device(s) for user '{}'", devices.size(), owner.getEmail());
    }

    private Set<Role> resolveRoles(Set<String> roleNames, Map<String, Role> rolesByName) {
        Set<Role> resolved = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            Role role = rolesByName.get(normalize(roleName));
            if (role != null) {
                resolved.add(role);
            } else {
                log.warn("Role '{}' referenced in seed data but not present in user.resource.roles", roleName);
            }
        }
        return resolved;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private Role resolveRole(String roleName, Map<String, Role> rolesByName) {
        if (roleName == null) {
            return null;
        }
        Role role = rolesByName.get(normalize(roleName));
        if (role == null) {
            log.warn("Role '{}' referenced in seed data but not present in user.resource.roles", roleName);
        }
        return role;
    }

    private record ProgramSeed(String name, String roleName) {
    }

    private record UserSeed(String email, String firstName, String lastName, Set<String> roleNames,
            List<DeviceSeed> devices) {
    }

    private record DeviceSeed(String name, Device.Status status, Duration lastSeenAgo, String macAddress,
            String publicIp) {
        Device toEntity(User owner) {
            Instant lastSeen = lastSeenAgo == null ? null : Instant.now().minus(lastSeenAgo);
            return new Device(name, owner, status, lastSeen, macAddress, publicIp);
        }
    }
}

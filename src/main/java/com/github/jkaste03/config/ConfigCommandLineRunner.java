package com.github.jkaste03.config;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.repository.ProgramRepository;
import com.github.jkaste03.repository.UserRepository;
import com.github.jkaste03.security.AppRoles;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@Profile("!prod")
public class ConfigCommandLineRunner implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(ConfigCommandLineRunner.class);

        private final RoleRepository roleRepo;
        private final UserRepository userRepo;
        private final DeviceRepository deviceRepo;
        private final ProgramRepository programRepo;
        private final PasswordEncoder passwordEncoder;

        public ConfigCommandLineRunner(
                        RoleRepository roleRepo,
                        UserRepository userRepo,
                        DeviceRepository deviceRepo,
                        ProgramRepository programRepo,
                        PasswordEncoder passwordEncoder) {
                this.roleRepo = roleRepo;
                this.userRepo = userRepo;
                this.deviceRepo = deviceRepo;
                this.programRepo = programRepo;
                this.passwordEncoder = passwordEncoder;
        }

        @Override
        @Transactional
        public void run(String... args) {
                ensureBaseRoles();
                ensureDefaultAdmin();

                // Reset device status at startup
                List<Device> allDevices = deviceRepo.findAll();
                for (Device d : allDevices) {
                        d.setStatus(Device.Status.OFFLINE);
                }
                deviceRepo.saveAll(allDevices);

                resetStuckProgramStatuses();
        }

        private void resetStuckProgramStatuses() {
                List<Program> toUpdate = new java.util.ArrayList<>();
                for (Program p : programRepo.findAll()) {
                        if (p.getStatus() == Program.Status.TRYING_TO_START
                                        || p.getStatus() == Program.Status.TRYING_TO_STOP) {
                                p.setStatus(Program.Status.STOPPED);
                                toUpdate.add(p);
                        }
                }
                if (!toUpdate.isEmpty()) {
                        programRepo.saveAll(toUpdate);
                        log.info("Reset programs stuck in TRYING_* to STOPPED at startup.");
                }
        }

        private void ensureBaseRoles() {
                createRole(AppRoles.USER);
                createRole(AppRoles.ADMIN);
        }

        private void ensureDefaultAdmin() {
                if (userRepo.count() > 0) {
                        return; // assume seeded
                }

                Role adminRole = roleRepo.findByName(AppRoles.ADMIN).orElseGet(() -> roleRepo.save(new Role(AppRoles.ADMIN)));
                Role userRole = roleRepo.findByName(AppRoles.USER).orElseGet(() -> roleRepo.save(new Role(AppRoles.USER)));

                String adminPassword = System.getenv().getOrDefault("APP_ADMIN_PASSWORD", "admin123");
                String userPassword = System.getenv().getOrDefault("APP_USER_PASSWORD", "user123");

                UUID adminId = UUID.fromString("11111111-1111-1111-1111-111111111111");
                UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

                User admin = new User(adminId, "admin@example.com", "admin", adminRole,
                                passwordEncoder.encode(adminPassword));
                User user = new User(userId, "user@example.com", "user", userRole,
                                passwordEncoder.encode(userPassword));

                userRepo.saveAll(List.of(admin, user));

                log.info("Seeded default users: admin/admin123 and user/user123 (override with env APP_ADMIN_PASSWORD / APP_USER_PASSWORD)");
        }

        private Role createRole(String name) {
                return roleRepo.findByName(name).orElseGet(() -> roleRepo.save(new Role(name)));
        }
}

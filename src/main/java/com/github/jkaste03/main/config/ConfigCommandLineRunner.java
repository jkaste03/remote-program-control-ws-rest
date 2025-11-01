package com.github.jkaste03.main.config;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.repository.UserRepository;

@Component
public class ConfigCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigCommandLineRunner.class);

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final DeviceRepository deviceRepo;

    public ConfigCommandLineRunner(
            RoleRepository roleRepo,
            UserRepository userRepo,
            DeviceRepository deviceRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Skipp hvis noe allerede finnes (trygt selv om du antar tom DB)
        if (roleRepo.count() > 0 || userRepo.count() > 0 || deviceRepo.count() > 0) {
            log.info("Seeder hoppet over: det finnes allerede data.");
            return;
        }

        log.info("Starter seeding av grunndata ...");

        // 1) Roller (data-drevet Roller beholdes)
        Role roleUser = createRole("USER");
        Role roleAdmin = createRole("ADMIN");

        // 2) Brukere (MANGE roller via UserRole-link)
        User admin = new User("admin@local", "Admin", "User");
        admin.addRole(roleAdmin);
        admin.addRole(roleUser); // admin har også USER

        User regular = new User("user@local", "Regular", "User");
        regular.addRole(roleUser);

        userRepo.saveAll(List.of(admin, regular));
        // Merk: CascadeType.PERSIST på User.roles gjør at UserRole-lenkene persisteres
        // samtidig

        // 3) Enheter
        Device userPc = new Device(
                "User-PC",
                regular,
                Device.Status.OFFLINE,
                null,
                "00:11:22:33:44:55",
                null);

        Device adminServer = new Device(
                "Admin-Server",
                admin,
                Device.Status.ONLINE,
                Instant.now(),
                "00:11:22:33:44:66",
                "198.51.100.10");

        // 4) Programmer
        // - Calculator: public (alle kan; ingen unntak nødvendig)
        Program calc = new Program("Calculator", true);

        // - AdminTool: privat (ingen kan, UNNTATT admin)
        Program adminTool = new Program("AdminTool", false);
        adminTool.allow(admin);

        // (valgfritt) Et ekstra public program på admin-serveren
        Program logViewer = new Program("LogViewer", true);

        // Koble programmer til enheter (holder begge sider i sync)
        userPc.addProgram(calc);
        adminServer.addProgram(adminTool);
        adminServer.addProgram(logViewer);

        // 5) Lagre (Device -> Program cascade = ALL, så programmer persisteres
        // automatisk)
        deviceRepo.saveAll(List.of(userPc, adminServer));

        log.info("Seeding ferdig. Roller: {}, Brukere: {}, Enheter: {}",
                roleRepo.count(), userRepo.count(), deviceRepo.count());
    }

    private Role createRole(String name) {
        return roleRepo.save(new Role(name));
    }
}

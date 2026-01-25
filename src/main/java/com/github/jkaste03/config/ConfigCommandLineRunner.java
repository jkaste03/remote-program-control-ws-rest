package com.github.jkaste03.config;

import java.util.List;
import java.util.UUID;

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
import com.github.jkaste03.security.AppRoles;

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
        log.info("Starter seeding av grunndata ...");

        // i ConfigCommandLineRunner.run(...)
        Role roleUser = createRole(AppRoles.USER);
        Role roleAdmin = createRole(AppRoles.ADMIN);

        // UUID-er synkronisert med Keycloak import (remote-control-users-0.json)
        UUID adminId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        User admin = userRepo.findById(adminId)
                .orElseGet(() -> new User(adminId, "admin@example.com", "admin", roleAdmin));
        admin.setEmail("admin@example.com");
        admin.setUsername("admin");
        admin.setRole(roleAdmin);

        User user = userRepo.findById(userId)
                .orElseGet(() -> new User(userId, "user@example.com", "user", roleUser));
        user.setEmail("user@example.com");
        user.setUsername("user");
        user.setRole(roleUser);

        // save(..) returns managed instances when IDs are pre-assigned; reuse them so
        // Device.owner is managed
        admin = userRepo.save(admin);
        user = userRepo.save(user);

        // Enheter + programmer
        Device userPc = new Device("User-PC", "84.211.133.68", 9,
                "54:AB:3A:66:70:15", user, Device.Status.OFFLINE,
                null);
        Device adminServer = new Device("Admin-Server", "84.211.133.68", 7,
                "C8:D3:FF:44:37:DB", admin,
                Device.Status.OFFLINE,
                null);

        Program calc = new Program("Calculator", true); // synlig for alle

        Program adminCalc = new Program("Admin Calculator", true); // synlig for alle
        Program adminTool = new Program("AdminTool", false); // kun admin-enheten

        userPc.addProgram(calc);
        adminServer.addProgram(adminCalc);
        adminServer.addProgram(adminTool);

        deviceRepo.saveAll(List.of(userPc, adminServer));

        // Reset all to OFFLINE on startup
        List<Device> allDevices = deviceRepo.findAll();
        for (Device d : allDevices) {
            d.setStatus(Device.Status.OFFLINE);
        }
        deviceRepo.saveAll(allDevices);
        log.info("Seeding ferdig. Roller: {}, Brukere: {}, Enheter: {}. Reset all devices to OFFLINE.",
                roleRepo.count(), userRepo.count(), deviceRepo.count());
    }

    private Role createRole(String name) {
        return roleRepo.findByName(name).orElseGet(() -> roleRepo.save(new Role(name)));
    }
}

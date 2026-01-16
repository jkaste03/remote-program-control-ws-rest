package com.github.jkaste03.config;

import java.time.Instant;
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

        // UUID-er fra dine test-tokens (application.properties)
        UUID user1Id = UUID.fromString("1ba8ff98-22e2-4607-8972-4e851cd0f6ca");
        UUID adminId = UUID.fromString("e0622f60-3606-40f9-8326-665b778ed7ea");
        UUID user3Id = UUID.fromString("8a0d7313-adfa-4751-aa05-d8ce219e5568");
        UUID user4Id = UUID.fromString("789ee020-e0e7-45de-aa08-ff4b3ddc398f");

        User u1 = userRepo.save(new User(user1Id, "user1@email.com", "user1", roleUser));
        User u2 = userRepo.save(new User(adminId, "user2@email.com", "user2", roleAdmin));
        User u3 = userRepo.save(new User(user3Id, "user3@email.com", "user3", roleUser));
        User u4 = userRepo.save(new User(user4Id, "user4@email.com", "user4", roleUser));

        userRepo.saveAll(List.of(u1, u2, u3, u4));

        // Enheter + programmer
        Device userPc = new Device("User1-PC", "84.211.133.68", 9, "54:AB:3A:66:70:15", u1, Device.Status.OFFLINE,
                null);
        Device adminServer = new Device("Admin-Server", "84.211.133.68", 7, "C8:D3:FF:44:37:DB", u2,
                Device.Status.ONLINE,
                Instant.now());

        // Public program som alle ser
        Program calc = new Program("Calculator", true);
        Program privateProgram = new Program("SecretApp", false);

        // Privat admin-verktøy (tillat bare adminbrukeren fra Keycloak)
        Program adminTool = new Program("AdminTool", false);

        // Knyt programmer
        userPc.addProgram(calc);
        userPc.addProgram(privateProgram);
        adminServer.addProgram(adminTool);

        deviceRepo.saveAll(List.of(userPc, adminServer));

        log.info("Seeding ferdig. Roller: {}, Brukere: {}, Enheter: {}",
                roleRepo.count(), userRepo.count(), deviceRepo.count());

        // Not to remove
        // Reset all to OFFLINE on startup
        List<Device> allDevices = deviceRepo.findAll();
        for (Device d : allDevices) {
            d.setStatus(Device.Status.OFFLINE);
        }
        deviceRepo.saveAll(allDevices);
        log.info("Reset all devices to OFFLINE on startup.");
    }

    private Role createRole(String name) {
        return roleRepo.save(new Role(name));
    }
}

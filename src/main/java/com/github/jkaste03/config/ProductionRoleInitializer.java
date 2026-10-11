package com.github.jkaste03.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.model.Role;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.security.AppRoles;

@Component
@Profile("prod")
public class ProductionRoleInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionRoleInitializer.class);

    private final RoleRepository roleRepository;

    public ProductionRoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureRole(AppRoles.USER);
        ensureRole(AppRoles.ADMIN);
    }

    private void ensureRole(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(name));
            log.info("Created missing production role '{}'.", name);
        }
    }
}

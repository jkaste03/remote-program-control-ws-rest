package com.github.jkaste03.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.github.jkaste03.repository")
@EntityScan("com.github.jkaste03.model")
@ComponentScan(basePackages = { "com.github.jkaste03.service",
		"com.github.jkaste03.controller",
		"com.github.jkaste03.main.config",
		"com.github.jkaste03.security" })
public class RemoteProgramControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemoteProgramControlApplication.class, args);

	}

}

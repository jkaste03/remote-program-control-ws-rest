package com.github.jkaste03.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("no.hvl.dat152.repository")
@EntityScan("no.hvl.dat152.model")
@ComponentScan(basePackages = { "no.hvl.dat152.service",
		"no.hvl.dat152.controller",
		"no.hvl.dat152.main.config",
		"no.hvl.dat152.security" })
public class RemoteProgramControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemoteProgramControlApplication.class, args);

	}

}

package com.github.jkaste03.main.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class ConfigCommandLineRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(ConfigCommandLineRunner.class);

	@Override
	public void run(String... args) throws Exception {

	}

}

package com.github.jkaste03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RemoteProgramControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemoteProgramControlApplication.class, args);
    }
}

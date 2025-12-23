package com.vibe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VibeDriveApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeDriveApplication.class, args);
    }

}

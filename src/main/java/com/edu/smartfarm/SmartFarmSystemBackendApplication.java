package com.edu.smartfarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartFarmSystemBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFarmSystemBackendApplication.class, args);
    }

}

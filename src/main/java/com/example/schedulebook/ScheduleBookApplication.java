package com.example.schedulebook;

import com.example.schedulebook.common.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ScheduleBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScheduleBookApplication.class, args);
    }

}

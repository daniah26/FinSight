package com.finsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FinSightApplication {
    public static void main(String[] args) {
        // Set default timezone to Bahrain
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Bahrain"));
        SpringApplication.run(FinSightApplication.class, args);
    }
}

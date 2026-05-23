package ru.practicum.statservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StatServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatServerApplication.class, args);
        System.out.println("STAT-SERVER RUNNING");
    }
}
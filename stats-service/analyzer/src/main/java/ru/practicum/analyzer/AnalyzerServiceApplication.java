package ru.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyzerServiceApplication {
    public static void main(String[] args) {
        System.out.println("ANALYZER START");
        SpringApplication.run(AnalyzerServiceApplication.class, args);
        System.out.println("ANALYZER RUNNING");
    }
}
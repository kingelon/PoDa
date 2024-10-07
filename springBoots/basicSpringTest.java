package com.example.hbaseapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class HBaseApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HBaseApiApplication.class, args);
    }

    @RestController
    @RequestMapping("/api")
    public static class SimpleController {

        @GetMapping("/ping")
        public String ping() {
            return "Spring Boot application is running!";
        }
    }
}

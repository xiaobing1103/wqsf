package com.wqst.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WanqiShangfutongApplication {
    public static void main(String[] args) {
        SpringApplication.run(WanqiShangfutongApplication.class, args);
    }
}

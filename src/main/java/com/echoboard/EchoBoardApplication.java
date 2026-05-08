package com.echoboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EchoBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoBoardApplication.class, args);
    }

}

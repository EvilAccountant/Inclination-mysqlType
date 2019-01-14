package com.ming.inclination;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan("com.ming.inclination.dao")
public class InclinationApplication {

    public static void main(String[] args) {
        SpringApplication.run(InclinationApplication.class, args);
    }

}


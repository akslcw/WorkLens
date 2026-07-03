package com.su.worklens_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.su.worklens_backend.mapper")
@SpringBootApplication
public class WorklensBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorklensBackendApplication.class, args);
    }

}

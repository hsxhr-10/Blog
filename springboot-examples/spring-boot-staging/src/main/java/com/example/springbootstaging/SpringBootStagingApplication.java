package com.example.springbootstaging;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.springbootstaging.mapper")
public class SpringBootStagingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootStagingApplication.class, args);
    }

}

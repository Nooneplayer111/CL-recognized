package com.elephantjava;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@Slf4j
@EnableTransactionManagement
public class ElephantJavaApplication {

    public static void main(String[] args) {

        SpringApplication.run(ElephantJavaApplication.class, args);
        log.info("server started");
    }

}

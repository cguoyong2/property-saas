package com.yongquan.propertysaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PropertySaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(PropertySaasApplication.class, args);
    }
}

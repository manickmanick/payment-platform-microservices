package com.fooddelivery.catalog;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFeignClients
public class CatalogServiceApplication {
    public static void main(String[] args) { SpringApplication.run(CatalogServiceApplication.class, args); }
}

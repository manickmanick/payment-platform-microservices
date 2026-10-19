package com.fooddelivery.delivery;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication @org.springframework.cloud.openfeign.EnableFeignClients public class DeliveryServiceApplication { public static void main(String[] args){SpringApplication.run(DeliveryServiceApplication.class,args);} }

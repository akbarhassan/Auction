package com.ga.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class WarehouseApplication {

    static {
        io.github.cdimascio.dotenv.Dotenv.configure().load();
    }

    public static void main(String[] args) {
        SpringApplication.run(WarehouseApplication.class, args);
    }

}

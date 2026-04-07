package com.ga.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WarehouseApplication {

    static {
        io.github.cdimascio.dotenv.Dotenv.configure().load();
    }

    public static void main(String[] args) {
        SpringApplication.run(WarehouseApplication.class, args);
    }

}

N
package com.example.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PaymentBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentBackendApplication.class, args);
    }
}

package com.example.refunds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A shop's refunds service: the application the contract describes, written by hand.
 */
@SpringBootApplication
public class RefundsApplication {

    /**
     * Starts the service.
     *
     * @param args the command line
     */
    public static void main(String[] args) {
        SpringApplication.run(RefundsApplication.class, args);
    }
}

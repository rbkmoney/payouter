package com.rbkmoney.payouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.rbkmoney.payouter", "com.rbkmoney.dbinit"})
public class PayouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayouterApplication.class, args);
    }

}

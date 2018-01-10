package com.rbkmoney.payouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication(scanBasePackages = {"com.rbkmoney.payouter", "com.rbkmoney.dbinit"})
@ServletComponentScan
public class PayouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayouterApplication.class, args);
    }

}

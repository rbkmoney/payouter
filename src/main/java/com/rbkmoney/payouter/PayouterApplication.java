package com.rbkmoney.payouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableScheduling
@ServletComponentScan
@SpringBootApplication(scanBasePackages = "com.rbkmoney.payouter")
public class PayouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayouterApplication.class, args);
    }

}

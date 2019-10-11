package com.rbkmoney.payouter;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT20S")
@ServletComponentScan
@SpringBootApplication(scanBasePackages = "com.rbkmoney.payouter")
public class PayouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayouterApplication.class, args);
    }

}

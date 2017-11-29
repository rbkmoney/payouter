package com.rbkmoney.payouter.config;

import com.rbkmoney.payouter.domain.Sht;
import org.jooq.Schema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaoConfig {

    @Bean
    public Schema schema() {
        return Sht.SHT;
    }

}

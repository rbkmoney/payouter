package com.rbkmoney.payouter.config;

import com.rbkmoney.damsel.payment_processing.PartyManagementSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class PartyManagementConfig {

    @Bean
    public PartyManagementSrv.Iface partyManagementSrv(@Value("${service.partyManagement.url}") Resource resource) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI()).build(PartyManagementSrv.Iface.class);
    }

}

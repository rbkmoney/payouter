package com.rbkmoney.payouter.config;

import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;


@Configuration
public class DominantConfig {

    @Bean
    public RepositoryClientSrv.Iface dominantClient(@Value("${service.dominant.url}") Resource resource) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI()).build(RepositoryClientSrv.Iface.class);
    }
}

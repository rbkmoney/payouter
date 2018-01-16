package com.rbkmoney.payouter.config;

import com.rbkmoney.damsel.message_sender.MessageSenderSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class DudoserConfig {

    @Value("${service.dudoser.url}")
    Resource resource;

    @Bean
    public MessageSenderSrv.Iface dudoser() throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(resource.getURI()).build(MessageSenderSrv.Iface.class);
    }
}

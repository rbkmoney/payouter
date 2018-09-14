package com.rbkmoney.payouter.config;

import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.eventstock.client.poll.PollingEventPublisherBuilder;
import com.rbkmoney.payouter.poller.EventStockHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class EventStockConfig {

    @Bean
    public EventPublisher eventPublisher(
            EventStockHandler eventStockHandler,
            @Value("${service.bustermaze.url}") Resource resource,
            @Value("${service.bustermaze.polling.delay}") int pollDelay,
            @Value("${service.bustermaze.polling.maxPoolSize}") int maxPoolSize,
            @Value("${service.bustermaze.polling.housekeeperTimeout}") int housekeeperTimeout
    ) throws IOException {
        return new PollingEventPublisherBuilder()
                .withURI(resource.getURI())
                .withHousekeeperTimeout(housekeeperTimeout)
                .withEventHandler(eventStockHandler)
                .withMaxPoolSize(maxPoolSize)
                .withEventRetryDelay(pollDelay)
                .withPollDelay(pollDelay)
                .build();
    }

}

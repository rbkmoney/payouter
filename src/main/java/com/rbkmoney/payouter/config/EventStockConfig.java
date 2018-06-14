package com.rbkmoney.payouter.config;

import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.eventstock.client.poll.PollingEventPublisherBuilder;
import com.rbkmoney.payouter.poller.EventStockHandler;
import com.rbkmoney.payouter.poller.TemporalEventStockHandler;
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
            @Value("${service.bustermaze.polling.maxPoolSize}") int maxPoolSize
    ) throws IOException {
        return new PollingEventPublisherBuilder()
                .withURI(resource.getURI())
                .withEventHandler(eventStockHandler)
                .withMaxPoolSize(maxPoolSize)
                .withEventRetryDelay(pollDelay)
                .withPollDelay(pollDelay)
                .build();
    }

    @Bean
    public EventPublisher temporalEventPublisher(
            TemporalEventStockHandler temporalEventStockHandler,
            @Value("${service.bustermaze.url}") Resource resource,
            @Value("${service.bustermaze.polling.delay}") int pollDelay,
            @Value("${service.bustermaze.polling.maxPoolSize}") int maxPoolSize
    ) throws IOException {
        return new PollingEventPublisherBuilder()
                .withURI(resource.getURI())
                .withEventHandler(temporalEventStockHandler)
                .withMaxPoolSize(maxPoolSize)
                .withEventRetryDelay(pollDelay)
                .withPollDelay(pollDelay)
                .build();
    }

}

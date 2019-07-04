package com.rbkmoney.payouter.config;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.payouter.converter.SourceEventParser;
import com.rbkmoney.payouter.poller.listener.InvoicingKafkaListener;
import com.rbkmoney.payouter.service.PaymentProcessingEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConsumerBeanEnableConfig {

    @Bean
    @ConditionalOnProperty(value = "kafka.topics.invoice.enabled", havingValue = "true")
    public InvoicingKafkaListener paymentEventsKafkaListener(PaymentProcessingEventService paymentProcessingEventService,
                                                             SourceEventParser parser) {
        return new InvoicingKafkaListener(paymentProcessingEventService, parser);
    }

}

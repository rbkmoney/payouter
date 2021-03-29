package com.rbkmoney.payouter.config;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.payouter.poller.listener.InvoicingKafkaListener;
import com.rbkmoney.payouter.poller.listener.PartyManagementKafkaListener;
import com.rbkmoney.payouter.service.PartyManagementEventService;
import com.rbkmoney.payouter.service.PaymentProcessingEventService;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConsumerBeanEnableConfig {

    @Bean
    @ConditionalOnProperty(value = "kafka.topics.invoice.enabled", havingValue = "true")
    public InvoicingKafkaListener paymentEventsKafkaListener(PaymentProcessingEventService paymentEventService,
                                                             MachineEventParser<EventPayload> parser) {
        return new InvoicingKafkaListener(paymentEventService, parser);
    }

    @Bean
    @ConditionalOnProperty(value = "kafka.topics.party-management.enabled", havingValue = "true")
    public PartyManagementKafkaListener partyEventsKafkaListener(PartyManagementEventService partyEventService,
                                                                     MachineEventParser<PartyEventData> parser) {
        return new PartyManagementKafkaListener(partyEventService, parser);
    }

}

package com.rbkmoney.payouter.poller.listener;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.payouter.converter.SourceEventParser;
import com.rbkmoney.payouter.service.PartyManagementEventService;
import com.rbkmoney.payouter.service.PaymentProcessingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicingKafkaListener {

    private final PaymentProcessingEventService paymentProcessingEventService;
    private final SourceEventParser sourceEventParser;

    @KafkaListener(topics = "${invoicing.kafka.topic}", containerFactory = "kafkaListenerContainerFactory")
    public void handle(SinkEvent sinkEvent, Acknowledgment ack) {
        log.debug("Reading sinkEvent, sourceId:{}, eventId:{}", sinkEvent.getEvent().getSourceId(), sinkEvent.getEvent().getEventId());
        EventPayload payload = sourceEventParser.parseEvent(sinkEvent.getEvent());
        if (payload.isSetInvoiceChanges()) {
            paymentProcessingEventService.processEvent(sinkEvent.getEvent(), payload);
        }
        ack.acknowledge();
    }

}

package com.rbkmoney.payouter.poller.listener;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.converter.SourceEventParser;
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

    @KafkaListener(topics = "${kafka.topics.invoicing}", containerFactory = "kafkaListenerContainerFactory")
    public void handle(MachineEvent machineEvent, Acknowledgment ack) {
        log.debug("Reading sinkEvent, sourceId: {}, eventId: {}", machineEvent.getSourceId(), machineEvent.getEventId());
        EventPayload payload = sourceEventParser.parseEvent(machineEvent);
        if (payload.isSetInvoiceChanges()) {
            paymentProcessingEventService.processEvent(machineEvent, payload);
        }
        ack.acknowledge();
    }

}

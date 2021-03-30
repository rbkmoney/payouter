package com.rbkmoney.payouter.poller.listener;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.payouter.service.PaymentProcessingEventService;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
@RequiredArgsConstructor
public class InvoicingKafkaListener {

    private final PaymentProcessingEventService paymentProcessingEventService;
    private final MachineEventParser<EventPayload> parser;

    @KafkaListener(topics = "${kafka.topics.invoice.id}", containerFactory = "invContainerFactory")
    public void handle(SinkEvent sinkEvent, Acknowledgment ack) {
        MachineEvent event = sinkEvent.getEvent();
        log.debug("Reading sinkEvent, sourceId: {}, sequenceId: {}", event.getSourceId(), event.getEventId());
        EventPayload payload = parser.parse(event);
        if (payload.isSetInvoiceChanges()) {
            paymentProcessingEventService.processEvent(event, payload);
        }
        ack.acknowledge();
    }

}

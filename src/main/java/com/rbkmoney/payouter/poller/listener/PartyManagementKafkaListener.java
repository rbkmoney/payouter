package com.rbkmoney.payouter.poller.listener;

import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.kafka.common.util.LogUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.payouter.service.PartyManagementEventService;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PartyManagementKafkaListener {

    private final PartyManagementEventService partyManagementService;

    private final MachineEventParser<PartyEventData> partyEventDataParser;

    @KafkaListener(topics = "${kafka.topics.party-management.id}",
            containerFactory = "partyManagementListenerContainerFactory")
    public void handle(List<ConsumerRecord<String, SinkEvent>> messages, Acknowledgment ack) {
        log.info("Got partyManagement machineEvent batch with size: {}", messages.size());
        for (ConsumerRecord<String, SinkEvent> message : messages) {
            if (message != null && message.value().isSetEvent()) {
                MachineEvent machineEvent = message.value().getEvent();
                PartyEventData eventPayload = partyEventDataParser.parse(machineEvent);
                partyManagementService.processPayloadEvent(machineEvent, eventPayload);
            }
        }

        ack.acknowledge();
        log.info("Batch partyManagement has been committed, size={}, {}", messages.size(),
                LogUtil.toSummaryStringWithSinkEventValues(messages));
    }
}
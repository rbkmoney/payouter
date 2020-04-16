package com.rbkmoney.payouter.kafka;

import com.rbkmoney.damsel.payment_processing.PartyEventData;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.payouter.config.KafkaConfig;
import com.rbkmoney.payouter.poller.listener.PartyManagementKafkaListener;
import com.rbkmoney.payouter.service.PartyManagementEventService;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ContextConfiguration(classes = {KafkaConfig.class, PartyManagementKafkaListener.class})
public class PartyManagementKafkaListenerTest extends AbstractKafkaTest {

    @Value("${kafka.topics.party-management.id}")
    public String topic;

    @MockBean
    private PartyManagementEventService partyManagementEventService;

    @MockBean
    private MachineEventParser<PartyEventData> partyEventDataParser;

    @Test
    public void listenChanges() {
        when(partyEventDataParser.parse(any())).thenReturn(new PartyEventData());

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(createTestMachineEvent());

        writeToTopic(topic, sinkEvent);

        verify(partyManagementEventService, timeout(KAFKA_SYNC_TIME).times(1))
                .processPayloadEvent(any(MachineEvent.class), any(PartyEventData.class));
    }

}

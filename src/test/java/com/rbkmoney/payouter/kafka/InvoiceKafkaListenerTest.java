package com.rbkmoney.payouter.kafka;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.payouter.config.KafkaConfig;
import com.rbkmoney.payouter.poller.listener.InvoicingKafkaListener;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ContextConfiguration(classes = {KafkaConfig.class, InvoicingKafkaListener.class})
public class InvoiceKafkaListenerTest extends AbstractKafkaTest {

    @Value("${kafka.topics.invoice.id}")
    public String topic;

    @MockBean
    private MachineEventParser<EventPayload> parser;

    @Test
    public void listenChanges() throws InterruptedException {
        when(parser.parse(any())).thenReturn(EventPayload.invoice_changes(List.of(new InvoiceChange())));

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(createTestMachineEvent());

        writeToTopic(topic, sinkEvent);
        waitForTopicSync();

        Mockito.verify(parser, Mockito.times(1)).parse(any());
    }

}

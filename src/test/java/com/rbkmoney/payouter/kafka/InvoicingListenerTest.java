package com.rbkmoney.payouter.kafka;

import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.payouter.converter.SourceEventParser;
import com.rbkmoney.payouter.exception.ParseException;
import com.rbkmoney.payouter.poller.listener.InvoicingKafkaListener;
import com.rbkmoney.payouter.service.PaymentProcessingEventService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

public class InvoicingListenerTest {

    @Mock
    private PaymentProcessingEventService paymentProcessingEventService;
    @Mock
    private SourceEventParser eventParser;
    @Mock
    private Acknowledgment ack;

    private InvoicingKafkaListener listener;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        listener = new InvoicingKafkaListener(paymentProcessingEventService, eventParser);
    }

    @Test
    public void listenNonInvoiceChanges() {

        MachineEvent message = new MachineEvent();
        Event event = new Event();
        EventPayload payload = new EventPayload();
        payload.setCustomerChanges(List.of());
        event.setPayload(payload);
        Mockito.when(eventParser.parseEvent(message)).thenReturn(payload);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        listener.handle(message, ack);

        Mockito.verify(paymentProcessingEventService, Mockito.times(0)).processEvent(any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }

    @Test(expected = ParseException.class)
    public void listenEmptyException() {
        MachineEvent message = new MachineEvent();

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        Mockito.when(eventParser.parseEvent(message)).thenThrow(new ParseException());

        listener.handle(message, ack);

        Mockito.verify(ack, Mockito.times(0)).acknowledge();
    }

    @Test
    public void listenChanges() {
        MachineEvent message = new MachineEvent();
        Event event = new Event();
        EventPayload payload = new EventPayload();
        ArrayList<InvoiceChange> invoiceChanges = new ArrayList<>();
        invoiceChanges.add(new InvoiceChange());
        payload.setInvoiceChanges(invoiceChanges);
        event.setPayload(payload);
        Mockito.when(eventParser.parseEvent(message)).thenReturn(payload);

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(message);

        listener.handle(message, ack);

        Mockito.verify(paymentProcessingEventService, Mockito.times(1)).processEvent(any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }

}

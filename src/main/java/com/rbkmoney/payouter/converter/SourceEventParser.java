package com.rbkmoney.payouter.converter;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.kafka.common.converter.BinaryConverter;
import com.rbkmoney.kafka.common.converter.BinaryConverterImpl;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.exception.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SourceEventParser {

    private final BinaryConverter<EventPayload> converter = new BinaryConverterImpl();

    public EventPayload parseEvent(MachineEvent message) {
        try {
            byte[] bin = message.getData().getBin();
            return converter.convert(bin, EventPayload.class);
        } catch (Exception e) {
            throw new ParseException("Exception when parse message", e);
        }
    }
}

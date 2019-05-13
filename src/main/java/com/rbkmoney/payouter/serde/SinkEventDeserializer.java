package com.rbkmoney.payouter.serde;

import com.rbkmoney.kafka.common.deserializer.AbstractDeserializerAdapter;
import com.rbkmoney.machinegun.eventsink.SinkEvent;

public class SinkEventDeserializer extends AbstractDeserializerAdapter<SinkEvent> {

    @Override
    public SinkEvent deserialize(String topic, byte[] data) {
        return deserialize(data, new SinkEvent());
    }
}
package com.rbkmoney.payouter.serde;

import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.util.Map;

@Slf4j
public class MachineEventDeserializer extends AbstractThriftDeserializer<SinkEvent> {

    @Override
    public SinkEvent deserialize(String topic, byte[] data) {
        return this.deserialize(data, new SinkEvent());
    }

}

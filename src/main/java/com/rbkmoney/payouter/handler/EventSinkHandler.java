package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.payout_processing.*;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventSinkHandler implements EventSinkSrv.Iface {
    @Override
    public List<Event> getEvents(EventRange eventRange) throws EventNotFound, InvalidRequest, TException {
        throw new TException("Unsupported operation");
    }

    @Override
    public long getLastEventID() throws NoLastEvent, TException {
        throw new TException("Unsupported operation");
    }
}

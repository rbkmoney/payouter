package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.EventRange;
import com.rbkmoney.damsel.payout_processing.EventSinkSrv;
import com.rbkmoney.damsel.payout_processing.NoLastEvent;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.mapper.EventMapper;
import com.rbkmoney.payouter.service.EventSinkService;
import com.rbkmoney.payouter.validator.EventRangeValidator;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventSinkHandler implements EventSinkSrv.Iface {

    private final EventSinkService eventSinkService;
    private final EventRangeValidator eventRangeValidator;
    private final EventMapper eventMapper;

    @Override
    public List<Event> getEvents(EventRange eventRange) throws TException {
        Optional<Long> after = eventRangeValidator.validateAndExtractAfter(eventRange);
        List<PayoutEvent> events = eventSinkService.getEvents(after, eventRange.getLimit());

        return eventMapper.toDamselEvents(events);
    }

    @Override
    public long getLastEventID() throws TException {
        Long lastEventId = eventSinkService.getLastEventId();
        if (lastEventId == null) throw new NoLastEvent();

        return lastEventId;
    }
}

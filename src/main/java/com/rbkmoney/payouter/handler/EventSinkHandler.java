package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.service.CashFlowDescriptionService;
import com.rbkmoney.payouter.service.EventSinkService;
import com.rbkmoney.payouter.util.DamselUtil;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EventSinkHandler implements EventSinkSrv.Iface {

    private final EventSinkService eventSinkService;
    private final CashFlowDescriptionService cashFlowDescriptionService;

    @Autowired
    public EventSinkHandler(EventSinkService eventSinkService, CashFlowDescriptionService cashFlowDescriptionService) {
        this.eventSinkService = eventSinkService;
        this.cashFlowDescriptionService = cashFlowDescriptionService;
    }

    @Override
    public List<Event> getEvents(EventRange eventRange) throws EventNotFound, InvalidRequest, TException {
        Optional<Long> after = eventRange.isSetAfter() ? Optional.of(eventRange.getAfter()) : Optional.empty();

        if (eventRange.getLimit() < 0) {
            throw new InvalidRequest(Arrays.asList("limit must not be negative"));
        }

        if (after.isPresent()) {
            PayoutEvent payoutEvent = eventSinkService.getEvent(after.get());
            if (payoutEvent == null) {
                throw new EventNotFound();
            }
        }

        List<Event> eventList = eventSinkService.getEvents(after, eventRange.getLimit()).stream()
                .map(DamselUtil::toDamselEvent)
                .collect(Collectors.toList());

        for (Event event : eventList) {
            for (PayoutChange pc : event.getPayload().getPayoutChanges()) {
                if (pc.isSetPayoutCreated()) {
                    Payout payout = pc.getPayoutCreated().getPayout();
                    List<CashFlowDescription> cashFlowDescriptions = cashFlowDescriptionService.get(Long.parseLong(payout.getId()));
                    payout.setCashFlowDescriptions(DamselUtil.toDamselCashFlowDescription(cashFlowDescriptions));
                }
            }
        }

        return eventList;
    }

    @Override
    public long getLastEventID() throws NoLastEvent, TException {
        Long lastEventId = eventSinkService.getLastEventId();
        if (lastEventId == null) {
            throw new NoLastEvent();
        }
        return lastEventId;
    }
}

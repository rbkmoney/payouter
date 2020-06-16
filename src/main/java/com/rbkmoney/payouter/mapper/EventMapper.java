package com.rbkmoney.payouter.mapper;

import com.rbkmoney.damsel.payout_processing.Event;
import com.rbkmoney.damsel.payout_processing.PayoutChange;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.service.PayoutSummaryService;
import com.rbkmoney.payouter.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class EventMapper {

    private final PayoutSummaryService payoutSummaryService;

    public List<Event> toDamselEvents(List<PayoutEvent> payoutEvents) {
        List<Event> eventList = payoutEvents.stream()
                .map(DamselUtil::toDamselEvent)
                .collect(toList());

        for (Event event : eventList) {
            for (PayoutChange pc : event.getPayload().getPayoutChanges()) {
                if (pc.isSetPayoutCreated()) {
                    com.rbkmoney.damsel.payout_processing.Payout payout = pc.getPayoutCreated().getPayout();
                    List<PayoutSummary> payoutSummaries = payoutSummaryService.get(payout.getId());
                    payout.setSummary(DamselUtil.toDamselPayoutSummary(payoutSummaries));
                }
            }
        }

        return eventList;
    }
}

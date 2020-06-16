package com.rbkmoney.payouter.validator;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.payout_processing.EventNotFound;
import com.rbkmoney.damsel.payout_processing.EventRange;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.service.EventSinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventRangeValidator {

    private final EventSinkService eventSinkService;

    public Optional<Long> validateAndExtractAfter(EventRange eventRange) throws InvalidRequest, EventNotFound {
        Optional<Long> after = eventRange.isSetAfter()
                ? Optional.of(eventRange.getAfter())
                : Optional.empty();

        if (eventRange.getLimit() < 0) {
            throw new InvalidRequest(List.of("limit must not be negative"));
        }

        if (after.isPresent()) {
            PayoutEvent payoutEvent = eventSinkService.getEvent(after.get());
            if (payoutEvent == null) throw new EventNotFound();
        }

        return after;
    }
}

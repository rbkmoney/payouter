package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;

import java.time.LocalDateTime;

public interface PayoutService {

    long createPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType);

    void doPaid(Payout payout);

}

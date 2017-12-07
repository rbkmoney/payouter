package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.PayoutService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PayoutManagementHandler implements PayoutManagementSrv.Iface {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutService payoutService;

    @Autowired
    public PayoutManagementHandler(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @Override
    public String generatePayout(GeneratePayoutParams generatePayoutParams) throws InvalidRequest, TException {
        try {
            String partyId = generatePayoutParams.getPartyId();
            String shopId = generatePayoutParams.getShopId();

            TimeRange timeRange = generatePayoutParams.getTimeRange();
            LocalDateTime fromTime = TypeUtil.stringToLocalDateTime(timeRange.getFromTime());
            LocalDateTime toTime = TypeUtil.stringToLocalDateTime(timeRange.getToTime());

            if (fromTime.isAfter(toTime)) {
                throw new InvalidRequest(Arrays.asList("fromTime must be less that toTime"));
            }

            long payoutId = payoutService.createPayout(partyId, shopId, fromTime, toTime, PayoutType.bank_account);

            return String.valueOf(payoutId);
        } catch (NotFoundException | InvalidStateException | IllegalArgumentException ex) {
            throw new InvalidRequest(Arrays.asList(ex.getMessage()));
        }
    }

    @Override
    public List<String> confirmPayouts(List<String> payoutIds) throws InvalidRequest, TException {
        List<String> confirmedPayouts = new ArrayList<>();
        for (String payoutId : payoutIds) {
            try {
                payoutService.confirm(Long.valueOf(payoutId));
                confirmedPayouts.add(payoutId);
            } catch (Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
        }
        return confirmedPayouts;
    }

    @Override
    public List<String> cancelPayouts(List<String> payoutIds) throws InvalidRequest, TException {
        List<String> cancelledPayouts = new ArrayList<>();
        for (String payoutId : payoutIds) {
            try {
                payoutService.cancel(Long.valueOf(payoutId));
                cancelledPayouts.add(payoutId);
            } catch (Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
        }
        return cancelledPayouts;
    }

    @Override
    public List<PayoutInfo> getPayoutsInfo(PayoutSearchCriteria payoutSearchCriteria) throws InvalidRequest, TException {
        throw new TException("Unsupported operation");
    }
}

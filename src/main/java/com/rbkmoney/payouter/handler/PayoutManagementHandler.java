package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.payout_processing.GeneratePayoutParams;
import com.rbkmoney.damsel.payout_processing.PayoutInfo;
import com.rbkmoney.damsel.payout_processing.PayoutManagementSrv;
import com.rbkmoney.damsel.payout_processing.PayoutSearchCriteria;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayoutManagementHandler implements PayoutManagementSrv.Iface {
    @Override
    public String generatePayout(GeneratePayoutParams generatePayoutParams) throws InvalidRequest, TException {
        throw new TException("Unsupported operation");
    }

    @Override
    public List<String> confirmPayouts(List<String> payoutIds) throws InvalidRequest, TException {
        throw new TException("Unsupported operation");
    }

    @Override
    public List<String> cancelPayouts(List<String> payoutIds) throws InvalidRequest, TException {
        throw new TException("Unsupported operation");
    }

    @Override
    public List<PayoutInfo> getPayoutsInfo(PayoutSearchCriteria payoutSearchCriteria) throws InvalidRequest, TException {
        throw new TException("Unsupported operation");
    }
}

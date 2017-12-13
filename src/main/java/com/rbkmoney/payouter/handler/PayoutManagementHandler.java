package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.domain.BankAccount;
import com.rbkmoney.damsel.domain.LegalAgreement;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.util.DamselUtil;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        log.info("GetPayoutsInfo with search criteria: {}", payoutSearchCriteria);
        Optional<com.rbkmoney.payouter.domain.enums.PayoutStatus> payoutStatus = Optional.ofNullable(payoutSearchCriteria.getStatus()).map(ps -> com.rbkmoney.payouter.domain.enums.PayoutStatus.valueOf(ps.name().toUpperCase()));
        Optional<LocalDateTime> fromTime = Optional.ofNullable(payoutSearchCriteria.getTimeRange()).map(tr -> TypeUtil.stringToLocalDateTime(tr.getFromTime()));
        Optional<LocalDateTime> toTime = Optional.ofNullable(payoutSearchCriteria.getTimeRange()).map(TimeRange::getToTime).map(TypeUtil::stringToLocalDateTime);
        Optional<List<Long>> payoutIds = Optional.ofNullable(payoutSearchCriteria.getPayoutIds()).map(pids -> pids.stream().map(Long::valueOf).collect(Collectors.toList()));

        validateRequest(fromTime, toTime);

        List<Payout> payoutList = payoutService.search(payoutStatus, fromTime, toTime, payoutIds);
        List<PayoutInfo> payoutInfoList = payoutList.stream().map(this::buildPayoutInfo).collect(Collectors.toList());
        log.info("GetPayoutsInfo count: {}", payoutInfoList.size());
        return payoutInfoList;
    }

    private void validateRequest(Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTime) throws InvalidRequest {
        List<String> errorList = new ArrayList<>();
        if (toTime.isPresent() && fromTime.isPresent()) {
            if (fromTime.get().isAfter(toTime.get())) {
                errorList.add(String.format("FromTime %s must be before toTime %s", fromTime.get().toString(), toTime.get().toString()));
            }
        }
        if (!errorList.isEmpty()) {
            throw new InvalidRequest(errorList);
        }
    }

    private PayoutInfo buildPayoutInfo(Payout record) {
        PayoutInfo payoutInfo = new PayoutInfo();
        payoutInfo.setId(String.valueOf(record.getId()));
        payoutInfo.setPartyId(record.getPartyId());
        payoutInfo.setShopId(record.getShopId());
        payoutInfo.setAmount(record.getAmount());
        if (record.getPayoutType().equals(PayoutType.bank_account)) {
            BankAccount bankAccount = new BankAccount();
            bankAccount.setBankBik(record.getBankBik());
            bankAccount.setAccount(record.getBankAccount());
            bankAccount.setBankPostAccount(record.getBankPostAccount());
            bankAccount.setBankName(record.getBankName());

            PayoutAccount payoutAccount = new PayoutAccount();
            payoutAccount.setAccount(bankAccount);
            payoutAccount.setInn(record.getInn());
            payoutAccount.setPurpose(record.getPurpose());
            LegalAgreement legalAgreement = new LegalAgreement(record.getAccountLegalAgreementId(), TypeUtil.temporalToString(record.getAccountLegalAgreementSignedAt()));
            payoutAccount.setLegalAgreement(legalAgreement);
            com.rbkmoney.damsel.payout_processing.PayoutType payoutType = new com.rbkmoney.damsel.payout_processing.PayoutType();
            payoutType.setBankAccount(payoutAccount);
            payoutInfo.setType(payoutType);
        }
        payoutInfo.setStatus(DamselUtil.toDamselPayoutStatus(record));
        payoutInfo.setFromTime(TypeUtil.temporalToString(record.getFromTime()));
        payoutInfo.setToTime(TypeUtil.temporalToString(record.getToTime()));
        payoutInfo.setCreatedAt(TypeUtil.temporalToString(record.getCreatedAt()));
        return payoutInfo;
    }
}

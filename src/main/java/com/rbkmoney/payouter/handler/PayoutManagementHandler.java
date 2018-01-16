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
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PayoutManagementHandler implements PayoutManagementSrv.Iface {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    public static final int MAX_SIZE = 1000;

    private final PayoutService payoutService;

    @Autowired
    public PayoutManagementHandler(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @Override
    public List<String> generatePayouts(GeneratePayoutParams generatePayoutParams) throws InvalidRequest, TException {
        log.info("Start generate payouts, params: {}", generatePayoutParams);
        try {
            TimeRange timeRange = generatePayoutParams.getTimeRange();
            LocalDateTime fromTime = TypeUtil.stringToLocalDateTime(timeRange.getFromTime());
            LocalDateTime toTime = TypeUtil.stringToLocalDateTime(timeRange.getToTime());

            if (fromTime.isAfter(toTime)) {
                throw new InvalidRequest(Arrays.asList("fromTime must be less that toTime"));
            }

            if (generatePayoutParams.isSetShop()) {
                ShopParams shopParams = generatePayoutParams.getShop();
                long payoutId = payoutService.createPayout(shopParams.getPartyId(), shopParams.getShopId(), fromTime, toTime, PayoutType.bank_account);
                return Arrays.asList(String.valueOf(payoutId));
            }

            List<Long> payoutIds = payoutService.createPayouts(fromTime, toTime, PayoutType.bank_account);
            return payoutIds.stream()
                    .map(id -> String.valueOf(id))
                    .collect(Collectors.toList());

        } catch (NotFoundException | InvalidStateException | IllegalArgumentException ex) {
            throw new InvalidRequest(Arrays.asList(ex.getMessage()));
        } finally {
            log.info("End generate payouts, params: {}", generatePayoutParams);
        }
    }

    @Override
    public Set<String> confirmPayouts(Set<String> payoutIds) throws InvalidRequest, TException {
        log.info("Start confirm payouts, payoutIds: {}", payoutIds);
        try {
            Set<String> confirmedPayouts = new HashSet<>();
            for (String payoutId : payoutIds) {
                try {
                    payoutService.confirm(Long.valueOf(payoutId));
                    confirmedPayouts.add(payoutId);
                } catch (Exception ex) {
                    log.warn("Failed to confirm payout, payoutId={}", payoutId, ex);
                }
            }
            return confirmedPayouts;
        } finally {
            log.info("End confirm payouts, payoutIds: {}", payoutIds);
        }
    }

    @Override
    public Set<String> cancelPayouts(Set<String> payoutIds, String details) throws InvalidRequest, TException {
        log.info("Start cancel payouts, payoutIds: {}", payoutIds);
        try {
            Set<String> cancelledPayouts = new HashSet<>();
            for (String payoutId : payoutIds) {
                try {
                    payoutService.cancel(Long.valueOf(payoutId), details);
                    cancelledPayouts.add(payoutId);
                } catch (Exception ex) {
                    log.warn("Failed to cancel payout, payoutId={}", payoutId, ex);
                }
            }
            return cancelledPayouts;
        } finally {
            log.info("End cancel payouts, payoutIds: {}", payoutIds);
        }
    }

    @Override
    public PayoutSearchResponse getPayoutsInfo(PayoutSearchRequest payoutSearchRequest) throws InvalidRequest, TException {
        PayoutSearchCriteria payoutSearchCriteria = payoutSearchRequest.getSearchCriteria();
        log.info("GetPayoutsInfo with request parameters: {}", payoutSearchRequest);
        long fromId = payoutSearchRequest.getFromId();
        int size = payoutSearchRequest.getSize();
        Optional<com.rbkmoney.payouter.domain.enums.PayoutStatus> payoutStatus = Optional.ofNullable(payoutSearchCriteria.getStatus()).map(ps -> com.rbkmoney.payouter.domain.enums.PayoutStatus.valueOf(ps.name().toUpperCase()));
        Optional<LocalDateTime> fromTime = Optional.ofNullable(payoutSearchCriteria.getTimeRange()).map(tr -> TypeUtil.stringToLocalDateTime(tr.getFromTime()));
        Optional<LocalDateTime> toTime = Optional.ofNullable(payoutSearchCriteria.getTimeRange()).map(tr -> TypeUtil.stringToLocalDateTime(tr.getToTime()));
        Optional<List<Long>> payoutIds = Optional.ofNullable(payoutSearchCriteria.getPayoutIds()).map(pids -> pids.stream().map(Long::valueOf).collect(Collectors.toList()));

        validateRequest(size, fromTime, toTime);

        List<Payout> payoutList = payoutService.search(payoutStatus, fromTime, toTime, payoutIds, fromId, size);
        List<PayoutInfo> payoutInfoList = payoutList.stream().map(this::buildPayoutInfo).collect(Collectors.toList());
        long lastId = payoutInfoList.isEmpty() ? 0 : Long.parseLong(payoutInfoList.get(payoutInfoList.size() - 1).getId());
        PayoutSearchResponse payoutSearchResponse = new PayoutSearchResponse(payoutInfoList, lastId);
        log.info("GetPayoutsInfo count: {}", payoutInfoList.size());
        return payoutSearchResponse;
    }

    private void validateRequest(int size, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTime) throws InvalidRequest {
        List<String> errorList = new ArrayList<>();
        if (size <= 0 || size > MAX_SIZE) {
            errorList.add(String.format("Size %d must be positive and less then %d", size, MAX_SIZE));
        }
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

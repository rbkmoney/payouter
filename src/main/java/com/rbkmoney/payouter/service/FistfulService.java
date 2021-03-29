package com.rbkmoney.payouter.service;

import com.rbkmoney.fistful.*;
import com.rbkmoney.fistful.admin.DepositAmountInvalid;
import com.rbkmoney.fistful.admin.DepositCurrencyInvalid;
import com.rbkmoney.fistful.admin.DepositParams;
import com.rbkmoney.fistful.admin.FistfulAdminSrv;
import com.rbkmoney.fistful.base.Cash;
import com.rbkmoney.fistful.base.CurrencyRef;
import com.rbkmoney.fistful.deposit.Deposit;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
public class FistfulService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final FistfulAdminSrv.Iface fistfulClient;

    private final RetryTemplate retryTemplate;

    private final String defaultSourceId;

    public FistfulService(
            FistfulAdminSrv.Iface fistfulClient,
            RetryTemplate retryTemplate,
            @Value("${service.fistful.sourceId}") String defaultSourceId
    ) {
        this.fistfulClient = fistfulClient;
        this.retryTemplate = retryTemplate;
        this.defaultSourceId = defaultSourceId;
    }

    public Deposit createDeposit(String payoutId, String walletId, long amount, String currencyCode)
            throws NotFoundException, InvalidStateException {
        return createDeposit(payoutId, defaultSourceId, walletId, amount, currencyCode);
    }

    public Deposit createDeposit(String payoutId, String sourceId, String walletId, long amount, String currencyCode)
            throws NotFoundException, InvalidStateException {
        DepositParams depositParams = new DepositParams();
        depositParams.setId(toDepositId(payoutId));
        depositParams.setSource(sourceId);
        depositParams.setDestination(walletId);
        depositParams.setBody(new Cash(amount, new CurrencyRef(currencyCode)));

        log.info("Trying to create deposit, depositParams='{}'", depositParams);
        try {
            Deposit deposit = retryTemplate.execute(
                    context -> fistfulClient.createDeposit(depositParams)
            );
            log.info("Deposit have been created, deposit='{}'", deposit);
            return deposit;
        } catch (SourceNotFound | DestinationNotFound ex) {
            throw new NotFoundException(ex);
        } catch (SourceUnauthorized | DepositCurrencyInvalid | DepositAmountInvalid ex) {
            throw new InvalidStateException(ex);
        } catch (TException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String toDepositId(String payoutId) {
        return "payout_" + payoutId;
    }


}

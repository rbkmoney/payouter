package com.rbkmoney.payouter.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.PartyManagementService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Service
public class PartyManagementServiceImpl implements PartyManagementService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final UserInfo userInfo = new UserInfo("admin", UserType.internal_user(new InternalUser()));

    private final PartyManagementSrv.Iface partyManagementClient;

    private final Cache<Map.Entry<String, PartyRevisionParam>, Party> partyCache;

    @Autowired
    public PartyManagementServiceImpl(
            PartyManagementSrv.Iface partyManagementClient,
            @Value("${cache.maxSize}") long cacheMaximumSize
    ) {
        this.partyManagementClient = partyManagementClient;
        this.partyCache = Caffeine.newBuilder()
                .maximumSize(cacheMaximumSize)
                .build();
    }

    @Override
    public Party getParty(String partyId) throws NotFoundException {
        return getParty(partyId, Instant.now());
    }

    @Override
    public Party getParty(String partyId, Instant timestamp) throws NotFoundException {
        return getParty(partyId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public Party getParty(String partyId, long partyRevision) throws NotFoundException {
        return getParty(partyId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Party getParty(String partyId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.info("Trying to get party, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
        Party party = partyCache.get(
                new AbstractMap.SimpleEntry<>(partyId, partyRevisionParam),
                key -> {
                    try {
                        return partyManagementClient.checkout(userInfo, partyId, partyRevisionParam);
                    } catch (PartyNotFound ex) {
                        throw new NotFoundException(
                                String.format("Party not found, partyId='%s', partyRevisionParam='%s'", partyId, partyRevisionParam), ex
                        );
                    } catch (InvalidPartyRevision ex) {
                        throw new NotFoundException(
                                String.format("Invalid party revision, partyId='%s', partyRevisionParam='%s'", partyId, partyRevisionParam), ex
                        );
                    } catch (TException ex) {
                        throw new RuntimeException(
                                String.format("Failed to get party, partyId='%s', partyRevisionParam='%s'", partyId, partyRevisionParam), ex
                        );
                    }
                });
        log.info("Party has been found, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
        return party;
    }

    @Override
    public Shop getShop(String partyId, String shopId) throws NotFoundException {
        return getShop(partyId, shopId, Instant.now());
    }

    @Override
    public Shop getShop(String partyId, String shopId, long partyRevision) throws NotFoundException {
        return getShop(partyId, shopId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Shop getShop(String partyId, String shopId, Instant timestamp) throws NotFoundException {
        return getShop(partyId, shopId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public Shop getShop(String partyId, String shopId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.info("Trying to get shop, partyId='{}', shopId='{}', partyRevisionParam='{}'", partyId, shopId, partyRevisionParam);
        Party party = getParty(partyId, partyRevisionParam);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', shopId='%s', partyRevisionParam='%s'", partyId, shopId, partyRevisionParam));
        }
        log.info("Shop has been found, partyId='{}', shopId='{}', partyRevisionParam='{}'", partyId, shopId, partyRevisionParam);
        return shop;
    }

    @Override
    public Contract getContract(String partyId, String contractId) throws NotFoundException {
        return getContract(partyId, contractId, Instant.now());
    }

    @Override
    public Contract getContract(String partyId, String contractId, long partyRevision) throws NotFoundException {
        return getContract(partyId, contractId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Contract getContract(String partyId, String contractId, Instant timestamp) throws NotFoundException {
        return getContract(partyId, contractId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public Contract getContract(String partyId, String contractId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.info("Trying to get contract, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId, partyRevisionParam);
        Party party = getParty(partyId, partyRevisionParam);

        Contract contract = party.getContracts().get(contractId);
        if (contract == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', contractId='%s', partyRevisionParam='%s'", partyId, contractId, partyRevisionParam));
        }
        log.info("Contract has been found, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId, partyRevisionParam);
        return contract;
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId) throws NotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, Instant.now());
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, long partyRevision) throws NotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, Instant timestamp) throws NotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.debug("Trying to get paymentInstitutionRef, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId, partyRevisionParam);
        Contract contract = getContract(partyId, contractId, partyRevisionParam);

        if (!contract.isSetPaymentInstitution()) {
            throw new NotFoundException(String.format("PaymentInstitutionRef not found, partyId='%s', contractId='%s', partyRevisionParam='%s'", partyId, contractId, partyRevisionParam));
        }

        PaymentInstitutionRef paymentInstitutionRef = contract.getPaymentInstitution();
        log.info("PaymentInstitutionRef has been found, partyId='{}', contractId='{}', paymentInstitutionRef='{}', partyRevisionParam='{}'", partyId, contractId, paymentInstitutionRef, partyRevisionParam);
        return paymentInstitutionRef;
    }

    @Override
    public com.rbkmoney.damsel.msgpack.Value getMetaData(String partyId, String namespace) throws NotFoundException {
        try {
            return partyManagementClient.getMetaData(userInfo, partyId, namespace);
        } catch (PartyMetaNamespaceNotFound ex) {
            return null;
        } catch (PartyNotFound ex) {
            throw new NotFoundException(
                    String.format("Party not found, partyId='%s', namespace='%s'", partyId, namespace),
                    ex
            );
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get namespace, partyId='%s', namespace='%s'", partyId, namespace), ex
            );
        }
    }

    @Override
    public List<FinalCashFlowPosting> computePayoutCashFlow(String partyId, String shopId, String payoutToolId, Cash amount, Instant timestamp) throws NotFoundException {
        PayoutParams payoutParams = new PayoutParams(shopId, amount, TypeUtil.temporalToString(timestamp));
        payoutParams.setPayoutToolId(payoutToolId);

        return computePayoutCashFlow(partyId, payoutParams);
    }

    @Override
    public List<FinalCashFlowPosting> computePayoutCashFlow(String partyId, PayoutParams payoutParams) throws NotFoundException {
        log.debug("Trying to compute payout cash flow, partyId='{}', payoutParams='{}'", partyId, payoutParams);
        try {
            List<FinalCashFlowPosting> finalCashFlowPostings = partyManagementClient.computePayoutCashFlow(userInfo, partyId, payoutParams);
            log.info("Payout cash flow has been computed, partyId='{}', payoutParams='{}', postings='{}'", partyId, payoutParams, finalCashFlowPostings);
            return finalCashFlowPostings;
        } catch (PartyNotFound | PartyNotExistsYet | ShopNotFound | PayoutToolNotFound ex) {
            throw new NotFoundException(String.format("%s, partyId='%s', payoutParams='%s'", ex.getClass().getSimpleName(), partyId, payoutParams), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to compute payout cash flow, partyId='%s', payoutParams='%s'", partyId, payoutParams), ex);
        }
    }

    @Override
    public long getPartyRevision(String partyId) throws NotFoundException {
        log.info("Trying to get party revision, partyId='{}'", partyId);
        try {
            long revision = partyManagementClient.getRevision(userInfo, partyId);
            log.info("Party revision has been found, partyId='{}', revision='{}'", partyId, revision);
            return revision;
        } catch (PartyNotFound ex) {
            throw new NotFoundException(String.format("Party not found, partyId='%s'", partyId), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get party revision, partyId='%s'", partyId), ex);
        }
    }

}

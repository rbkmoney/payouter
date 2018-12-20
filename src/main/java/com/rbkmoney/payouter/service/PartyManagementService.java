package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.PartyRevisionParam;
import com.rbkmoney.damsel.payment_processing.PayoutParams;
import com.rbkmoney.payouter.exception.NotFoundException;

import java.time.Instant;
import java.util.List;

public interface PartyManagementService {

    Party getParty(String partyId) throws NotFoundException;

    Party getParty(String partyId, Instant timestamp) throws NotFoundException;

    Party getParty(String partyId, long partyRevision) throws NotFoundException;

    Party getParty(String partyId, PartyRevisionParam partyRevisionParam) throws NotFoundException;

    Shop getShop(String partyId, String shopId) throws NotFoundException;

    Shop getShop(String partyId, String shopId, long partyRevision) throws NotFoundException;

    Shop getShop(String partyId, String shopId, Instant timestamp) throws NotFoundException;

    Shop getShop(String partyId, String shopId, PartyRevisionParam partyRevisionParam) throws NotFoundException;

    Contract getContract(String partyId, String contractId) throws NotFoundException;

    Contract getContract(String partyId, String contractId, long partyRevision) throws NotFoundException;

    Contract getContract(String partyId, String contractId, Instant timestamp) throws NotFoundException;

    Contract getContract(String partyId, String contractId, PartyRevisionParam partyRevisionParam) throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId) throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, long partyRevision) throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, Instant timestamp) throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, PartyRevisionParam partyRevisionParam) throws NotFoundException;

    Value getMetaData(String partyId, String namespace) throws NotFoundException;

    List<FinalCashFlowPosting> computePayoutCashFlow(String partyId, String shopId, String payoutToolId, Cash amount, Instant timestamp) throws NotFoundException;

    List<FinalCashFlowPosting> computePayoutCashFlow(String partyId, PayoutParams payoutParams) throws NotFoundException;

    long getPartyRevision(String partyId) throws NotFoundException;

}

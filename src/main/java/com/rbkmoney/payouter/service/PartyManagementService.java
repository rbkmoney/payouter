package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.CategoryType;
import com.rbkmoney.damsel.domain.Party;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.model.PayoutToolData;

import java.time.Instant;

public interface PartyManagementService {

    Party getParty(String partyId, Instant timestamp) throws NotFoundException;

    Shop getShop(String partyId, String shopId, Instant timestamp) throws NotFoundException;

    Value getMetaData(String partyId, String namespace) throws NotFoundException;

    PayoutToolData getPayoutToolData(String partyId, String shopId) throws InvalidStateException, NotFoundException;

    PayoutToolData getPayoutToolData(String partyId, String shopId, Instant timestamp) throws InvalidStateException, NotFoundException;

    CategoryType getCategoryType(String partyId, String shopId, long domainRevision, Instant timestamp) throws NotFoundException;

    boolean isTestCategoryType(String partyId, String shopId, long domainRevision, Instant timestamp) throws NotFoundException;

}

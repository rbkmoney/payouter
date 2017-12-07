package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.Contract;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.model.PayoutToolData;

public interface PartyManagementService {

    Shop getShop(String partyId, String shopId) throws NotFoundException;

    Contract getContract(String partyId, String contractId) throws NotFoundException;

    PayoutToolData getPayoutToolData(String partyId, String shopId) throws NotFoundException;

}

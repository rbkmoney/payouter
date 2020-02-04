package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.payouter.exception.InvalidChangesetException;
import com.rbkmoney.payouter.handler.CommitHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimCommitterService implements ClaimCommitterSrv.Iface {

    private final CommitHandler<ScheduleModification> partyModificationCommitHandler;

    @Override
    public void accept(String partyId, Claim receivedClaim) throws PartyNotFound, InvalidChangeset, TException {
        for (ModificationUnit modificationUnit : receivedClaim.getChangeset()) {
            Modification modification = modificationUnit.getModification();
            if (modification.isSetPartyModification()) {
                PartyModification partyModification = modification.getPartyModification();
                if (partyModification.isSetShopModification()) {
                    ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
                    String shopId = shopModificationUnit.getId();
                    ShopModification shopModification = shopModificationUnit.getModification();
                    if (shopModification.isSetPayoutScheduleModification()) {
                        try {
                            partyModificationCommitHandler.accept(partyId, shopId, shopModification.getPayoutScheduleModification());
                        } catch (InvalidChangesetException ex) {
                            throw new InvalidChangeset(ex.getMessage(), Collections.singletonList(modification));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void commit(String partyId, Claim claim) throws TException {
        for (ModificationUnit modificationUnit : claim.getChangeset()) {
            Modification modification = modificationUnit.getModification();
            if (modification.isSetPartyModification()) {
                PartyModification partyModification = modification.getPartyModification();
                if (partyModification.isSetShopModification()) {
                    ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
                    String shopId = shopModificationUnit.getId();
                    ShopModification shopModification = shopModificationUnit.getModification();
                    if (shopModification.isSetPayoutScheduleModification()) {
                        partyModificationCommitHandler.commit(partyId, shopId, shopModification.getPayoutScheduleModification());
                    }
                }
            }
        }
    }

}

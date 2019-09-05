package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.payouter.handler.CommitHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimCommitterService implements ClaimCommitterSrv.Iface {

    private final CommitHandler<PartyModification> partyModificationCommitHandler;

    @Override
    public void accept(String partyId, Claim receivedClaim) throws PartyNotFound, InvalidChangeset, TException {

        for (ModificationUnit modificationUnit : receivedClaim.getChangeset()) {
            Modification modification = modificationUnit.getModification();
            if (modification.isSetClaimModfication()) {
                log.info("Accepting for claim modification not implemented yet!");
            } else if (modification.isSetPartyModification()) {
                PartyModification partyModification = modification.getPartyModification();
                partyModificationCommitHandler.accept(partyId, partyModification);
            } else {
                log.info("Unknown modification!");
            }
        }
    }

    @Override
    public void commit(String partyId, Claim claim) throws TException {
        for (ModificationUnit modificationUnit : claim.getChangeset()) {
            Modification modification = modificationUnit.getModification();
            if (modification.isSetClaimModfication()) {
                log.info("Commit for claim modification not implemented yet!");
            } else if (modification.isSetPartyModification()) {
                PartyModification partyModification = modification.getPartyModification();
                partyModificationCommitHandler.commit(partyId, partyModification);
            } else {
                log.info("Unknown modification!");
            }
        }
    }

}

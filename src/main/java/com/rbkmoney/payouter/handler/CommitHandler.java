package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.claim_management.InvalidChangeset;
import com.rbkmoney.damsel.claim_management.PartyNotFound;
import org.apache.thrift.TException;

public interface CommitHandler<T> {

    void accept(String partyId, T claim) throws PartyNotFound, InvalidChangeset, TException;

    void commit(String partyId, T claim) throws TException;

}

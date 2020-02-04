package com.rbkmoney.payouter.handler;

public interface CommitHandler<T> {

    void accept(String partyId, String shopId, T claim);

    void commit(String partyId, String shopId, T claim);

}

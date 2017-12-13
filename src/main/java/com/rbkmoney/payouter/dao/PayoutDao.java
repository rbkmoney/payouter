package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.DaoException;

import java.util.Collection;
import java.util.List;

public interface PayoutDao extends GenericDao {

    Payout get(long payoutId) throws DaoException;

    Payout getExclusive(long payoutId) throws DaoException;

    List<Payout> get(Collection<Long> payoutIds) throws DaoException;

    long save(Payout payout) throws DaoException;

    void changeStatus(long payoutId, PayoutStatus payoutStatus) throws DaoException;

    List<Payout> getUnpaidPayouts() throws DaoException;

}

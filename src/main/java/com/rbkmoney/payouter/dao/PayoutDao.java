package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayoutDao extends GenericDao {

    Payout get(long payoutId) throws DaoException;

    List<Payout> get(Collection<Long> payoutIds) throws DaoException;

    long save(Payout payout) throws DaoException;

    void changeStatus(long payoutId, PayoutStatus payoutStatus) throws DaoException;

    List<Payout> getUnpaidPayouts() throws DaoException;

    List<Payout> search(Optional<PayoutStatus> payoutStatus, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTimer, Optional<List<Long>> payoutIds, long fromId, int size) throws DaoException;

}

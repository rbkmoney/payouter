package com.rbkmoney.payouter.dao;

import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayoutDao extends GenericDao {

    Payout get(long payoutId) throws DaoException;

    Payout getExclusive(long payoutId) throws DaoException;

    List<Payout> get(Collection<Long> payoutIds) throws DaoException;

    long save(Payout payout) throws DaoException;

    void changePurpose(long payoutId, String purpose) throws DaoException;

    void changeStatus(long payoutId, PayoutStatus payoutStatus) throws DaoException;

    List<Payout> getUnpaidPayoutsByAccountType(PayoutAccountType accountType) throws DaoException;

    List<Payout> search(
            Optional<PayoutStatus> payoutStatus,
            Optional<LocalDateTime> fromTime,
            Optional<LocalDateTime> toTime,
            Optional<List<Long>> payoutIds,
            Optional<Long> minAmount,
            Optional<Long> maxAmount,
            Optional<CurrencyRef> currency,
            Optional<Long> fromId,
            Optional<Integer> size
    ) throws DaoException;

    List<Payout> getByIds(List<Long> payoutIds) throws DaoException;
}

package com.rbkmoney.payouter.dao;

import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutRangeData;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface PayoutDao extends GenericDao {

    Payout get(String payoutId) throws DaoException;

    Payout getExclusive(String payoutId) throws DaoException;

    List<Payout> get(List<String> payoutIds) throws DaoException;

    long save(Payout payout) throws DaoException;

    void changeStatus(String payoutId, PayoutStatus payoutStatus) throws DaoException;

    PayoutRangeData getRangeData(String payoutId) throws DaoException;

    long saveRangeData(String payoutId, String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime) throws DaoException;

    int includeUnpaid(String payoutId, String partyId, String shopId) throws DaoException;

    int excludeFromPayout(String payoutId) throws DaoException;

    long getAvailableAmount(String payoutId) throws DaoException;

    List<Payout> getUnpaidPayoutsByAccountType(PayoutAccountType accountType) throws DaoException;

    List<Payout> search(
            PayoutStatus payoutStatus,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            List<String> payoutIds,
            Long minAmount,
            Long maxAmount,
            CurrencyRef currency,
            PayoutType payoutType,
            Long fromId,
            int size
    ) throws DaoException;

    List<Payout> getByIds(Set<String> payoutIds) throws DaoException;

    PayoutSummary getSummary(String payoutId) throws DaoException;

}

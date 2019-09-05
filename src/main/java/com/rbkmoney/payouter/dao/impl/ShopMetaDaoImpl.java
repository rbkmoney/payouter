package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.rbkmoney.payouter.domain.Tables.SHOP_META;

@Component
public class ShopMetaDaoImpl extends AbstractGenericDao implements ShopMetaDao {

    private final RowMapper<ShopMeta> shopMetaRowMapper;

    @Autowired
    public ShopMetaDaoImpl(DataSource dataSource) {
        super(dataSource);
        shopMetaRowMapper = new RecordRowMapper<>(SHOP_META, ShopMeta.class);
    }


    @Override
    public void save(ShopMeta shopMeta) throws DaoException {
        save(shopMeta.getPartyId(), shopMeta.getShopId());
    }

    @Override
    public void save(String partyId, String shopId) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().insertInto(SHOP_META)
                .set(SHOP_META.PARTY_ID, partyId)
                .set(SHOP_META.SHOP_ID, shopId)
                .set(SHOP_META.WTIME, now)
                .onDuplicateKeyUpdate()
                .set(SHOP_META.WTIME, now);

        executeOne(query);
    }

    @Override
    public void save(String partyId, String shopId, int calendarId, int schedulerId) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().insertInto(SHOP_META)
                .set(SHOP_META.PARTY_ID, partyId)
                .set(SHOP_META.SHOP_ID, shopId)
                .set(SHOP_META.CALENDAR_ID, calendarId)
                .set(SHOP_META.SCHEDULER_ID, schedulerId)
                .set(SHOP_META.WTIME, now)
                .onDuplicateKeyUpdate()
                .set(SHOP_META.CALENDAR_ID, calendarId)
                .set(SHOP_META.SCHEDULER_ID, schedulerId)
                .set(SHOP_META.WTIME, now);

        executeOne(query);
    }

    @Override
    public void save(String partyId, String shopId, int schedulerId) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().insertInto(SHOP_META)
                .set(SHOP_META.PARTY_ID, partyId)
                .set(SHOP_META.SHOP_ID, shopId)
                .set(SHOP_META.SCHEDULER_ID, schedulerId)
                .set(SHOP_META.WTIME, now)
                .onDuplicateKeyUpdate()
                .set(SHOP_META.SCHEDULER_ID, schedulerId)
                .set(SHOP_META.WTIME, now);

        executeOne(query);
    }

    @Override
    public ShopMeta get(String partyId, String shopId) throws DaoException {
        Query query = getDslContext().selectFrom(SHOP_META)
                .where(SHOP_META.PARTY_ID.eq(partyId)
                        .and(SHOP_META.SHOP_ID.eq(shopId)));
        return fetchOne(query, shopMetaRowMapper);
    }

    @Override
    public ShopMeta getExclusive(String partyId, String shopId) throws DaoException {
        Query query = getDslContext().selectFrom(SHOP_META)
                .where(SHOP_META.PARTY_ID.eq(partyId)
                        .and(SHOP_META.SHOP_ID.eq(shopId)))
                .forUpdate();

        return fetchOne(query, shopMetaRowMapper);
    }

    @Override
    public List<ShopMeta> getByCalendarAndSchedulerId(int calendarId, int schedulerId) throws DaoException {
        Query query = getDslContext().selectFrom(SHOP_META)
                .where(
                        SHOP_META.CALENDAR_ID.eq(calendarId)
                                .and(SHOP_META.SCHEDULER_ID.eq(schedulerId))
                );

        return fetch(query, shopMetaRowMapper);
    }

    @Override
    public List<ShopMeta> getAllActiveShops() throws DaoException {
        Query query = getDslContext().selectFrom(SHOP_META)
                .where(SHOP_META.SCHEDULER_ID.isNotNull()
                        .and(SHOP_META.CALENDAR_ID.isNotNull()));

        return fetch(query, shopMetaRowMapper);
    }

    @Override
    public void disableShop(String partyId, String shopId) throws DaoException {
        Query query = getDslContext().update(SHOP_META)
                .set(SHOP_META.CALENDAR_ID, (Integer) null)
                .set(SHOP_META.SCHEDULER_ID, (Integer) null)
                .where(SHOP_META.PARTY_ID.eq(partyId)
                        .and(SHOP_META.SHOP_ID.eq(shopId)));

        executeOne(query);
    }

    @Override
    public void updateLastPayoutCreatedAt(String partyId, String shopId, LocalDateTime payoutCreatedAt) throws DaoException {
        Query query = getDslContext()
                .update(SHOP_META)
                .set(SHOP_META.LAST_PAYOUT_CREATED_AT, payoutCreatedAt)
                .where(
                        SHOP_META.PARTY_ID.eq(partyId)
                                .and(SHOP_META.SHOP_ID.eq(shopId))
                );
        executeOne(query);
    }
}

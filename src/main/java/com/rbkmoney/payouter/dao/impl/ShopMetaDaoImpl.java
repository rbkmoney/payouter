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
        Query query = getDslContext().insertInto(SHOP_META)
                .set(SHOP_META.PARTY_ID, partyId)
                .set(SHOP_META.SHOP_ID, shopId)
                .onDuplicateKeyUpdate()
                .set(SHOP_META.WTIME, LocalDateTime.now());

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
}

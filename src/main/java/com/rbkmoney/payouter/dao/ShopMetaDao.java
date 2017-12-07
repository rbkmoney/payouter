package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.DaoException;

public interface ShopMetaDao extends GenericDao {

    void save(ShopMeta shopMeta) throws DaoException;

    void save(String partyId, String shopId) throws DaoException;

    ShopMeta get(String partyId, String shopId) throws DaoException;

    ShopMeta getExclusive(String partyId, String shopId) throws DaoException;

}

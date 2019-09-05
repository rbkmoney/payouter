package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;

public interface ShopMetaDao extends GenericDao {

    void save(ShopMeta shopMeta) throws DaoException;

    void save(String partyId, String shopId) throws DaoException;

    void save(String partyId, String shopId, int schedulerId) throws DaoException;

    void save(String partyId, String shopId, int calendarId, int schedulerId) throws DaoException;

    ShopMeta get(String partyId, String shopId) throws DaoException;

    ShopMeta getExclusive(String partyId, String shopId) throws DaoException;

    List<ShopMeta> getByCalendarAndSchedulerId(int calendarId, int schedulerId) throws DaoException;

    List<ShopMeta> getAllActiveShops() throws DaoException;

    void disableShop(String partyId, String shopId) throws DaoException;

    void updateLastPayoutCreatedAt(String partyId, String shopId, LocalDateTime payoutCreatedAt) throws DaoException;

}

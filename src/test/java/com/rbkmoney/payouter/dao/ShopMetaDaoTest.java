package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class ShopMetaDaoTest extends AbstractIntegrationTest {

    @Autowired
    private ShopMetaDao shopMetaDao;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    public void testSaveAndGetShopMeta() {
        String partyId = "partyId";
        String shopId = "shopId";

        shopMetaDao.save(partyId, shopId);
        ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);

        shopMetaDao.save(partyId, shopId);
        assertTrue(shopMeta.getWtime().isBefore(shopMetaDao.get(partyId, shopId).getWtime()));
    }

    @Test
    public void testGetExclusive() throws InterruptedException {
        String partyId = "partyId";
        String shopId = "shopId";
        long sleep = 1000L;

        shopMetaDao.save(partyId, shopId);

        ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() ->
                transactionTemplate.execute((status) -> {
                    ShopMeta exclusive = shopMetaDao.getExclusive(partyId, shopId);
                    latch.countDown();
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {}
                    shopMetaDao.save(partyId, shopId);
                    return exclusive;
                })
        ).start();

        latch.await();

        long start = System.currentTimeMillis();
        ShopMeta newShopMeta = transactionTemplate.execute((status) -> shopMetaDao.getExclusive(partyId, shopId));
        assertTrue(sleep <= System.currentTimeMillis() - start);
        assertTrue(shopMeta.getWtime().isBefore(newShopMeta.getWtime()));

    }

}

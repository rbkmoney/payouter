package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.github.benas.randombeans.api.EnhancedRandom.randomListOf;
import static org.junit.Assert.*;

public class PaymentDaoTest extends AbstractIntegrationTest {

    @Autowired
    PaymentDao paymentDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Payment payment = random(Payment.class);

        paymentDao.save(payment);

        assertEquals(payment, paymentDao.get(payment.getInvoiceId(), payment.getPaymentId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        Payment payment = random(Payment.class, "payoutId", "externalFee", "capturedAt", "terminalId", "domainRevision");
        paymentDao.save(payment);
        assertEquals(payment, paymentDao.get(payment.getInvoiceId(), payment.getPaymentId()));
    }
    
}

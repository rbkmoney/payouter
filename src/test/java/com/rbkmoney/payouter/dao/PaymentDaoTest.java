package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.DaoException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.List;

import static com.rbkmoney.payouter.dao.DaoTestUtil.getNullColumnNames;
import static com.rbkmoney.payouter.domain.Tables.PAYMENT;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.github.benas.randombeans.api.EnhancedRandom.randomListOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PaymentDaoTest extends AbstractIntegrationTest {

    @Autowired
    PaymentDao paymentDao;

    @Test
    public void testSaveAndGet() throws DaoException {
        Payment payment = random(Payment.class);

        paymentDao.save(payment);

        //save again
        try {
            paymentDao.save(payment);
            fail();
        } catch (DaoException ex) {
            assertTrue(DuplicateKeyException.class.isAssignableFrom(ex.getCause().getClass()));
        }

        assertEquals(payment, paymentDao.get(payment.getInvoiceId(), payment.getPaymentId()));
    }

    @Test
    public void testSaveOnlyNonNullValues() throws DaoException {
        System.out.println(Arrays.toString(getNullColumnNames(PAYMENT)));
        Payment payment = random(Payment.class, getNullColumnNames(PAYMENT));
        paymentDao.save(payment);
    }

    @Test
    public void testIncludeAndExcludeFromPayout() throws DaoException {
        List<Payment> payments = randomListOf(10, Payment.class, "payoutId");
        long payoutId = 1;

        payments.stream().forEach(payment -> paymentDao.save(payment));

        assertTrue(paymentDao.getByPayoutId(payoutId).isEmpty());
        assertEquals(payments.size(), paymentDao.includeToPayout(payoutId, payments));
        assertEquals(payments.size(), paymentDao.getByPayoutId(payoutId).size());
        assertEquals(payments.size(), paymentDao.excludeFromPayout(payoutId));
        assertTrue(paymentDao.getByPayoutId(payoutId).isEmpty());
    }

}

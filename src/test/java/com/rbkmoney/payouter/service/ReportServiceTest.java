package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.message_sender.Message;
import com.rbkmoney.damsel.message_sender.MessageSenderSrv;
import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.enums.CashFlowType;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.service.impl.NonresidentsReportServiceImpl;
import com.rbkmoney.payouter.service.impl.ResidentsReportServiceImpl;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static io.github.benas.randombeans.api.EnhancedRandom.randomListOf;
import static io.github.benas.randombeans.api.EnhancedRandom.randomStreamOf;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

public class ReportServiceTest extends AbstractIntegrationTest {

    @Autowired
    ResidentsReportServiceImpl residentsReportService;

    @Autowired
    NonresidentsReportServiceImpl nonresidentsReportService;

    @Autowired
    PayoutDao payoutDao;

    @Autowired
    ReportDao reportDao;

    @Autowired
    CashFlowDescriptionDao cashFlowDescriptionDao;

    @MockBean
    MessageSenderSrv.Iface dudoser;

    @Test
    public void testCreateReportForResidents() throws TException, InterruptedException {
        List<Payout> payouts = randomStreamOf(10, Payout.class)
                .map(payout -> {
                    payout.setAccountType(PayoutAccountType.russian_payout_account);
                    payout.setStatus(PayoutStatus.UNPAID);
                    return payout;
                }).collect(Collectors.toList());
        payouts.forEach(payout -> {
            long payoutId = payoutDao.save(payout);
            List<CashFlowDescription> cfds = randomListOf(2, CashFlowDescription.class);
            cfds.forEach(cfd -> cfd.setPayoutId(String.valueOf(payoutId)));
            cfds.get(0).setCashFlowType(CashFlowType.payment);
            cfds.get(1).setCashFlowType(CashFlowType.refund);
            cashFlowDescriptionDao.save(cfds);
        });

        Report report = reportDao.get(residentsReportService.generateAndSave(payouts));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(answer -> {
            countDownLatch.countDown();
            Message message = answer.getArgumentAt(0, Message.class);
            assertTrue(Arrays.equals(report.getContent().getBytes(report.getEncoding()), message.getMessageMail().getAttachments().get(0).getData()));
            return null;
        }).when(dudoser).send(any());

        residentsReportService.createNewReportsJob();
        assertTrue(payoutDao.getUnpaidPayoutsByAccountType(PayoutAccountType.russian_payout_account).isEmpty());
        countDownLatch.await();
    }

    @Test
    public void testCreateReportForNonresidents() {
        List<Payout> payouts = randomStreamOf(10, Payout.class)
                .map(payout -> {
                    payout.setAccountType(PayoutAccountType.international_payout_account);
                    payout.setStatus(PayoutStatus.UNPAID);
                    return payout;
                }).collect(Collectors.toList());
        payouts.forEach(payout -> {
            long payoutId = payoutDao.save(payout);
            List<CashFlowDescription> cfds = randomListOf(2, CashFlowDescription.class);
            cfds.forEach(cfd -> cfd.setPayoutId(String.valueOf(payoutId)));
            cfds.get(0).setCashFlowType(CashFlowType.payment);
            cfds.get(1).setCashFlowType(CashFlowType.refund);
            cashFlowDescriptionDao.save(cfds);
        });

        nonresidentsReportService.createNewReportsJob();
        assertTrue(payoutDao.getUnpaidPayoutsByAccountType(PayoutAccountType.international_payout_account).isEmpty());
    }
}

package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payout_processing.GeneratePayoutParams;
import com.rbkmoney.damsel.payout_processing.PayoutManagementSrv;
import com.rbkmoney.damsel.payout_processing.ShopParams;
import com.rbkmoney.damsel.payout_processing.TimeRange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.generation.*;
import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.meta.UserIdentityIdExtensionKit;
import com.rbkmoney.payouter.meta.UserIdentityRealmExtensionKit;
import com.rbkmoney.payouter.model.PayoutToolData;
import com.rbkmoney.woody.api.flow.WFlow;
import com.rbkmoney.woody.api.trace.ContextUtils;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.rbkmoney.payouter.domain.enums.PayoutStatus.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

public class PayoutServiceTest extends AbstractIntegrationTest {

    @Autowired
    EventStockService eventStockService;

    @Autowired
    PayoutService payoutService;

    @Autowired
    PayoutDao payoutDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockBean
    PartyManagementService partyManagementService;

    @MockBean
    ShumwayService shumwayService;

    EventsGenerator eventsGenerator;

    InvoicePaymentGenerator invoicePaymentGenerator;

    AdjustmentGenerator adjustmentGenerator;

    RefundGenerator refundGenerator;

    PayoutManagementSrv.Iface client;

    WFlow wFlow = new WFlow();

    String partyId = "owner_id";

    String shopId = "test-shop-id";

    @Before
    public void setUp() throws URISyntaxException {
        client = new THSpawnClientBuilder()
                .withAddress(new URI("http://localhost:" + port + "/payout/management"))
                .withNetworkTimeout(0)
                .build(PayoutManagementSrv.Iface.class);

        GeneratorConfig generatorConfig = new GeneratorConfig();

        eventsGenerator = new EventsGenerator(generatorConfig);
        invoicePaymentGenerator = new InvoicePaymentGenerator(generatorConfig);
        adjustmentGenerator = new AdjustmentGenerator(generatorConfig);
        refundGenerator = new RefundGenerator(generatorConfig);
    }

    @After
    public void cleanUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate,
                "sht.payout", "sht.payment", "sht.adjustment", "sht.refund");
    }

    @Test(expected = InvalidRequest.class)
    public void testCreatePayoutWithoutFunds() throws Exception {
        GeneratePayoutParams generatePayoutParams = new GeneratePayoutParams();
        ShopParams shopParams = new ShopParams();
        shopParams.setPartyId(partyId);
        shopParams.setShopId(shopId);
        generatePayoutParams.setShop(shopParams);

        TimeRange timeRange = new TimeRange();
        timeRange.setFromTime(TypeUtil.temporalToString(LocalDateTime.now()));
        timeRange.setToTime(TypeUtil.temporalToString(LocalDateTime.now()));
        generatePayoutParams.setTimeRange(timeRange);

        callService(() -> client.generatePayouts(generatePayoutParams));
    }

    @Test
    public void testCreatePaidConfirmAndCancelPayout() throws Exception {
        given(partyManagementService.getPayoutToolData(partyId, shopId))
                .willReturn(random(PayoutToolData.class));

        addCapturedPayment();

        GeneratePayoutParams generatePayoutParams = new GeneratePayoutParams();
        ShopParams shopParams = new ShopParams();
        shopParams.setPartyId(partyId);
        shopParams.setShopId(shopId);
        generatePayoutParams.setShop(shopParams);
        generatePayoutParams.setTimeRange(new TimeRange("2015-06-17T00:00:00Z", "2018-06-17T00:00:00Z"));

        List<String> payoutIds = callService(() -> client.generatePayouts(generatePayoutParams));
        long payoutId = Long.valueOf(payoutIds.get(0));
        Payout payout = payoutDao.get(payoutId);
        assertEquals(9500L, (long) payout.getAmount());
        assertEquals(UNPAID, payout.getStatus());

        Set<String> uniquePayoutsIds = new HashSet<>(payoutIds);
        payoutService.pay(payoutId);
        assertEquals(PAID, payoutDao.get(payoutId).getStatus());

        assertEquals(uniquePayoutsIds, callService(() -> client.confirmPayouts(uniquePayoutsIds)));
        assertEquals(CONFIRMED, payoutDao.get(payoutId).getStatus());

        assertEquals(uniquePayoutsIds, callService(() -> client.cancelPayouts(uniquePayoutsIds, "так вышло")));
        assertEquals(CANCELLED, payoutDao.get(payoutId).getStatus());

    }

    public void addCapturedPayment() {
        Event invoiceCreated = invoicePaymentGenerator.createInvoiceCreated();
        Event paymentStarted = invoicePaymentGenerator.createInvoicePaymentStarted();
        Event paymentCaptured = invoicePaymentGenerator.createPaymentStatusChanged();

        eventStockService.processStockEvent(buildStockEvent(invoiceCreated));
        eventStockService.processStockEvent(buildStockEvent(paymentStarted));
        eventStockService.processStockEvent(buildStockEvent(paymentCaptured));
    }

    public StockEvent buildStockEvent(Event event) {
        SourceEvent sourceEvent = new SourceEvent();
        sourceEvent.setProcessingEvent(event);

        StockEvent stockEvent = new StockEvent();
        stockEvent.setSourceEvent(sourceEvent);
        return stockEvent;
    }

    private <T> T callService(Callable<T> callable) throws Exception {
        return wFlow.createServiceFork(
                () -> {
                    ContextUtils.setCustomMetadataValue(UserIdentityIdExtensionKit.KEY, "test");
                    ContextUtils.setCustomMetadataValue(UserIdentityRealmExtensionKit.KEY, "internal");
                    return callable.call();
                }).call();
    }

}

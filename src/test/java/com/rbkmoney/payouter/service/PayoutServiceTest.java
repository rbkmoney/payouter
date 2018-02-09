package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payout_processing.GeneratePayoutParams;
import com.rbkmoney.damsel.payout_processing.PayoutManagementSrv;
import com.rbkmoney.damsel.payout_processing.ShopParams;
import com.rbkmoney.damsel.payout_processing.TimeRange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.generation.AdjustmentGenerator;
import com.rbkmoney.generation.GeneratorConfig;
import com.rbkmoney.generation.InvoicePaymentGenerator;
import com.rbkmoney.generation.RefundGenerator;
import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.meta.UserIdentityIdExtensionKit;
import com.rbkmoney.payouter.meta.UserIdentityRealmExtensionKit;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;

import static com.rbkmoney.payouter.domain.enums.PayoutStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

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

    PayoutManagementSrv.Iface client;

    WFlow wFlow = new WFlow();

    String partyId = "owner_id";

    String shopId = "test-shop-id";

    String contractId = "test-contract-id";

    String payoutToolId = "test-payout-tool";

    @Before
    public void setUp() throws URISyntaxException {
        client = new THSpawnClientBuilder()
                .withAddress(new URI("http://localhost:" + port + "/payout/management"))
                .withNetworkTimeout(0)
                .build(PayoutManagementSrv.Iface.class);

        given(partyManagementService.getParty(any(), any(Instant.class)))
                .willReturn(buildParty(partyId, shopId, contractId, payoutToolId));
        given(partyManagementService.getMetaData(any(), any()))
                .willReturn(null);
    }

    @After
    public void cleanUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate,
                "sht.payout", "sht.payment", "sht.adjustment", "sht.refund");
    }

    @Test
    public void testCreatePayoutWithAdjustment() throws Exception {
        addCapturedPayment("adjustment-id");
        addCapturedAdjustment("adjustment-id");

        GeneratePayoutParams generatePayoutParams = new GeneratePayoutParams();
        ShopParams shopParams = new ShopParams();
        shopParams.setPartyId(partyId);
        shopParams.setShopId(shopId);
        generatePayoutParams.setShop(shopParams);
        generatePayoutParams.setTimeRange(new TimeRange("2015-06-17T00:00:00Z", "2018-06-17T00:00:00Z"));

        List<String> payoutIds = callService(() -> client.generatePayouts(generatePayoutParams));
        assertEquals(1, payoutIds.size());
        long payoutId = Long.valueOf(payoutIds.get(0));

        payoutService.pay(payoutId);

        Payout payout = payoutDao.get(payoutId);
        assertEquals(Long.valueOf(9600L), payout.getAmount());
        assertEquals(PAID, payout.getStatus());
        assertEquals(partyId, payout.getPartyId());
    }

    @Test
    public void createPayoutWithRefund() throws Exception {
        addCapturedPayment("refund-id");
        addCapturedRefund("refund-id");
        addCapturedPayment();

        GeneratePayoutParams generatePayoutParams = new GeneratePayoutParams();
        ShopParams shopParams = new ShopParams();
        shopParams.setPartyId(partyId);
        shopParams.setShopId(shopId);
        generatePayoutParams.setTimeRange(new TimeRange("2015-06-17T00:00:00Z", "2018-06-17T00:00:00Z"));

        List<String> payoutIds = callService(() -> client.generatePayouts(generatePayoutParams));
        assertEquals(1, payoutIds.size());
        long payoutId = Long.valueOf(payoutIds.get(0));

        payoutService.pay(payoutId);

        Payout payout = payoutDao.get(payoutId);
        assertEquals(Long.valueOf(9000L), payout.getAmount());
        assertEquals(PAID, payout.getStatus());
        assertEquals(partyId, payout.getPartyId());
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
    public void testCreatePayoutsWithInvalidStateException() throws Exception {
        given(partyManagementService.getMetaData(any(), any()))
                .willReturn(Value.b(true));
        addCapturedPayment("payment-id");

        GeneratePayoutParams generatePayoutParams = new GeneratePayoutParams();
        generatePayoutParams.setTimeRange(new TimeRange("2015-06-17T00:00:00Z", "2018-06-17T00:00:00Z"));

        List<String> payoutIds = callService(() -> client.generatePayouts(generatePayoutParams));
        assertTrue(payoutIds.isEmpty());
    }

    @Test
    public void testCreatePaidConfirmAndCancelPayout() throws Exception {
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

    public void addCapturedAdjustment() {
        addCapturedAdjustment("for-adjustment");
    }

    public void addCapturedAdjustment(String invoiceId) {
        GeneratorConfig generatorConfig = new GeneratorConfig();
        generatorConfig.setInvoiceId(invoiceId);

        AdjustmentGenerator adjustmentGenerator = new AdjustmentGenerator(generatorConfig);

        Event adjustmentCreated = adjustmentGenerator.createAdjustmentCreated();
        Event adjustmentStatusChanged = adjustmentGenerator.createAdjustmentStatusChanged();

        eventStockService.processStockEvent(buildStockEvent(adjustmentCreated));
        eventStockService.processStockEvent(buildStockEvent(adjustmentStatusChanged));
    }

    public void addCapturedRefund() {
        addCapturedRefund("for-refund");
    }

    public void addCapturedRefund(String invoiceId) {
        GeneratorConfig generatorConfig = new GeneratorConfig();
        generatorConfig.setInvoiceId(invoiceId);

        RefundGenerator refundGenerator = new RefundGenerator(generatorConfig);

        Event refundCreated = refundGenerator.createRefundCreated();
        Event refundCaptured = refundGenerator.createRefundCaptured();

        eventStockService.processStockEvent(buildStockEvent(refundCreated));
        eventStockService.processStockEvent(buildStockEvent(refundCaptured));
    }

    public void addCapturedPayment() {
        addCapturedPayment("for-payment");
    }

    public void addCapturedPayment(String invoiceId) {
        GeneratorConfig generatorConfig = new GeneratorConfig();
        generatorConfig.setInvoiceId(invoiceId);

        InvoicePaymentGenerator invoicePaymentGenerator = new InvoicePaymentGenerator(generatorConfig);

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

    private Party buildParty(String partyId, String shopId, String contractId, String payoutToolId) {
        Instant timestamp = Instant.now();

        Party party = new Party();
        party.setId(partyId);
        party.setBlocking(Blocking.unblocked(new Unblocked("", TypeUtil.temporalToString(timestamp))));
        party.setCreatedAt(TypeUtil.temporalToString(timestamp));
        party.setRevision(1L);
        party.setContactInfo(new PartyContactInfo("me@party.com"));
        party.setShops(buildShops(shopId, contractId, payoutToolId));
        party.setContracts(buildContracts(contractId, payoutToolId));
        return party;
    }

    private Map<String, Contract> buildContracts(String contractId, String payoutToolId) {
        Map<String, Contract> contracts = new HashMap<>();
        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setLegalAgreement(new LegalAgreement(
                TypeUtil.temporalToString(Instant.now()),
                "12/12")
        );
        contract.setPayoutTools(Arrays.asList(
                new PayoutTool(
                        payoutToolId,
                        TypeUtil.temporalToString(Instant.now()),
                        new CurrencyRef("RUB"),
                        PayoutToolInfo.international_bank_account(
                                new InternationalBankAccount(
                                        "123",
                                        "123",
                                        "123",
                                        "123",
                                        "123"
                                )
                        )
                )
        ));
        contract.setContractor(
                Contractor.legal_entity(
                        LegalEntity.international_legal_entity(new InternationalLegalEntity(
                                "kek",
                                "711-2880 Nulla St. Mankato Mississippi 96522"
                        ))
                )
        );
        contracts.put(contractId, contract);
        return contracts;
    }

    private Map<String, Shop> buildShops(String shopId, String contractId, String payoutToolId) {
        Map<String, Shop> shops = new HashMap<>();
        Shop shop = new Shop();
        shop.setContractId(contractId);
        shop.setAccount(new ShopAccount(
                new CurrencyRef("RUB"),
                1,
                2,
                3
        ));
        shop.setPayoutToolId(payoutToolId);
        shops.put(shopId, shop);

        return shops;
    }

}

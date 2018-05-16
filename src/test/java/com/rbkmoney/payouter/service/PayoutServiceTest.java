package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain.Calendar;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.domain_config.VersionedObject;
import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.*;
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
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
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
import static org.mockito.Matchers.eq;

public class PayoutServiceTest extends AbstractIntegrationTest {

    @Autowired
    EventStockService eventStockService;

    @Autowired
    PayoutService payoutService;

    @Autowired
    PayoutDao payoutDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Scheduler scheduler;

    @MockBean
    PartyManagementSrv.Iface partyManagementClient;

    @MockBean
    RepositoryClientSrv.Iface dominantClient;

    @MockBean
    ShumwayService shumwayService;

    PayoutManagementSrv.Iface client;

    WFlow wFlow = new WFlow();

    String partyId = "owner_id";

    String shopId = "test-shop-id";

    String contractId = "test-contract-id";

    String payoutToolId = "test-payout-tool";

    @Before
    public void setUp() throws URISyntaxException, TException {
        client = new THSpawnClientBuilder()
                .withAddress(new URI("http://localhost:" + port + "/payout/management"))
                .withNetworkTimeout(0)
                .build(PayoutManagementSrv.Iface.class);

        given(partyManagementClient.checkout(any(), any(), any()))
                .willReturn(buildParty(partyId, shopId, contractId, payoutToolId));
        given(partyManagementClient.getMetaData(any(), any(), any()))
                .willReturn(null);
        given(partyManagementClient.computePayoutCashFlow(any(), any(), any()))
                .willAnswer(answer -> {
                    PayoutParams payoutParams = answer.getArgumentAt(2, PayoutParams.class);
                    return Arrays.asList(
                            new FinalCashFlowPosting(
                                    new FinalCashFlowAccount(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement), 12),
                                    new FinalCashFlowAccount(CashFlowAccount.merchant(MerchantCashFlowAccount.payout), 13),
                                    payoutParams.getAmount()
                            )
                    );
                });
        given(dominantClient.checkoutObject(any(), eq(Reference.payment_institution(new PaymentInstitutionRef(1)))))
                .willReturn(buildPaymentInstitutionObject(new PaymentInstitutionRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.calendar(new CalendarRef(1)))))
                .willReturn(buildPaymentCalendarObject(new CalendarRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.business_schedule(new BusinessScheduleRef(1)))))
                .willReturn(buildPayoutScheduleObject(new BusinessScheduleRef(1)));
        given(dominantClient.checkoutObject(any(), eq(Reference.category(new CategoryRef(1)))))
                .willReturn(buildCategoryObject(new CategoryRef(1)));
    }

    @After
    public void cleanUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate,
                "sht.payout", "sht.payment", "sht.adjustment", "sht.refund");
    }

    @Test
    public void testCreatePayoutWithScheduler() {
        eventStockService.processStockEvent(
                buildStockEvent(buildScheduleEvent(partyId, shopId))
        );
        addCapturedPayment();

        List<Payout> payouts;
        do {
            payouts = payoutService.search(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(1));
        } while (payouts.isEmpty());

        assertEquals(1, payouts.size());
        Payout payout = payouts.get(0);
        assertEquals(9500L, (long) payout.getAmount());
        assertEquals(UNPAID, payout.getStatus());
    }

    @Test
    public void testRegisterAndDeregisterScheduler() throws SchedulerException {
        eventStockService.processStockEvent(
                buildStockEvent(buildScheduleEvent(partyId, shopId))
        );
        assertTrue(!scheduler.getJobKeys(GroupMatcher.anyGroup()).isEmpty());
        assertTrue(!scheduler.getTriggerKeys(GroupMatcher.anyGroup()).isEmpty());
        eventStockService.processStockEvent(
                buildStockEvent(buildScheduleEvent(partyId, shopId, null))
        );
        assertTrue(scheduler.getJobKeys(GroupMatcher.anyGroup()).isEmpty());
        assertTrue(scheduler.getTriggerKeys(GroupMatcher.anyGroup()).isEmpty());
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
        given(partyManagementClient.getMetaData(any(), any(), any()))
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

    @Test
    public void testGeneratePayoutReport() throws Exception {
        addCapturedPayment();

        GeneratePayoutParams generatePayoutParams = new GeneratePayoutParams();
        ShopParams shopParams = new ShopParams();
        shopParams.setPartyId(partyId);
        shopParams.setShopId(shopId);
        generatePayoutParams.setShop(shopParams);
        generatePayoutParams.setTimeRange(new TimeRange("2015-06-17T00:00:00Z", "2018-06-17T00:00:00Z"));

        List<String> payoutIds = callService(() -> client.generatePayouts(generatePayoutParams));

        addCapturedPayment("for-payment-2");
        payoutIds.addAll(callService(() -> client.generatePayouts(generatePayoutParams)));

        runService(() -> {
            try {
                client.generateReport(Collections.singleton("fff"));
            } catch (InvalidRequest e) {
                assertTrue(e.getErrors().get(0).contains("Couldn't convert to long value."));
            } catch (TException e) {
            }
        });

        long maxId = payoutIds.stream().mapToLong(Long::valueOf).max().getAsLong();
        runService(() -> {
            try {
                client.generateReport(Collections.singleton(String.valueOf(maxId + 1)));
            } catch (InvalidRequest e) {
                assertTrue(e.getErrors().get(0).contains("Some of payouts not found: " + Collections.singletonList(maxId + 1)));
            } catch (TException e) {
            }
        });

        long payoutId = Long.valueOf(payoutIds.get(0));
        payoutService.pay(payoutId);
        runService(() -> {
            try {
                client.generateReport(new HashSet<>(payoutIds));
            } catch (InvalidRequest e) {
                assertTrue(e.getErrors().get(0).contains("Payout " + payoutId + " has wrong status; it should be UNPAID"));
            } catch (TException e) {
            }
        });
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

    private void runService(Runnable runnable) {
        wFlow.createServiceFork(
                () -> {
                    ContextUtils.setCustomMetadataValue(UserIdentityIdExtensionKit.KEY, "test");
                    ContextUtils.setCustomMetadataValue(UserIdentityRealmExtensionKit.KEY, "internal");
                    runnable.run();
                }).run();
    }

    private Event buildScheduleEvent(String partyId, String shopId) {
        return buildScheduleEvent(partyId, shopId, new BusinessScheduleRef(1));
    }

    private Event buildScheduleEvent(String partyId, String shopId, BusinessScheduleRef payoutScheduleRef) {
        ClaimStatusChanged claimStatusChanged = new ClaimStatusChanged();
        ClaimAccepted claimAccepted = new ClaimAccepted();
        ClaimEffect claimEffect = new ClaimEffect();
        ShopEffectUnit shopEffectUnit = new ShopEffectUnit();
        shopEffectUnit.setShopId(shopId);

        ScheduleChanged scheduleChanged = new ScheduleChanged();
        scheduleChanged.setSchedule(payoutScheduleRef);
        shopEffectUnit.setEffect(ShopEffect.payout_schedule_changed(scheduleChanged));
        claimEffect.setShopEffect(shopEffectUnit);
        claimAccepted.setEffects(Arrays.asList(claimEffect));
        claimStatusChanged.setStatus(ClaimStatus.accepted(claimAccepted));

        return new Event(
                1,
                TypeUtil.temporalToString(Instant.now()),
                EventSource.party_id(partyId),
                EventPayload.party_changes(Arrays.asList(PartyChange.claim_status_changed(claimStatusChanged)))
        );
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

    private VersionedObject buildPayoutScheduleObject(BusinessScheduleRef payoutScheduleRef) {
        ScheduleEvery nth5 = new ScheduleEvery();
        nth5.setNth((byte) 5);

        BusinessSchedule payoutSchedule = new BusinessSchedule();
        payoutSchedule.setName("schedule");
        payoutSchedule.setSchedule(new Schedule(
                ScheduleYear.every(new ScheduleEvery()),
                ScheduleMonth.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleDayOfWeek.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery(nth5))
        ));
        payoutSchedule.setPolicy(new PayoutCompilationPolicy(new TimeSpan()));

        return new VersionedObject(
                1,
                DomainObject.business_schedule(new BusinessScheduleObject(
                        payoutScheduleRef,
                        payoutSchedule
                ))
        );
    }

    private VersionedObject buildCategoryObject(CategoryRef categoryRef) {
        Category category = new Category();
        category.setType(CategoryType.live);

        return new VersionedObject(
                1,
                DomainObject.category(new CategoryObject(
                        categoryRef,
                        category
                ))
        );
    }

    private VersionedObject buildPaymentCalendarObject(CalendarRef calendarRef) {
        Calendar calendar = new Calendar("calendar", "Europe/Moscow", Collections.emptyMap());

        return new VersionedObject(
                1,
                DomainObject.calendar(new CalendarObject(
                        calendarRef,
                        calendar
                ))
        );
    }

    private VersionedObject buildPaymentInstitutionObject(PaymentInstitutionRef paymentInstitutionRef) {
        PaymentInstitution paymentInstitution = new PaymentInstitution();
        paymentInstitution.setCalendar(new CalendarRef(1));

        return new VersionedObject(
                1,
                DomainObject.payment_institution(new PaymentInstitutionObject(
                        paymentInstitutionRef,
                        paymentInstitution
                ))
        );
    }

    private Map<String, Contract> buildContracts(String contractId, String payoutToolId) {
        Map<String, Contract> contracts = new HashMap<>();
        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setLegalAgreement(new LegalAgreement(
                TypeUtil.temporalToString(Instant.now()),
                "12/12")
        );
        contract.setPaymentInstitution(new PaymentInstitutionRef(1));
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
        shop.setLocation(ShopLocation.url("http://2ch.ru"));
        shop.setContractId(contractId);
        shop.setCategory(new CategoryRef(1));
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

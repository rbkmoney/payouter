package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.github.benas.randombeans.api.EnhancedRandom.randomStreamOf;
import static org.junit.Assert.assertEquals;

public class EventSinkTest extends AbstractIntegrationTest {

    @Autowired
    EventSinkService eventSinkService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    EventSinkSrv.Iface client;

    @Before
    public void setup() throws URISyntaxException {
        client = new THSpawnClientBuilder()
                .withAddress(new URI("http://localhost:" + port + "/repo")).build(EventSinkSrv.Iface.class);
    }

    @Test
    public void getPayoutEventFromEventSinkTest() throws TException {
        PayoutEvent payoutEvent = random(PayoutEvent.class);
        payoutEvent.setPayoutId("12345");
        payoutEvent.setEventType("payout_created");
        payoutEvent.setPayoutStatus("paid");
        payoutEvent.setPayoutType(PayoutType.bank_account.toString());
        payoutEvent.setPayoutAccountType(random(PayoutAccountType.class).toString());
        payoutEvent.setPayoutCashFlow("[{\"source\":{\"account_type\":{\"external\":\"outcome\"},\"account_id\":3597919171377506497},\"destination\":{\"account_type\":{\"external\":\"outcome\"},\"account_id\":6831933432983240766},\"volume\":{\"amount\":5211427573358778888,\"currency\":{\"symbolic_code\":\"\"}},\"details\":\"1H 7\"},{\"source\":{\"account_type\":{\"system\":\"settlement\"},\"account_id\":5023621675459118179},\"destination\":{\"account_type\":{\"merchant\":\"settlement\"},\"account_id\":5917509550248754017},\"volume\":{\"amount\":-4222063269285646320,\"currency\":{\"symbolic_code\":\"_Xhj+XMxL*\"}},\"details\":\"(\"},{\"source\":{\"account_type\":{\"provider\":\"settlement\"},\"account_id\":-5960687128262570088},\"destination\":{\"account_type\":{\"system\":\"settlement\"},\"account_id\":-7162006304489189748},\"volume\":{\"amount\":5968107316042487537,\"currency\":{\"symbolic_code\":\"9AEc=`4q((+\"}},\"details\":\"`s\"},{\"source\":{\"account_type\":{\"external\":\"income\"},\"account_id\":-6956345410083087622},\"destination\":{\"account_type\":{\"system\":\"settlement\"},\"account_id\":-1431660791080627688},\"volume\":{\"amount\":-8572831265387909550,\"currency\":{\"symbolic_code\":\"RKT/x<a.\"}},\"details\":\"lYU.V%k(Y\"},{\"source\":{\"account_type\":{\"external\":\"income\"},\"account_id\":2719813353463804457},\"destination\":{\"account_type\":{\"external\":\"outcome\"},\"account_id\":-4963934282946639877},\"volume\":{\"amount\":-8810554754230626184,\"currency\":{\"symbolic_code\":\"^$xfA250z%iEnI\"}},\"details\":\"B fP\"}]");

        eventSinkService.saveEvent(payoutEvent);

        EventRange eventRange = new EventRange();
        eventRange.setAfter(payoutEvent.getEventId());
        eventRange.setLimit(0);
        assertEquals(0, client.getEvents(eventRange).size());

        List<Event> events = client.getEvents(new EventRange(1));

        assertEquals(1, events.size());
        assertEquals(eventSinkService.getEvents(Optional.empty(), 1).size(), events.size());

        Event event = events.get(0);
        assertEquals((long) payoutEvent.getEventId(), event.getId());
        assertEquals(payoutEvent.getPayoutId(), event.getSource().getPayoutId());
        assertEquals(payoutEvent.getPayoutId(), event.getPayload().getPayoutChanges().get(0).getPayoutCreated().getPayout().getId());
    }

    @Test(expected = NoLastEvent.class)
    public void NoLastEventTest() throws TException {
        client.getLastEventID();
    }

    @Test(expected = EventNotFound.class)
    public void afterNotFoundTest() throws TException {
        EventRange eventRange = new EventRange();
        eventRange.setAfter(0);
        eventRange.setLimit(10);

        client.getEvents(eventRange);
    }

    @Test(expected = InvalidRequest.class)
    public void negativeLimitTest() throws TException {
        PayoutEvent payoutEvent = random(PayoutEvent.class);
        eventSinkService.saveEvent(payoutEvent);

        EventRange eventRange = new EventRange();
        eventRange.setAfter(payoutEvent.getEventId());
        eventRange.setLimit(-1);
        client.getEvents(eventRange);
    }

    @Test
    public void eventPollingTest() throws TException {
        int expectedEventCount = 1000;

        randomStreamOf(expectedEventCount, PayoutEvent.class).map(
                new Function<PayoutEvent, PayoutEvent>() {
                    AtomicLong atomicLong = new AtomicLong();

                    @Override
                    public PayoutEvent apply(PayoutEvent payoutEvent) {
                        payoutEvent.setEventId(atomicLong.incrementAndGet());
                        payoutEvent.setPayoutId(String.valueOf(payoutEvent.getEventId()));
                        payoutEvent.setEventType("payout_created");
                        payoutEvent.setUserType("internal_user");
                        payoutEvent.setAmount(100L);
                        payoutEvent.setFee(10L);
                        payoutEvent.setCurrencyCode("RUB");
                        payoutEvent.setPayoutStatus("paid");
                        payoutEvent.setPayoutType("bank_account");
                        payoutEvent.setPayoutAccountType("russian_payout_account");
                        payoutEvent.setPayoutCashFlow("[{\"source\":{\"account_type\":{\"merchant\":\"guarantee\"},\"account_id\":-5608211090340005449},\"destination\":{\"account_type\":{\"provider\":\"settlement\"},\"account_id\":-1931135403352418467},\"volume\":{\"amount\":-6200157497056484578,\"currency\":{\"symbolic_code\":\"fNuRYJM&_iM\"}},\"details\":\"<H76B@v\"},{\"source\":{\"account_type\":{\"provider\":\"settlement\"},\"account_id\":-302594006063573495},\"destination\":{\"account_type\":{\"system\":\"settlement\"},\"account_id\":1918349338440800005},\"volume\":{\"amount\":1463184550909845529,\"currency\":{\"symbolic_code\":\"6vqLZ$r\"}},\"details\":\"fZ506e\"},{\"source\":{\"account_type\":{\"external\":\"outcome\"},\"account_id\":4199067975318860850},\"destination\":{\"account_type\":{\"provider\":\"settlement\"},\"account_id\":1750107826214569590},\"volume\":{\"amount\":-8840443966485658626,\"currency\":{\"symbolic_code\":\"3N\"}},\"details\":\"!bfRT\\\\y8608!\"}]");
                        return payoutEvent;
                    }

                }
        ).forEach(eventSinkService::saveEvent);

        EventRange eventRange = new EventRange(10);
        int eventCount = 0;
        Random random = new Random();
        List<Event> events;
        do {
            events = client.getEvents(eventRange);
            if (!events.isEmpty()) {
                eventRange.setAfter(events.get(events.size() - 1).getId());
            }
            eventCount += events.size();
            eventRange.setLimit(random.nextInt(100));
        } while (eventRange.getAfter() != expectedEventCount);
        assertEquals(expectedEventCount, eventCount);
    }

    @After
    public void after() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sht.payout_event");
    }

}

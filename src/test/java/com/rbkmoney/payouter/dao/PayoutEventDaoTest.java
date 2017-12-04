package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.github.benas.randombeans.api.EnhancedRandom.randomStreamOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Transactional
public class PayoutEventDaoTest extends AbstractIntegrationTest {

    @Autowired
    PayoutEventDao payoutEventDao;

    @Test
    public void insertAndGetTest() {
        PayoutEvent payoutEvent = random(PayoutEvent.class);

        long eventId = payoutEventDao.saveEvent(payoutEvent);

        assertEquals(payoutEvent.getEventId(), (Long) eventId);
        assertEquals(payoutEvent, payoutEventDao.getEvent(eventId));
    }

    @Test
    public void getEventsAfterTest() {
        List<PayoutEvent> expectedPayoutEvents = randomStreamOf(1000, PayoutEvent.class)
                .map(new Function<PayoutEvent, PayoutEvent>() {
                    AtomicLong atomicLong = new AtomicLong();

                    @Override
                    public PayoutEvent apply(PayoutEvent payoutEvent) {
                        payoutEvent.setEventId(atomicLong.incrementAndGet());
                        return payoutEvent;
                    }
                }).collect(Collectors.toList());

        expectedPayoutEvents.forEach(payoutEventDao::saveEvent);

        assertEquals(1000L, (long) payoutEventDao.getLastEventId());

        checkPartOfEvents(expectedPayoutEvents, payoutEventDao.getEvents(Optional.empty(), 100), 100);
        checkPartOfEvents(expectedPayoutEvents, payoutEventDao.getEvents(Optional.of(510L), 500), 490);
        checkPartOfEvents(expectedPayoutEvents, payoutEventDao.getEvents(Optional.empty(), 0), 0);
    }

    public void checkPartOfEvents(List<PayoutEvent> expectedPayoutEvents, List<PayoutEvent> partOfActualPayoutEvents, int expectedCount) {
        assertEquals(expectedCount, partOfActualPayoutEvents.size());
        assertTrue(expectedPayoutEvents.containsAll(partOfActualPayoutEvents));
    }

}

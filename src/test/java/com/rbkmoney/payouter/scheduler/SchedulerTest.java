package com.rbkmoney.payouter.scheduler;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@Slf4j
public class SchedulerTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static AtomicInteger counter = new AtomicInteger(0);

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "shedTest", lockAtLeastFor = 1000)
    public void firstScheduler() {
        log.info("first scheduler, counter = {}", counter.getAndIncrement());
    }

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "shedTest", lockAtLeastFor = 1000)
    public void secondScheduler(){
        log.info("second scheduler, counter = {}", counter.getAndIncrement());
    }


    @Test
    public void shedLockTest() throws InterruptedException {
        Thread.sleep(3000);
        assertTrue(counter.get() <= 4);
        List<Map<String, Object>> query = jdbcTemplate.queryForList("select * from public.shedlock where name = 'shedTest'");
        assertNotNull(query);
        assertEquals(1, query.size());
    }
}

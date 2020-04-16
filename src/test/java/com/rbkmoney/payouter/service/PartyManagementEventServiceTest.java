package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.payouter.kafka.AbstractKafkaTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.rbkmoney.payouter.service.data.TestPayloadData.*;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PartyManagementEventServiceTest extends AbstractKafkaTest {

    @Autowired
    private PartyManagementEventService partyManagementEventService;

    @MockBean
    private SchedulerService schedulerService;

    private final static int TOTAL_SUCCESS_OPS = 5;

    @Test
    public void processStockEventTest() {
        partyManagementEventService.processPayloadEvent(
                createTestMachineEvent(),
                createTestPartyEventData(
                        1,
                        TOTAL_SUCCESS_OPS,
                        random(String.class),
                        CREATED,
                        1,
                        true)
        );
        verify(schedulerService, times(TOTAL_SUCCESS_OPS))
                .registerJob(
                        anyString(), // party id
                        anyString(), // shop Id
                        any(BusinessScheduleRef.class)
                );
    }

}

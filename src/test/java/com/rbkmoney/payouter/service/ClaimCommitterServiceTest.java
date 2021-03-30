package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.payouter.handler.PartyModificationCommitHandler;
import com.rbkmoney.payouter.service.impl.ClaimCommitterService;
import com.rbkmoney.payouter.service.impl.DominantServiceImpl;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {PartyModificationCommitHandler.class, ClaimCommitterService.class})
public class ClaimCommitterServiceTest {

    @Autowired
    private ClaimCommitterSrv.Iface claimCommitterService;

    @MockBean
    private DominantServiceImpl dominantService;

    @MockBean
    private SchedulerService schedulerService;

    private final String partyId = "party_id";

    private final String shopId = "shop_id";

    private final BusinessScheduleRef businessScheduleRef = new BusinessScheduleRef().setId(1);

    @Before
    public void setup() {
        when(dominantService.getBusinessSchedule(any())).thenReturn(buildPayoutScheduleObject());
    }

    @Test
    public void testAccept() throws TException {
        claimCommitterService.accept(partyId, getTestClaim(partyId, shopId, new BusinessScheduleRef().setId(1)));
        claimCommitterService.accept(partyId, getTestClaim(partyId, shopId, null));

        verify(dominantService).getBusinessSchedule(eq(businessScheduleRef));
    }

    @Test
    public void testCommit() throws TException {
        claimCommitterService.commit(partyId, getTestClaim(partyId, shopId, businessScheduleRef));
        claimCommitterService.commit(partyId, getTestClaim(partyId, shopId, null));

        verify(schedulerService).registerJob(eq(partyId), eq(shopId), eq(businessScheduleRef));
        verify(schedulerService).deregisterJob(eq(partyId), eq(shopId));
    }

    private static Claim getTestClaim(String partyId, String shopId, BusinessScheduleRef businessScheduleRef) {
        Claim claim = new Claim();
        claim.setId(1L);
        claim.setPartyId(partyId);
        ModificationUnit modificationUnit = new ModificationUnit();
        modificationUnit.setModificationId(1L);

        ShopModificationUnit shopModificationUnit = new ShopModificationUnit();
        shopModificationUnit.setId(shopId);

        ShopModification shopModification = new ShopModification();
        ScheduleModification scheduleModification = new ScheduleModification();
        scheduleModification.setSchedule(businessScheduleRef);
        shopModification.setPayoutScheduleModification(scheduleModification);
        shopModificationUnit.setModification(shopModification);

        PartyModification partyModification = new PartyModification();
        partyModification.setShopModification(shopModificationUnit);
        Modification modification = new Modification();
        modification.setPartyModification(partyModification);

        modificationUnit.setModification(modification);
        List<ModificationUnit> modificationUnitList = new ArrayList<>();
        modificationUnitList.add(modificationUnit);
        claim.setChangeset(modificationUnitList);
        return claim;
    }

    private BusinessSchedule buildPayoutScheduleObject() {
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

        return payoutSchedule;
    }

}

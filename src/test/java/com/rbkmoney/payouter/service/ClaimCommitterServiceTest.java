package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import org.apache.thrift.TException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ClaimCommitterServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ClaimCommitterSrv.Iface claimCommitterService;

    @MockBean
    private ShopMetaDao shopMetaDao;

    @MockBean
    private SchedulerService schedulerService;

    @Test
    public void serviceTest() throws TException {
        when(shopMetaDao.get(any(String.class), any(String.class))).thenReturn(getTestShopMeta());
        boolean isError = false;
        try {
            claimCommitterService.accept("1", getTestScheduleModificationClaim(new BusinessScheduleRef().setId(1)));
        } catch (PartyNotFound | InvalidChangeset ex) {
            isError = true;
        }
        assertFalse("Error occurred during accepting", isError);

        try {
            claimCommitterService.accept("1", getTestScheduleModificationClaim(new BusinessScheduleRef().setId(2)));
        } catch (PartyNotFound | InvalidChangeset ex) {
            isError = true;
        }
        assertTrue("Exception didn't threw during accepting", isError);
    }

    private static ShopMeta getTestShopMeta() {
        ShopMeta shopMeta = new ShopMeta();
        shopMeta.setShopId("ShopId-1");
        shopMeta.setWtime(LocalDateTime.parse("2021-09-04T19:01:02.407796"));
        shopMeta.setPartyId("1");
        shopMeta.setCalendarId(1);
        shopMeta.setLastPayoutCreatedAt(LocalDateTime.parse("2021-09-04T19:01:02.407796"));
        shopMeta.setSchedulerId(1);
        return shopMeta;
    }

    private static Claim getTestScheduleModificationClaim(BusinessScheduleRef businessScheduleRef) {
        Claim claim = new Claim();
        claim.setId(1L);
        List<ModificationUnit> modificationUnitList = new ArrayList<>();
        ModificationUnit modificationUnit = new ModificationUnit();
        modificationUnit.setModificationId(1L);
        Modification modification = new Modification();

        PartyModification partyModification = new PartyModification();
        ShopModificationUnit shopModificationUnit = new ShopModificationUnit();
        shopModificationUnit.setId("ShopId-1");

        ShopModification shopModification = new ShopModification();
        ScheduleModification scheduleModification = new ScheduleModification();
        scheduleModification.setSchedule(businessScheduleRef);
        shopModification.setPayoutScheduleModification(scheduleModification);
        shopModificationUnit.setModification(shopModification);

        partyModification.setShopModification(shopModificationUnit);
        modification.setPartyModification(partyModification);

        modificationUnit.setModification(modification);
        modificationUnitList.add(modificationUnit);
        claim.setChangeset(modificationUnitList);
        return claim;
    }

}

package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.DominantService;
import com.rbkmoney.payouter.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyModificationCommitHandler implements CommitHandler<PartyModification> {

    private final ShopMetaDao shopMetaDao;

    private final SchedulerService schedulerService;

    private final DominantService dominantService;

    @Override
    public void accept(String partyId, PartyModification partyModification) throws PartyNotFound, InvalidChangeset, TException {

        if (partyModification.isSetShopModification()) {
            ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
            String shopId = shopModificationUnit.getId();
            ShopModification shopModification = shopModificationUnit.getModification();
            if (shopModification.isSetPayoutScheduleModification()) {
                ScheduleModification payoutScheduleModification = shopModification.getPayoutScheduleModification();
                BusinessScheduleRef schedule = payoutScheduleModification.getSchedule();
                checkSchedule(schedule);

                ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
                if (shopMeta == null) {
                    shopMetaDao.save(partyId, shopId, schedule.getId());
                } else if (shopMeta.getSchedulerId() == null || !shopMeta.getSchedulerId().equals(schedule.getId())) {
                    throw new InvalidChangeset();
                }
            } else {
                log.info("Accepting for '{}' party modification not implemented yet!", shopModification.getSetField().getFieldName());
            }
        } else {
            log.info("Accepting for '{}' modification not implemented yet!", partyModification.getSetField().getFieldName());
        }
    }

    @Override
    public void commit(String partyId, PartyModification partyModification) throws TException {
        if (partyModification.isSetShopModification()) {
            ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
            String shopId = shopModificationUnit.getId();
            ShopModification shopModification = shopModificationUnit.getModification();

            if (shopModification.isSetPayoutScheduleModification()) {
                BusinessScheduleRef schedule = shopModification.getPayoutScheduleModification().getSchedule();
                schedulerService.registerJob(partyId, shopId, schedule);
            } else {
                log.info("Accepting for '{}' patry modification not implemented yet!", shopModification.getSetField().getFieldName());
            }
        } else {
            log.info("Accepting for '{}' modification not implemented yet!", partyModification.getSetField().getFieldName());
        }
    }

    private void checkSchedule(BusinessScheduleRef schedule) throws InvalidChangeset {
        if (schedule == null) {
            throw new InvalidChangeset();
        }
        try {
            dominantService.getBusinessSchedule(schedule);
        } catch (NotFoundException ex) {
            throw new InvalidChangeset();
        }
    }

}

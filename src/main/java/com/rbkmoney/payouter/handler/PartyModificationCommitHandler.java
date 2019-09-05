package com.rbkmoney.payouter.handler;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
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

    @Override
    public void accept(String partyId, PartyModification partyModification) throws PartyNotFound, InvalidChangeset, TException {

        if (partyModification.isSetShopModification()) {
            ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
            String shopId = shopModificationUnit.getId();
            ShopModification shopModification = shopModificationUnit.getModification();
            if (shopModification.isSetPayoutScheduleModification()) {
                ScheduleModification payoutScheduleModification = shopModification.getPayoutScheduleModification();
                ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
                if (shopMeta == null) {
                    throw new PartyNotFound();
                }

                BusinessScheduleRef schedule = payoutScheduleModification.getSchedule();
                if (schedule == null
                        || (shopMeta.getSchedulerId() != null && !shopMeta.getSchedulerId().equals(schedule.getId()))
                        || (shopMeta.getSchedulerId() == null && schedule != null)) {
                    throw new InvalidChangeset();
                }

            } else {
                log.info("Accepting for '{}' patry modification not implemented yet!", shopModification.getSetField().getFieldName());
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

}

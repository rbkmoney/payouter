package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.service.SchedulerService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PartyClaimCreatedHandler implements Handler<PartyChange, Event> {

    private final SchedulerService schedulerService;

    private final Filter filter;

    public PartyClaimCreatedHandler(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "claim_status_changed.status.accepted",
                new IsNullCondition().not()));
    }

    @Override
    public void handle(PartyChange change, Event event) {
        ClaimStatusChanged claimStatusChanged = change.getClaimStatusChanged();
        String partyId = event.getSource().getPartyId();
        List<ClaimEffect> claimEffects = claimStatusChanged
                .getStatus()
                .getAccepted()
                .getEffects();

        for (ClaimEffect claimEffect : claimEffects) {
            if (claimEffect.isSetShopEffect()) {
                ShopEffectUnit shopEffectUnit = claimEffect.getShopEffect();
                String shopId = shopEffectUnit.getShopId();
                ShopEffect shopEffect = shopEffectUnit.getEffect();
                if (shopEffect.isSetPayoutScheduleChanged()) {
                    ScheduleChanged scheduleChanged = shopEffect.getPayoutScheduleChanged();
                    if (scheduleChanged.isSetSchedule()) {
                        schedulerService.registerJob(partyId, shopId, scheduleChanged.getSchedule());
                    } else {
                        schedulerService.deregisterJob(partyId, shopId);
                    }
                } else if (shopEffect.isSetCreated()) {
                    Shop shop = shopEffect.getCreated();
                    if (shop.isSetPayoutSchedule()) {
                        schedulerService.registerJob(partyId, shopId, shop.getPayoutSchedule());
                    }
                }
            }
        }
    }

    @Override
    public Filter<PartyChange> getFilter() {
        return filter;
    }
}

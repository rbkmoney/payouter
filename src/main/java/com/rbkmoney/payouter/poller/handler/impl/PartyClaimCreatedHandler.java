package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.service.SchedulerService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PartyClaimCreatedHandler implements Handler<PartyChange, Event> {

    private final SchedulerService schedulerService;

    public PartyClaimCreatedHandler(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
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
            if (claimEffect.isSetContractEffect()) {
                ContractEffectUnit contractEffectUnit = claimEffect.getContractEffect();
                String contractId = contractEffectUnit.getContractId();
                ContractEffect contractEffect = contractEffectUnit.getEffect();
                if (contractEffect.isSetPayoutToolEffect()) {
                    PayoutToolEffectUnit payoutToolEffectUnit = contractEffect.getPayoutToolEffect();
                    String payoutToolId = payoutToolEffectUnit.getPayoutToolId();
                    PayoutToolEffect payoutToolEffect = payoutToolEffectUnit.getEffect();
                    if (payoutToolEffect.isSetScheduleChanged()) {
                        ScheduleChanged scheduleChanged = payoutToolEffect.getScheduleChanged();
                        if (scheduleChanged.isSetSchedule()) {
                            schedulerService.registerJob(partyId, contractId, payoutToolId, scheduleChanged.getSchedule());
                        } else {
                            schedulerService.deregisterJob(partyId, contractId, payoutToolId);
                        }
                        return;
                    }
                }
            }
        }
    }

    @Override
    public Filter<PartyChange> getFilter() {
        return partyChange -> partyChange.isSetClaimStatusChanged()
                && partyChange.getClaimStatusChanged().getStatus().isSetAccepted();
    }
}

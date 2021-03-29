package com.rbkmoney.payouter.service.data;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestPayloadData {

    public static final String CREATED = "created";
    public static final String CHANGED = "changed";

    public static MachineEvent createTestMachineEvent() {
        MachineEvent machineEvent = new MachineEvent();
        machineEvent.setEventId(random(Long.class));
        machineEvent.setSourceId(random(String.class));
        return machineEvent;
    }

    public static PartyEventData createTestPartyEventData(int changesCount,
                                                          int totalSuccessInChange,
                                                          String shopId,
                                                          String shopEffect,
                                                          int scheduleId,
                                                          boolean isSetSchedule) {
        PartyEventData eventData = new PartyEventData();
        eventData.setChanges(
                createTestPartyChangesList(
                        changesCount,
                        totalSuccessInChange,
                        shopId,
                        shopEffect,
                        scheduleId,
                        isSetSchedule
                )
        );
        return eventData;
    }

    private static List<PartyChange> createTestPartyChangesList(int changesCount,
                                                                int totalSuccessInChange,
                                                                String shopId,
                                                                String shopEffect,
                                                                int scheduleId,
                                                                boolean isSetSchedule) {
        List<PartyChange> changes = new ArrayList<>();
        for (int i = 0; i < changesCount; i++) {
            changes.add(createTestPartyChange(totalSuccessInChange, shopId, shopEffect, scheduleId, isSetSchedule));
        }
        return changes;
    }

    private static PartyChange createTestPartyChange(int totalSuccessInChange,
                                                     String shopId,
                                                     String shopEffect,
                                                     int scheduleId,
                                                     boolean isSetSchedule) {
        ClaimStatusChanged claimStatusChanged = new ClaimStatusChanged();
        claimStatusChanged.setId(random(Long.class));
        ClaimStatus status = new ClaimStatus();

        ClaimAccepted accepted = new ClaimAccepted();
        List<ClaimEffect> claimEffectList = new ArrayList<>();
        for (int i = 0; i < totalSuccessInChange; i++) {
            claimEffectList.add(createTestClaimEffect(shopEffect, shopId, scheduleId, isSetSchedule));
            //claimEffectList.add(createTestClaimEffect(false, shopId, scheduleId, isSetSchedule));
        }
        accepted.setEffects(claimEffectList);
        status.setAccepted(accepted);
        claimStatusChanged.setStatus(status);
        PartyChange partyChange = new PartyChange();
        partyChange.setClaimStatusChanged(claimStatusChanged);
        return partyChange;
    }

    private static ClaimEffect createTestClaimEffect(String shopEffect,
                                                     String shopId,
                                                     int scheduleId,
                                                     boolean isSetSchedule) {
        ClaimEffect claimEffect = new ClaimEffect();
        ShopEffectUnit shopEffectUnit = new ShopEffectUnit();
        shopEffectUnit.setShopId(shopId);
        ShopEffect effect = new ShopEffect();

        switch (shopEffect) {
            case CREATED:
                Shop shop = new Shop();
                if (isSetSchedule) {
                    shop.setPayoutSchedule(new BusinessScheduleRef().setId(scheduleId));
                }
                effect.setCreated(shop);
                shopEffectUnit.setEffect(effect);
                claimEffect.setShopEffect(shopEffectUnit);
                break;
            case CHANGED:
                ScheduleChanged payoutScheduleChanged = new ScheduleChanged();
                if (isSetSchedule) {
                    payoutScheduleChanged.setSchedule(new BusinessScheduleRef(scheduleId));
                }
                effect.setPayoutScheduleChanged(payoutScheduleChanged);
                shopEffectUnit.setEffect(effect);
                claimEffect.setShopEffect(shopEffectUnit);
                break;
            default:
                claimEffect.setContractEffect(new ContractEffectUnit());
                break;
        }

        return claimEffect;
    }

}

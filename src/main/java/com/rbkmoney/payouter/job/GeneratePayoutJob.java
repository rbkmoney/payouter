package com.rbkmoney.payouter.job;

import com.rbkmoney.damsel.payout_processing.InternalUser;
import com.rbkmoney.damsel.payout_processing.UserType;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.trigger.FreezeTimeCronTrigger;
import com.rbkmoney.payouter.util.WoodyUtils;
import com.rbkmoney.woody.api.flow.WFlow;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.geck.common.util.TypeUtil.toLocalDateTime;

@Component
public class GeneratePayoutJob implements Job {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String PARTY_ID = "party_id";

    public static final String SHOP_ID = "shop_id";

    private final WFlow wFlow = new WFlow();

    @Autowired
    private PayoutService payoutService;

    @Value("${scheduler.user-id}")
    private String userId;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        FreezeTimeCronTrigger trigger = (FreezeTimeCronTrigger) jobExecutionContext.getTrigger();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        String partyId = jobDataMap.getString(PARTY_ID);
        String shopId = jobDataMap.getString(SHOP_ID);

        log.info("Trying to create payout for shop, partyId='{}', shopId='{}', trigger='{}', jobExecutionContext='{}'",
                partyId, shopId, trigger, jobExecutionContext);
        try {
            try {
                LocalDateTime toTime = toLocalDateTime(trigger.getCurrentCronTime().toInstant());
                List<Long> payoutIds = wFlow.createServiceFork(
                        () -> {
                            WoodyUtils.setUserInfo(userId, UserType.internal_user(new InternalUser()));
                            return payoutService.createPayouts(
                                    partyId,
                                    shopId,
                                    toTime.minusDays(1),
                                    toTime,
                                    PayoutType.bank_account
                            );
                        }
                ).call();

                log.info("Payouts for shop have been successfully created, payoutIds='{}' partyId='{}', shopId='{}', trigger='{}', jobExecutionContext='{}'",
                        payoutIds, partyId, shopId, trigger, jobExecutionContext);
            } catch (NotFoundException | InvalidStateException ex) {
                log.warn("Failed to create payouts for shop, partyId='{}', shopId='{}', trigger='{}', jobExecutionContext='{}'",
                        partyId, shopId, trigger, jobExecutionContext, ex);
            }
        } catch (StorageException | WRuntimeException ex) {
            throw new JobExecutionException(String.format("Job execution failed (partyId='%s', shopId='%s', trigger='%s', jobExecutionContext='%s'), retry",
                    partyId, shopId, trigger, jobExecutionContext), ex, true);
        } catch (Exception ex) {
            JobExecutionException jobExecutionException = new JobExecutionException(
                    String.format("Job execution failed (partyId='%s', shopId='%s', trigger='%s', jobExecutionContext='%s'), stop triggers",
                            partyId, shopId, trigger, jobExecutionContext), ex);
            jobExecutionException.setUnscheduleAllTriggers(true);
            throw jobExecutionException;
        }
    }
}

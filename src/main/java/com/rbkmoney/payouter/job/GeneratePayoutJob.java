package com.rbkmoney.payouter.job;

import com.rbkmoney.damsel.base.TimeSpan;
import com.rbkmoney.damsel.domain.TermSet;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.util.SchedulerUtil;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import org.quartz.*;
import org.quartz.impl.calendar.HolidayCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class GeneratePayoutJob implements Job {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutService payoutService;

    private final PartyManagementService partyManagementService;

    @Autowired
    public GeneratePayoutJob(PayoutService payoutService, PartyManagementService partyManagementService) {
        this.payoutService = payoutService;
        this.partyManagementService = partyManagementService;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Start generatePayout job, jobExecutionContext='{}'", jobExecutionContext);
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Calendar calendar = jobExecutionContext.getCalendar();

        List<ShopMeta> shops = (List<ShopMeta>) jobDetail.getJobDataMap().get("shops");
        log.info("Start shops processing, shops='{}'", shops);
        try {
            for (ShopMeta shopMeta : shops) {
                TermSet termSet = partyManagementService.computeShopTerms(shopMeta.getPartyId(), shopMeta.getShopId());

                TimeSpan timeSpan = termSet.getPayouts().getPolicy().getAssetsFreezeFor();
                Instant toTime = SchedulerUtil.computeToTimeBound(
                        jobExecutionContext.getFireTime().toInstant(),
                        timeSpan,
                        (HolidayCalendar) calendar);
                try {
                    payoutService.createPayout(
                            shopMeta.getPartyId(),
                            shopMeta.getShopId(),
                            LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                            LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                            PayoutType.bank_account
                    );
                } catch (NotFoundException | InvalidStateException ex) {
                    log.warn("Failed to create payout for shop, skipped. shopMeta='{}', toTime='{}'", shopMeta, toTime, ex);
                }
            }
        } catch (StorageException | WRuntimeException ex) {
            throw new JobExecutionException(String.format("Job execution failed, retry. jobExecutionContext='%s'", jobExecutionContext), ex, true);
        } catch (Exception ex) {
            JobExecutionException jobExecutionException = new JobExecutionException(
                    String.format("Job execution failed, stop scheduler. jobExecutionContext='%s'", jobExecutionContext),
                    ex);
            jobExecutionException.setUnscheduleFiringTrigger(true);
            throw jobExecutionException;
        }
    }
}

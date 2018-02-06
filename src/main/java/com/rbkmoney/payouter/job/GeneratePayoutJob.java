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
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class GeneratePayoutJob implements Job {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private PartyManagementService partyManagementService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Start generatePayout job, jobExecutionContext='{}'", jobExecutionContext);
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Calendar calendar = jobExecutionContext.getCalendar();

        List<ShopMeta> shops = (List<ShopMeta>) jobDetail.getJobDataMap().get("shops");
        log.info("Start shops processing, shops='{}'", shops);
        try {
            for (ShopMeta shopMeta : shops) {
                try {
                    TermSet termSet = partyManagementService.computeShopTerms(shopMeta.getPartyId(), shopMeta.getShopId());

                    TimeSpan timeSpan = termSet.getPayouts().getPolicy().getAssetsFreezeFor();
                    Instant toTime = SchedulerUtil.computeToTimeBound(
                            jobExecutionContext.getFireTime().toInstant(),
                            timeSpan,
                            (HolidayCalendar) calendar);

                    payoutService.createPayout(
                            shopMeta.getPartyId(),
                            shopMeta.getShopId(),
                            LocalDateTime.ofInstant(toTime, ZoneOffset.UTC).minusDays(1),
                            LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                            PayoutType.bank_account
                    );
                } catch (NotFoundException | InvalidStateException ex) {
                    log.warn("Failed to create payout for shop, skipped. shopMeta='{}', fireTime='{}'", shopMeta, jobExecutionContext.getFireTime(), ex);
                }
            }
        } catch (StorageException | WRuntimeException ex) {
            throw new JobExecutionException(String.format("Job execution failed, shops='%s', retry='%s'. jobExecutionContext='%s'", shops, jobExecutionContext), ex, true);
        } catch (Exception ex) {
            JobExecutionException jobExecutionException = new JobExecutionException(
                    String.format("Job execution failed, stop. shops='%s', jobExecutionContext='%s'", shops, jobExecutionContext),
                    ex);
            jobExecutionException.setUnscheduleFiringTrigger(true);
            throw jobExecutionException;
        }
    }
}

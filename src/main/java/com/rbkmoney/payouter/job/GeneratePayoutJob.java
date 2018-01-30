package com.rbkmoney.payouter.job;

import com.rbkmoney.damsel.base.TimeSpan;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.domain.TermSet;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.JobMeta;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.util.SchedulerUtil;
import org.quartz.*;
import org.quartz.impl.calendar.HolidayCalendar;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class GeneratePayoutJob implements Job {

    private final PayoutService payoutService;

    private final PartyManagementService partyManagementService;

    @Autowired
    public GeneratePayoutJob(PayoutService payoutService, PartyManagementService partyManagementService) {
        this.payoutService = payoutService;
        this.partyManagementService = partyManagementService;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Calendar calendar = jobExecutionContext.getCalendar();

        List<JobMeta> jobs = (List<JobMeta>) jobDetail.getJobDataMap().get("jobs");
        for (JobMeta jobMeta : jobs) {
            Shop shop = partyManagementService.getShopByContractAndPayoutToolIds(
                    jobMeta.getPartyId(),
                    jobMeta.getContractId(),
                    jobMeta.getPayoutToolId()
            );
            TermSet termSet = partyManagementService.computeContractTerms(
                    jobMeta.getPartyId(),
                    jobMeta.getContractId()
            );

            TimeSpan timeSpan = termSet.getPayouts().getPolicy().getAssetsFreezeFor();
            Instant toTime = SchedulerUtil.computeToTimeBound(
                    jobExecutionContext.getFireTime().toInstant(),
                    timeSpan,
                    (HolidayCalendar) calendar
            );
            payoutService.createPayout(
                    jobMeta.getPartyId(),
                    shop.getId(),
                    LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                    LocalDateTime.ofInstant(toTime, ZoneOffset.UTC),
                    PayoutType.bank_account
            );

        }
    }
}

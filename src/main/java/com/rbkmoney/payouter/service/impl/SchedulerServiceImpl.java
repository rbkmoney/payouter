package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.Calendar;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.ScheduleProcessingException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.job.GeneratePayoutJob;
import com.rbkmoney.payouter.service.DominantService;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.service.SchedulerService;
import com.rbkmoney.payouter.util.SchedulerUtil;
import org.quartz.*;
import org.quartz.impl.calendar.HolidayCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@DependsOn("dbInitializer")
public class SchedulerServiceImpl implements SchedulerService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Scheduler scheduler;

    private final ShopMetaDao shopMetaDao;

    private final PartyManagementService partyManagementService;

    private final DominantService dominantService;

    @Autowired
    public SchedulerServiceImpl(Scheduler scheduler,
                                ShopMetaDao shopMetaDao,
                                PartyManagementService partyManagementService,
                                DominantService dominantService) {
        this.scheduler = scheduler;
        this.shopMetaDao = shopMetaDao;
        this.partyManagementService = partyManagementService;
        this.dominantService = dominantService;
    }

    @PostConstruct
    public void initJobs() {
        log.info("Starting jobs...");
        List<Map.Entry<Integer, Integer>> activeShops = shopMetaDao.getAllActiveShops();
        if (activeShops.isEmpty()) {
            log.info("No shops found, nothing to do");
            return;
        }

        for (Map.Entry<Integer, Integer> job : activeShops) {
            updateJobs(new CalendarRef(job.getKey()), new ScheduleRef(job.getValue()));
        }
        log.info("Jobs have been successfully started, jobsCount='{}'", activeShops.size());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerJob(String partyId, String shopId, ScheduleRef scheduleRef) {
        try {
            log.info("Trying to register job, partyId='{}', shopId='{}', scheduleRef='{}'",
                    partyId, shopId, scheduleRef);

            Shop shop = partyManagementService.getShop(partyId, shopId);
            PaymentInstitutionRef paymentInstitutionRef = partyManagementService.getPaymentInstitutionRef(partyId, shop.getContractId());
            PaymentInstitution paymentInstitution = dominantService.getPaymentInstitution(paymentInstitutionRef);
            CalendarRef calendarRef = paymentInstitution.getCalendar();

            shopMetaDao.save(partyId, shopId, calendarRef.getId(), scheduleRef.getId());

            updateJobs(calendarRef, scheduleRef);
            log.info("Job have been successfully enabled, partyId='{}', shopId='{}', scheduleRef='{}', calendarRef='{}'",
                    partyId, shopId, scheduleRef, calendarRef);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format(
                            "Failed to save job on storage, partyId='%s', shopId='%s', scheduleRef='%s'",
                            partyId, shopId, scheduleRef), ex);
        }
    }

    private void updateJobs(CalendarRef calendarRef, ScheduleRef scheduleRef) {
        log.info("Trying to update jobs, calendarRef='{}', scheduleRef='{}'", calendarRef, scheduleRef);
        try {
            List<ShopMeta> shops = shopMetaDao.getByCalendarAndSchedulerId(calendarRef.getId(), scheduleRef.getId());

            if (shops.isEmpty()) {
                cleanUpJobs(calendarRef, scheduleRef);
                return;
            }

            Schedule schedule = dominantService.getSchedule(scheduleRef);
            Calendar calendar = dominantService.getCalendar(calendarRef);

            String calendarId = "calendar-" + calendarRef.getId();
            HolidayCalendar holidayCalendar = SchedulerUtil.buildCalendar(calendar);
            scheduler.addCalendar(calendarId, holidayCalendar, true, true);
            log.info("New calendar was saved, calendarRef='{}', calendarId='{}'", calendarRef, calendarId);

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("shops", shops);

            JobDetail jobDetail = JobBuilder.newJob(GeneratePayoutJob.class)
                    .withIdentity(buildJobKey(calendarRef, scheduleRef))
                    .withDescription(schedule.getDescription())
                    .usingJobData(jobDataMap)
                    .build();

            Set<Trigger> triggers = new HashSet<>();
            List<String> cronList = SchedulerUtil.buildCron(schedule.getSchedule());
            for (int itemId = 0; itemId < cronList.size(); itemId++) {
                String cron = cronList.get(itemId);
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(buildTriggerKey(calendarRef, scheduleRef, itemId))
                        .withDescription(schedule.getDescription())
                        .forJob(jobDetail)
                        .withSchedule(
                                CronScheduleBuilder.cronSchedule(cron)
                                        .inTimeZone(TimeZone.getTimeZone(calendar.getTimezone()))
                        )
                        .modifiedByCalendar(calendarId)
                        .build();
                triggers.add(trigger);
            }
            scheduler.scheduleJob(jobDetail, triggers, true);
            log.info("Jobs have been successfully updated, calendarRef='{}', scheduleRef='{}', jobDetail='{}', triggers='{}'", calendarRef, scheduleRef, jobDetail, triggers);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("failed to update jobs from storage, calendarRef='%s', scheduleRef='%s'",
                            calendarRef, scheduleRef, ex));
        } catch (SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to update jobs, calendarRef='%s', scheduleRef='%s'",
                            calendarRef, scheduleRef), ex);
        }
    }

    private void cleanUpJobs(CalendarRef calendarRef, ScheduleRef scheduleRef) {
        try {
            log.info("Starting clean-up for jobs, calendarRef='{}', scheduleRef='{}'", calendarRef, scheduleRef);
            JobKey jobKey = buildJobKey(calendarRef, scheduleRef);
            List<TriggerKey> triggerKeys = scheduler.getTriggersOfJob(jobKey).stream()
                    .map(trigger -> trigger.getKey())
                    .collect(Collectors.toList());

            scheduler.unscheduleJobs(triggerKeys);
            scheduler.deleteJob(jobKey);
            log.info("Jobs clean-up finished, calendarRef='{}', scheduleRef='{}', jobKey='{}', triggerKeys='{}'",
                    calendarRef, scheduleRef, jobKey, triggerKeys);
        } catch (SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to clean-up jobs, calendarRef='%s', scheduleRef='%s'",
                            calendarRef, scheduleRef), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deregisterJob(String partyId, String shopId) {
        try {
            log.info("Trying to deregister job, partyId='{}', contractId='{}', payoutToolId='{}'", partyId, shopId);
            ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
            shopMetaDao.disableShop(partyId, shopId);
            if (shopMeta.getCalendarId() != null && shopMeta.getSchedulerId() != null) {
                updateJobs(new CalendarRef(shopMeta.getCalendarId()), new ScheduleRef(shopMeta.getSchedulerId()));
            }
            log.info("Job have been successfully disabled, partyId='{}', shopId='{}', scheduleId='{}', calendarId='{}'",
                    partyId, shopId, shopMeta.getSchedulerId(), shopMeta.getCalendarId());
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to disable job on storage, partyId='%s', shopId='%s'",
                            partyId, shopId), ex);
        }
    }

    private JobKey buildJobKey(CalendarRef calendarRef, ScheduleRef scheduleRef) {
        return JobKey.jobKey(String.format("job-%d:%d", calendarRef.getId(), scheduleRef.getId()));
    }

    private TriggerKey buildTriggerKey(CalendarRef calendarRef, ScheduleRef scheduleRef, int triggerId) {
        return TriggerKey.triggerKey(
                String.format("trigger-%d:%d:%d", calendarRef.getId(), scheduleRef.getId(), triggerId)
        );
    }
}
package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.base.TimeSpan;
import com.rbkmoney.damsel.domain.Calendar;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.ScheduleProcessingException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.job.GeneratePayoutJob;
import com.rbkmoney.payouter.service.DominantService;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.service.SchedulerService;
import com.rbkmoney.payouter.trigger.FreezeTimeCronScheduleBuilder;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
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
        List<ShopMeta> activeShops = shopMetaDao.getAllActiveShops();
        if (activeShops.isEmpty()) {
            log.info("No shops found, nothing to do");
            return;
        }

        for (ShopMeta shopMeta : activeShops) {
            createJob(
                    shopMeta.getPartyId(),
                    shopMeta.getShopId(),
                    new CalendarRef(shopMeta.getCalendarId()),
                    new PayoutScheduleRef(shopMeta.getSchedulerId())
            );
        }
        log.info("Jobs have been successfully started, jobsCount='{}'", activeShops.size());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerJob(String partyId, String shopId, PayoutScheduleRef scheduleRef) throws NotFoundException, ScheduleProcessingException, StorageException {
        try {
            log.info("Trying to register job, partyId='{}', shopId='{}', scheduleRef='{}'",
                    partyId, shopId, scheduleRef);

            Shop shop = partyManagementService.getShop(partyId, shopId);
            PaymentInstitutionRef paymentInstitutionRef = partyManagementService.getPaymentInstitutionRef(partyId, shop.getContractId());
            PaymentInstitution paymentInstitution = dominantService.getPaymentInstitution(paymentInstitutionRef);
            CalendarRef calendarRef = paymentInstitution.getCalendar();

            shopMetaDao.save(partyId, shopId, calendarRef.getId(), scheduleRef.getId());

            createJob(partyId, shopId, calendarRef, scheduleRef);
            log.info("Job have been successfully enabled, partyId='{}', shopId='{}', scheduleRef='{}', calendarRef='{}'",
                    partyId, shopId, scheduleRef, calendarRef);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format(
                            "Failed to save job on storage, partyId='%s', shopId='%s', scheduleRef='%s'",
                            partyId, shopId, scheduleRef), ex);
        }
    }

    private void createJob(String partyId, String shopId, CalendarRef calendarRef, PayoutScheduleRef scheduleRef) throws NotFoundException, ScheduleProcessingException, StorageException {
        log.info("Trying to create job, partyId='{}', shopId='{}', calendarRef='{}', scheduleRef='{}'", partyId, shopId, calendarRef, scheduleRef);
        try {
            PayoutSchedule schedule = dominantService.getPayoutSchedule(scheduleRef);
            Calendar calendar = dominantService.getCalendar(calendarRef);

            String calendarId = "calendar-" + calendarRef.getId();
            HolidayCalendar holidayCalendar = SchedulerUtil.buildCalendar(calendar);
            scheduler.addCalendar(calendarId, holidayCalendar, true, true);
            log.info("New calendar was saved, calendarRef='{}', calendarId='{}'", calendarRef, calendarId);

            JobDetail jobDetail = JobBuilder.newJob(GeneratePayoutJob.class)
                    .withIdentity(buildJobKey(partyId, shopId, calendarRef.getId(), scheduleRef.getId()))
                    .withDescription(schedule.getDescription())
                    .usingJobData(GeneratePayoutJob.PARTY_ID, partyId)
                    .usingJobData(GeneratePayoutJob.SHOP_ID, shopId)
                    .build();

            TimeSpan timeSpan = schedule.getPolicy().getAssetsFreezeFor();
            Set<Trigger> triggers = new HashSet<>();
            List<String> cronList = SchedulerUtil.buildCron(schedule.getSchedule());
            for (int triggerId = 0; triggerId < cronList.size(); triggerId++) {
                String cron = cronList.get(triggerId);
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(buildTriggerKey(partyId, shopId, calendarRef.getId(), scheduleRef.getId(), triggerId))
                        .withDescription(schedule.getDescription())
                        .forJob(jobDetail)
                        .withSchedule(
                                FreezeTimeCronScheduleBuilder.cronSchedule(cron)
                                        .inTimeZone(TimeZone.getTimeZone(calendar.getTimezone()))
                                        .withYears(timeSpan.getYears())
                                        .withMonths(timeSpan.getMonths())
                                        .withDays(timeSpan.getDays())
                                        .withHours(timeSpan.getHours())
                                        .withMinutes(timeSpan.getMinutes())
                                        .withSeconds(timeSpan.getSeconds())
                        )
                        .modifiedByCalendar(calendarId)
                        .build();
                triggers.add(trigger);
            }
            scheduler.scheduleJob(jobDetail, triggers, true);
            log.info("Jobs have been successfully created or updated, partyId='{}', shopId='{}', calendarRef='{}', scheduleRef='{}', jobDetail='{}', triggers='{}'", calendarRef, scheduleRef, jobDetail, triggers);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("failed to create job on storage, partyId='%s', shopId='%s', calendarRef='%s', scheduleRef='%s'",
                            partyId, shopId, calendarRef, scheduleRef, ex));
        } catch (NotFoundException | SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to create job, partyId='%s', shopId='%s', calendarRef='%s', scheduleRef='%s'",
                            partyId, shopId, calendarRef, scheduleRef), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deregisterJob(String partyId, String shopId) throws NotFoundException, ScheduleProcessingException, StorageException {
        try {
            log.info("Trying to deregister job, partyId='{}', contractId='{}', payoutToolId='{}'", partyId, shopId);
            ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
            shopMetaDao.disableShop(partyId, shopId);
            if (shopMeta.getCalendarId() != null && shopMeta.getSchedulerId() != null) {
                JobKey jobKey = buildJobKey(partyId, shopId, shopMeta.getCalendarId(), shopMeta.getSchedulerId());
                List<TriggerKey> triggerKeys = scheduler.getTriggersOfJob(jobKey).stream()
                        .map(trigger -> trigger.getKey())
                        .collect(Collectors.toList());

                scheduler.unscheduleJobs(triggerKeys);
                scheduler.deleteJob(jobKey);
            }
            log.info("Job have been successfully disabled, partyId='{}', shopId='{}', scheduleId='{}', calendarId='{}'",
                    partyId, shopId, shopMeta.getSchedulerId(), shopMeta.getCalendarId());
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to disable job on storage, partyId='%s', shopId='%s'",
                            partyId, shopId), ex);
        } catch (SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to disable job, partyId='%s', shopId='%s'",
                            partyId, shopId), ex);
        }
    }

    private JobKey buildJobKey(String partyId, String shopId, int calendarId, int scheduleId) {
        return JobKey.jobKey(
                String.format("job-%s-%s", partyId, shopId),
                buildGroupKey(calendarId, scheduleId)
        );
    }

    private TriggerKey buildTriggerKey(String partyId, String shopId, int calendarId, int scheduleId, int triggerId) {
        return TriggerKey.triggerKey(
                String.format("trigger-%s-%s-%d", partyId, shopId, triggerId),
                buildGroupKey(calendarId, scheduleId)
        );
    }

    private String buildGroupKey(int calendarId, int scheduleId) {
        return String.format("group-%d-%d", calendarId, scheduleId);
    }
}

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.calendar.HolidayCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {

    private final Scheduler scheduler;

    private final ShopMetaDao shopMetaDao;

    private final PartyManagementService partyManagementService;

    private final DominantService dominantService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void syncJobs() {
        try {
            log.info("Starting synchronization of jobs...");
            List<ShopMeta> activeShops = shopMetaDao.getAllActiveShops();
            if (activeShops.isEmpty()) {
                log.info("No active shops found, nothing to do");
                return;
            }

            for (ShopMeta shopMeta : activeShops) {
                JobKey jobKey = buildJobKey(shopMeta.getPartyId(), shopMeta.getShopId(),
                        shopMeta.getCalendarId(), shopMeta.getSchedulerId());
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (triggers.isEmpty() || !triggers.stream().allMatch(this::isTriggerOnNormalState)) {
                    if (scheduler.checkExists(jobKey)) {
                        log.warn("Inactive job found, please check it manually. " +
                                "Job will be restored, shopMeta='{}'", shopMeta);
                    }
                    createJob(
                            shopMeta.getPartyId(),
                            shopMeta.getShopId(),
                            new CalendarRef(shopMeta.getCalendarId()),
                            new BusinessScheduleRef(shopMeta.getSchedulerId())
                    );
                }
            }
        } catch (DaoException | SchedulerException ex) {
            throw new ScheduleProcessingException("Failed to sync jobs", ex);
        } finally {
            log.info("End synchronization of jobs");
        }
    }

    private boolean isTriggerOnNormalState(Trigger trigger) {
        try {
            return scheduler.getTriggerState(trigger.getKey()) == Trigger.TriggerState.NORMAL;
        } catch (SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to get trigger state, triggerKey='%s'", trigger.getKey()), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerJob(String partyId, String shopId, BusinessScheduleRef scheduleRef)
            throws NotFoundException, ScheduleProcessingException, StorageException {
        try {
            log.info("Trying to register job, partyId='{}', shopId='{}', scheduleRef='{}'",
                    partyId, shopId, scheduleRef);

            Shop shop = partyManagementService.getShop(partyId, shopId);
            var paymentInstitutionRef = partyManagementService.getPaymentInstitutionRef(partyId, shop.getContractId());
            PaymentInstitution paymentInstitution = dominantService.getPaymentInstitution(paymentInstitutionRef);
            if (!paymentInstitution.isSetCalendar()) {
                throw new NotFoundException(String.format("Calendar not found, " +
                        "partyId='%s', shopId='%s', contractId='%s'", partyId, shop.getId(), shop.getContractId()));
            }
            deregisterJob(partyId, shopId);
            CalendarRef calendarRef = paymentInstitution.getCalendar();
            shopMetaDao.save(partyId, shopId, calendarRef.getId(), scheduleRef.getId());

            createJob(partyId, shopId, calendarRef, scheduleRef);
            log.info("Job have been successfully enabled, partyId='{}', shopId='{}', schedRef='{}', calendarRef='{}'",
                    partyId, shopId, scheduleRef, calendarRef);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format(
                            "Failed to save job on storage, partyId='%s', shopId='%s', scheduleRef='%s'",
                            partyId, shopId, scheduleRef), ex);
        }
    }

    private void createJob(String partyId, String shopId, CalendarRef calendarRef, BusinessScheduleRef scheduleRef)
            throws NotFoundException, ScheduleProcessingException, StorageException {
        log.info("Trying to create job, partyId='{}', shopId='{}', calendarRef='{}', scheduleRef='{}'",
                partyId, shopId, calendarRef, scheduleRef);
        try {
            BusinessSchedule schedule = dominantService.getBusinessSchedule(scheduleRef);
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

            Set<Trigger> triggers = new HashSet<>();
            List<String> cronList = SchedulerUtil.buildCron(schedule.getSchedule(), calendar.getFirstDayOfWeek());
            for (int triggerId = 0; triggerId < cronList.size(); triggerId++) {
                String cron = cronList.get(triggerId);

                FreezeTimeCronScheduleBuilder cronScheduleBuilder = FreezeTimeCronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(TimeZone.getTimeZone(calendar.getTimezone()));

                if (schedule.isSetDelay() || schedule.isSetPolicy()) {
                    TimeSpan timeSpan = Optional.ofNullable(schedule.getPolicy())
                            .map(PayoutCompilationPolicy::getAssetsFreezeFor)
                            .orElse(schedule.getDelay());

                    cronScheduleBuilder.withYears(timeSpan.getYears())
                            .withMonths(timeSpan.getMonths())
                            .withDays(timeSpan.getDays())
                            .withHours(timeSpan.getHours())
                            .withMinutes(timeSpan.getMinutes())
                            .withSeconds(timeSpan.getSeconds());
                }

                var triggerKey = buildTriggerKey(partyId, shopId, calendarRef.getId(), scheduleRef.getId(), triggerId);
                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey)
                        .withDescription(schedule.getDescription())
                        .forJob(jobDetail)
                        .withSchedule(cronScheduleBuilder)
                        .modifiedByCalendar(calendarId)
                        .build();
                triggers.add(trigger);
            }
            scheduler.scheduleJob(jobDetail, triggers, true);
            log.info("Jobs have been successfully created or updated, " +
                    "partyId='{}', shopId='{}', calendarRef='{}', scheduleRef='{}', jobDetail='{}', triggers='{}'",
                    partyId, shopId, calendarRef, scheduleRef, jobDetail, triggers);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("failed to create job on storage, " +
                                    "partyId='%s', shopId='%s', calendarRef='%s', scheduleRef='%s'",
                            partyId, shopId, calendarRef, scheduleRef), ex);
        } catch (NotFoundException | SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to create job, partyId='%s', shopId='%s', calendarRef='%s', scheduleRef='%s'",
                            partyId, shopId, calendarRef, scheduleRef), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deregisterJob(String partyId, String shopId)
            throws NotFoundException, ScheduleProcessingException, StorageException {
        try {
            ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
            if (shopMeta != null) {
                log.info("Trying to deregister job, partyId='{}', shopId='{}'", partyId, shopId);
                shopMetaDao.disableShop(partyId, shopId);
                if (shopMeta.getCalendarId() != null && shopMeta.getSchedulerId() != null) {
                    JobKey jobKey = buildJobKey(partyId, shopId, shopMeta.getCalendarId(), shopMeta.getSchedulerId());
                    List<TriggerKey> triggerKeys = scheduler.getTriggersOfJob(jobKey).stream()
                            .map(Trigger::getKey)
                            .collect(Collectors.toList());

                    scheduler.unscheduleJobs(triggerKeys);
                    scheduler.deleteJob(jobKey);
                    log.info("Job have been successfully disabled, partyId='{}', shopId='{}', " +
                            "scheduleId='{}', calendarId='{}'", partyId, shopId,
                            shopMeta.getSchedulerId(), shopMeta.getCalendarId());
                }
            }
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to disable job on storage, partyId='%s', shopId='%s'", partyId, shopId), ex);
        } catch (SchedulerException ex) {
            throw new ScheduleProcessingException(
                    String.format("Failed to disable job, partyId='%s', shopId='%s'", partyId, shopId), ex);
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

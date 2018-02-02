package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.*;
import com.rbkmoney.damsel.domain_config.Reference;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.DominantService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DominantServiceImpl implements DominantService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RepositoryClientSrv.Iface dominantClient;

    @Override
    public Schedule getSchedule(ScheduleRef scheduleRef) throws NotFoundException {
        return getSchedule(scheduleRef, Reference.head(new Head()));
    }

    @Override
    public Schedule getSchedule(ScheduleRef scheduleRef, long domainRevision) throws NotFoundException {
        return getSchedule(scheduleRef, Reference.version(domainRevision));
    }

    @Override
    public Schedule getSchedule(ScheduleRef scheduleRef, Reference revisionReference) throws NotFoundException {
        log.info("Trying to get schedule, scheduleRef='{}', revisionReference='{}'", scheduleRef, revisionReference);
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setSchedule(scheduleRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(revisionReference, reference);
            Schedule schedule = versionedObject.getObject().getSchedule().getData();
            log.info("Schedule has been found, scheduleRef='{}', revisionReference='{}', schedule='{}'", scheduleRef, revisionReference, schedule);
            return schedule;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, scheduleRef='%s', revisionReference='%s'", scheduleRef, revisionReference), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get schedule, scheduleRef='%s', revisionReference='%s'", scheduleRef, revisionReference), ex);
        }
    }

    @Override
    public PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef) throws NotFoundException {
        return getPaymentInstitution(paymentInstitutionRef, Reference.head(new Head()));
    }

    @Override
    public PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef, long domainRevision) throws NotFoundException {
        return getPaymentInstitution(paymentInstitutionRef, Reference.version(domainRevision));
    }

    @Override
    public PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef, Reference revisionReference) throws NotFoundException {
        log.info("Trying to get payment institution, paymentInstitutionRef='{}', revisionReference='{}'", paymentInstitutionRef, revisionReference);
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setPaymentInstitution(paymentInstitutionRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(revisionReference, reference);
            PaymentInstitution paymentInstitution = versionedObject.getObject().getPaymentInstitution().getData();
            log.info("Payment institution has been found, PaymentInstitutionRef='{}', revisionReference='{}', paymentInstitution='{}'", paymentInstitutionRef, revisionReference, paymentInstitution);
            return paymentInstitution;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, paymentInstitutionRef='%s', revisionReference='%s'", paymentInstitutionRef, revisionReference), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get payment institution, paymentInstitutionRef='%s', revisionReference='%s'", paymentInstitutionRef, revisionReference), ex);
        }
    }

    @Override
    public Calendar getCalendar(CalendarRef calendarRef) throws NotFoundException {
        return getCalendar(calendarRef, Reference.head(new Head()));
    }

    @Override
    public Calendar getCalendar(CalendarRef calendarRef, long domainRevision) throws NotFoundException {
        return getCalendar(calendarRef, Reference.version(domainRevision));
    }

    @Override
    public Calendar getCalendar(CalendarRef calendarRef, Reference revisionReference) throws NotFoundException {
        log.info("Trying to get calendar, calendarRef='{}', revisionReference='{}'", calendarRef, revisionReference);
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setCalendar(calendarRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(revisionReference, reference);
            Calendar calendar = versionedObject.getObject().getCalendar().getData();
            log.info("Calendar has been found, calendarRef='{}', revisionReference='{}', calendar='{}'", calendarRef, revisionReference, calendar);
            return calendar;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, calendarRef='%s', revisionReference='%s'", calendarRef, revisionReference), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get calendar, calendarRef='%s', revisionReference='%s'", calendarRef, revisionReference), ex);
        }
    }

    @Override
    public CategoryType getCategoryType(CategoryRef categoryRef) throws NotFoundException {
        return getCategoryType(categoryRef, Reference.head(new Head()));
    }

    @Override
    public CategoryType getCategoryType(CategoryRef categoryRef, long domainRevision) throws NotFoundException {
        return getCategoryType(categoryRef, Reference.version(domainRevision));
    }

    @Override
    public CategoryType getCategoryType(CategoryRef categoryRef, Reference revisionReference) throws NotFoundException {
        log.info("Trying to get category type, categoryRef='{}', revisionReference='{}'", categoryRef, revisionReference);
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setCategory(categoryRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(revisionReference, reference);
            CategoryType categoryType = versionedObject.getObject().getCategory().getData().getType();
            log.info("Category type has been found, categoryRef='{}', revisionReference='{}', categoryType='{}'", categoryRef, revisionReference, categoryType);
            return categoryType;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, categoryRef='%s', revisionReference='%s'", categoryRef, revisionReference), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get category type, categoryRef='%s', revisionReference='%s'", categoryRef, revisionReference), ex);
        }
    }
}

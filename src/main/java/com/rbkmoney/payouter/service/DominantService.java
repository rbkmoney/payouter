package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.Reference;
import com.rbkmoney.payouter.exception.NotFoundException;

public interface DominantService {
    BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef) throws NotFoundException;

    BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef, long domainRevision) throws NotFoundException;

    BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef, Reference revisionReference) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef, long domainRevision) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef, Reference revisionReference) throws NotFoundException;

    Calendar getCalendar(CalendarRef calendarRef) throws NotFoundException;

    Calendar getCalendar(CalendarRef calendarRef, long domainRevision) throws NotFoundException;

    Calendar getCalendar(CalendarRef calendarRef, Reference revisionReference) throws NotFoundException;

}

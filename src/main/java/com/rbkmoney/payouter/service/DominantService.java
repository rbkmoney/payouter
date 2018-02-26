package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.Reference;
import com.rbkmoney.payouter.exception.NotFoundException;

public interface DominantService {
    PayoutSchedule getPayoutSchedule(PayoutScheduleRef scheduleRef) throws NotFoundException;

    PayoutSchedule getPayoutSchedule(PayoutScheduleRef scheduleRef, long domainRevision) throws NotFoundException;

    PayoutSchedule getPayoutSchedule(PayoutScheduleRef scheduleRef, Reference revisionReference) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef, long domainRevision) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef, Reference revisionReference) throws NotFoundException;

    Calendar getCalendar(CalendarRef calendarRef) throws NotFoundException;

    Calendar getCalendar(CalendarRef calendarRef, long domainRevision) throws NotFoundException;

    Calendar getCalendar(CalendarRef calendarRef, Reference revisionReference) throws NotFoundException;

    CategoryType getCategoryType(CategoryRef categoryRef) throws NotFoundException;

    CategoryType getCategoryType(CategoryRef categoryRef, long domainRevision) throws NotFoundException;

    CategoryType getCategoryType(CategoryRef categoryRef, Reference revisionReference) throws NotFoundException;
}

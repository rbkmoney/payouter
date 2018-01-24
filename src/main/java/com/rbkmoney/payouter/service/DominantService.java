package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.CategoryRef;
import com.rbkmoney.damsel.domain.CategoryType;
import com.rbkmoney.damsel.domain.Schedule;
import com.rbkmoney.damsel.domain.ScheduleRef;
import com.rbkmoney.damsel.domain_config.Reference;
import com.rbkmoney.payouter.exception.NotFoundException;

public interface DominantService {
    Schedule getSchedule(ScheduleRef scheduleRef) throws NotFoundException;

    Schedule getSchedule(ScheduleRef scheduleRef, long domainRevision) throws NotFoundException;

    Schedule getSchedule(ScheduleRef scheduleRef, Reference revisionReference) throws NotFoundException;

    CategoryType getCategoryType(CategoryRef categoryRef) throws NotFoundException;

    CategoryType getCategoryType(CategoryRef categoryRef, long domainRevision) throws NotFoundException;

    CategoryType getCategoryType(CategoryRef categoryRef, Reference revisionReference) throws NotFoundException;
}

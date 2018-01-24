package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.CategoryRef;
import com.rbkmoney.damsel.domain.CategoryType;
import com.rbkmoney.damsel.domain.Schedule;
import com.rbkmoney.damsel.domain.ScheduleRef;
import com.rbkmoney.damsel.domain_config.*;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.DominantService;
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
        log.debug("Trying to get schedule, scheduleRef='{}', revisionReference='{}'", scheduleRef, revisionReference);
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setSchedule(scheduleRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(revisionReference, reference);
            Schedule schedule = versionedObject.getObject().getSchedule().getData();
            log.info("Schedule has been found, scheduleRef='{}', revisionReference='{}', schedule='{}'", scheduleRef, revisionReference, schedule);
            return schedule;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, scheduleRef='%s', revisionReference='%s'", scheduleRef, revisionReference), ex);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed to get schedule, scheduleRef='%s', revisionReference='%s'", scheduleRef, revisionReference), ex);
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
        log.debug("Trying to get category type, categoryRef='{}', revisionReference='{}'", categoryRef, revisionReference);
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setCategory(categoryRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(revisionReference, reference);
            CategoryType categoryType = versionedObject.getObject().getCategory().getData().getType();
            log.info("Category type has been found, categoryRef='{}', revisionReference='{}', categoryType='{}'", categoryRef, revisionReference, categoryType);
            return categoryType;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, categoryRef='%s', revisionReference='%s'", categoryRef, revisionReference), ex);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed to get category type, categoryRef='%s', revisionReference='%s'", categoryRef, revisionReference), ex);
        }
    }
}

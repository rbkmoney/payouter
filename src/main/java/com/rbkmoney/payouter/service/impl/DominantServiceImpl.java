package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.CategoryRef;
import com.rbkmoney.damsel.domain.CategoryType;
import com.rbkmoney.damsel.domain_config.ObjectNotFound;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.domain_config.VersionNotFound;
import com.rbkmoney.damsel.domain_config.VersionedObject;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.DominantService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DominantServiceImpl implements DominantService {

    @Autowired
    private RepositoryClientSrv.Iface dominantClient;

    @Override
    public CategoryType getCategoryType(CategoryRef categoryRef, long domainRevision) throws NotFoundException {
        try {
            com.rbkmoney.damsel.domain.Reference reference = new com.rbkmoney.damsel.domain.Reference();
            reference.setCategory(categoryRef);
            VersionedObject versionedObject = dominantClient.checkoutObject(com.rbkmoney.damsel.domain_config.Reference.version(domainRevision), reference);
            return versionedObject.getObject().getCategory().getData().getType();
        } catch (VersionNotFound | ObjectNotFound e) {
            throw new NotFoundException(String.format("Version not found, categoryId=%d, revisionId=%d", categoryRef.getId(), domainRevision), e);
        } catch (TException e) {
            throw new RuntimeException(String.format("Failed to get category type, categoryId=%d, revisionId=%d", categoryRef.getId(), domainRevision), e);
        }
    }
}

package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.CategoryRef;
import com.rbkmoney.damsel.domain.CategoryType;
import com.rbkmoney.payouter.exception.NotFoundException;

public interface DominantService {
    CategoryType getCategoryType(CategoryRef categoryRef, long domainRevision) throws NotFoundException;
}

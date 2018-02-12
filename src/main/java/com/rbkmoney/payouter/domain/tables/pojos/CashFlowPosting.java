/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables.pojos;


import com.rbkmoney.payouter.domain.enums.AccountType;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Generated;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.6"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CashFlowPosting implements Serializable {

    private static final long serialVersionUID = -579250397;

    private Long          id;
    private Long          payoutId;
    private String        planId;
    private Long          batchId;
    private Long          fromAccountId;
    private AccountType   fromAccountType;
    private Long          toAccountId;
    private AccountType   toAccountType;
    private Long          amount;
    private String        currencyCode;
    private String        description;
    private LocalDateTime createdAt;

    public CashFlowPosting() {}

    public CashFlowPosting(CashFlowPosting value) {
        this.id = value.id;
        this.payoutId = value.payoutId;
        this.planId = value.planId;
        this.batchId = value.batchId;
        this.fromAccountId = value.fromAccountId;
        this.fromAccountType = value.fromAccountType;
        this.toAccountId = value.toAccountId;
        this.toAccountType = value.toAccountType;
        this.amount = value.amount;
        this.currencyCode = value.currencyCode;
        this.description = value.description;
        this.createdAt = value.createdAt;
    }

    public CashFlowPosting(
        Long          id,
        Long          payoutId,
        String        planId,
        Long          batchId,
        Long          fromAccountId,
        AccountType   fromAccountType,
        Long          toAccountId,
        AccountType   toAccountType,
        Long          amount,
        String        currencyCode,
        String        description,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.payoutId = payoutId;
        this.planId = planId;
        this.batchId = batchId;
        this.fromAccountId = fromAccountId;
        this.fromAccountType = fromAccountType;
        this.toAccountId = toAccountId;
        this.toAccountType = toAccountType;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPayoutId() {
        return this.payoutId;
    }

    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }

    public String getPlanId() {
        return this.planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Long getBatchId() {
        return this.batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getFromAccountId() {
        return this.fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public AccountType getFromAccountType() {
        return this.fromAccountType;
    }

    public void setFromAccountType(AccountType fromAccountType) {
        this.fromAccountType = fromAccountType;
    }

    public Long getToAccountId() {
        return this.toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public AccountType getToAccountType() {
        return this.toAccountType;
    }

    public void setToAccountType(AccountType toAccountType) {
        this.toAccountType = toAccountType;
    }

    public Long getAmount() {
        return this.amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CashFlowPosting other = (CashFlowPosting) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        }
        else if (!id.equals(other.id))
            return false;
        if (payoutId == null) {
            if (other.payoutId != null)
                return false;
        }
        else if (!payoutId.equals(other.payoutId))
            return false;
        if (planId == null) {
            if (other.planId != null)
                return false;
        }
        else if (!planId.equals(other.planId))
            return false;
        if (batchId == null) {
            if (other.batchId != null)
                return false;
        }
        else if (!batchId.equals(other.batchId))
            return false;
        if (fromAccountId == null) {
            if (other.fromAccountId != null)
                return false;
        }
        else if (!fromAccountId.equals(other.fromAccountId))
            return false;
        if (fromAccountType == null) {
            if (other.fromAccountType != null)
                return false;
        }
        else if (!fromAccountType.equals(other.fromAccountType))
            return false;
        if (toAccountId == null) {
            if (other.toAccountId != null)
                return false;
        }
        else if (!toAccountId.equals(other.toAccountId))
            return false;
        if (toAccountType == null) {
            if (other.toAccountType != null)
                return false;
        }
        else if (!toAccountType.equals(other.toAccountType))
            return false;
        if (amount == null) {
            if (other.amount != null)
                return false;
        }
        else if (!amount.equals(other.amount))
            return false;
        if (currencyCode == null) {
            if (other.currencyCode != null)
                return false;
        }
        else if (!currencyCode.equals(other.currencyCode))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        }
        else if (!description.equals(other.description))
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        }
        else if (!createdAt.equals(other.createdAt))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.payoutId == null) ? 0 : this.payoutId.hashCode());
        result = prime * result + ((this.planId == null) ? 0 : this.planId.hashCode());
        result = prime * result + ((this.batchId == null) ? 0 : this.batchId.hashCode());
        result = prime * result + ((this.fromAccountId == null) ? 0 : this.fromAccountId.hashCode());
        result = prime * result + ((this.fromAccountType == null) ? 0 : this.fromAccountType.hashCode());
        result = prime * result + ((this.toAccountId == null) ? 0 : this.toAccountId.hashCode());
        result = prime * result + ((this.toAccountType == null) ? 0 : this.toAccountType.hashCode());
        result = prime * result + ((this.amount == null) ? 0 : this.amount.hashCode());
        result = prime * result + ((this.currencyCode == null) ? 0 : this.currencyCode.hashCode());
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        result = prime * result + ((this.createdAt == null) ? 0 : this.createdAt.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CashFlowPosting (");

        sb.append(id);
        sb.append(", ").append(payoutId);
        sb.append(", ").append(planId);
        sb.append(", ").append(batchId);
        sb.append(", ").append(fromAccountId);
        sb.append(", ").append(fromAccountType);
        sb.append(", ").append(toAccountId);
        sb.append(", ").append(toAccountType);
        sb.append(", ").append(amount);
        sb.append(", ").append(currencyCode);
        sb.append(", ").append(description);
        sb.append(", ").append(createdAt);

        sb.append(")");
        return sb.toString();
    }
}
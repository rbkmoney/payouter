/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables.pojos;


import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;

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
public class Payout implements Serializable {

    private static final long serialVersionUID = -1865958160;

    private Long              id;
    private String            partyId;
    private String            shopId;
    private LocalDateTime     createdAt;
    private LocalDateTime     fromTime;
    private LocalDateTime     toTime;
    private PayoutStatus      status;
    private PayoutType        type;
    private Long              amount;
    private Long              shopAcc;
    private Long              shopPayoutAcc;
    private String            currencyCode;
    private String            bankAccount;
    private String            bankLocalCode;
    private String            bankName;
    private String            bankPostAccount;
    private String            inn;
    private String            purpose;
    private String            description;
    private String            accountLegalAgreementId;
    private LocalDateTime     accountLegalAgreementSignedAt;
    private PayoutAccountType accountType;
    private Long              fee;
    private String            bankAddress;
    private String            bankBic;
    private String            bankIban;
    private String            accountLegalName;
    private String            accountTradingName;
    private String            accountRegisteredAddress;
    private String            accountActualAddress;
    private String            accountRegisteredNumber;
    private String            shopUrl;

    public Payout() {}

    public Payout(Payout value) {
        this.id = value.id;
        this.partyId = value.partyId;
        this.shopId = value.shopId;
        this.createdAt = value.createdAt;
        this.fromTime = value.fromTime;
        this.toTime = value.toTime;
        this.status = value.status;
        this.type = value.type;
        this.amount = value.amount;
        this.shopAcc = value.shopAcc;
        this.shopPayoutAcc = value.shopPayoutAcc;
        this.currencyCode = value.currencyCode;
        this.bankAccount = value.bankAccount;
        this.bankLocalCode = value.bankLocalCode;
        this.bankName = value.bankName;
        this.bankPostAccount = value.bankPostAccount;
        this.inn = value.inn;
        this.purpose = value.purpose;
        this.description = value.description;
        this.accountLegalAgreementId = value.accountLegalAgreementId;
        this.accountLegalAgreementSignedAt = value.accountLegalAgreementSignedAt;
        this.accountType = value.accountType;
        this.fee = value.fee;
        this.bankAddress = value.bankAddress;
        this.bankBic = value.bankBic;
        this.bankIban = value.bankIban;
        this.accountLegalName = value.accountLegalName;
        this.accountTradingName = value.accountTradingName;
        this.accountRegisteredAddress = value.accountRegisteredAddress;
        this.accountActualAddress = value.accountActualAddress;
        this.accountRegisteredNumber = value.accountRegisteredNumber;
        this.shopUrl = value.shopUrl;
    }

    public Payout(
        Long              id,
        String            partyId,
        String            shopId,
        LocalDateTime     createdAt,
        LocalDateTime     fromTime,
        LocalDateTime     toTime,
        PayoutStatus      status,
        PayoutType        type,
        Long              amount,
        Long              shopAcc,
        Long              shopPayoutAcc,
        String            currencyCode,
        String            bankAccount,
        String            bankLocalCode,
        String            bankName,
        String            bankPostAccount,
        String            inn,
        String            purpose,
        String            description,
        String            accountLegalAgreementId,
        LocalDateTime     accountLegalAgreementSignedAt,
        PayoutAccountType accountType,
        Long              fee,
        String            bankAddress,
        String            bankBic,
        String            bankIban,
        String            accountLegalName,
        String            accountTradingName,
        String            accountRegisteredAddress,
        String            accountActualAddress,
        String            accountRegisteredNumber,
        String            shopUrl
    ) {
        this.id = id;
        this.partyId = partyId;
        this.shopId = shopId;
        this.createdAt = createdAt;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.status = status;
        this.type = type;
        this.amount = amount;
        this.shopAcc = shopAcc;
        this.shopPayoutAcc = shopPayoutAcc;
        this.currencyCode = currencyCode;
        this.bankAccount = bankAccount;
        this.bankLocalCode = bankLocalCode;
        this.bankName = bankName;
        this.bankPostAccount = bankPostAccount;
        this.inn = inn;
        this.purpose = purpose;
        this.description = description;
        this.accountLegalAgreementId = accountLegalAgreementId;
        this.accountLegalAgreementSignedAt = accountLegalAgreementSignedAt;
        this.accountType = accountType;
        this.fee = fee;
        this.bankAddress = bankAddress;
        this.bankBic = bankBic;
        this.bankIban = bankIban;
        this.accountLegalName = accountLegalName;
        this.accountTradingName = accountTradingName;
        this.accountRegisteredAddress = accountRegisteredAddress;
        this.accountActualAddress = accountActualAddress;
        this.accountRegisteredNumber = accountRegisteredNumber;
        this.shopUrl = shopUrl;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartyId() {
        return this.partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getShopId() {
        return this.shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getFromTime() {
        return this.fromTime;
    }

    public void setFromTime(LocalDateTime fromTime) {
        this.fromTime = fromTime;
    }

    public LocalDateTime getToTime() {
        return this.toTime;
    }

    public void setToTime(LocalDateTime toTime) {
        this.toTime = toTime;
    }

    public PayoutStatus getStatus() {
        return this.status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public PayoutType getType() {
        return this.type;
    }

    public void setType(PayoutType type) {
        this.type = type;
    }

    public Long getAmount() {
        return this.amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getShopAcc() {
        return this.shopAcc;
    }

    public void setShopAcc(Long shopAcc) {
        this.shopAcc = shopAcc;
    }

    public Long getShopPayoutAcc() {
        return this.shopPayoutAcc;
    }

    public void setShopPayoutAcc(Long shopPayoutAcc) {
        this.shopPayoutAcc = shopPayoutAcc;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getBankAccount() {
        return this.bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankLocalCode() {
        return this.bankLocalCode;
    }

    public void setBankLocalCode(String bankLocalCode) {
        this.bankLocalCode = bankLocalCode;
    }

    public String getBankName() {
        return this.bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankPostAccount() {
        return this.bankPostAccount;
    }

    public void setBankPostAccount(String bankPostAccount) {
        this.bankPostAccount = bankPostAccount;
    }

    public String getInn() {
        return this.inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getPurpose() {
        return this.purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountLegalAgreementId() {
        return this.accountLegalAgreementId;
    }

    public void setAccountLegalAgreementId(String accountLegalAgreementId) {
        this.accountLegalAgreementId = accountLegalAgreementId;
    }

    public LocalDateTime getAccountLegalAgreementSignedAt() {
        return this.accountLegalAgreementSignedAt;
    }

    public void setAccountLegalAgreementSignedAt(LocalDateTime accountLegalAgreementSignedAt) {
        this.accountLegalAgreementSignedAt = accountLegalAgreementSignedAt;
    }

    public PayoutAccountType getAccountType() {
        return this.accountType;
    }

    public void setAccountType(PayoutAccountType accountType) {
        this.accountType = accountType;
    }

    public Long getFee() {
        return this.fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public String getBankAddress() {
        return this.bankAddress;
    }

    public void setBankAddress(String bankAddress) {
        this.bankAddress = bankAddress;
    }

    public String getBankBic() {
        return this.bankBic;
    }

    public void setBankBic(String bankBic) {
        this.bankBic = bankBic;
    }

    public String getBankIban() {
        return this.bankIban;
    }

    public void setBankIban(String bankIban) {
        this.bankIban = bankIban;
    }

    public String getAccountLegalName() {
        return this.accountLegalName;
    }

    public void setAccountLegalName(String accountLegalName) {
        this.accountLegalName = accountLegalName;
    }

    public String getAccountTradingName() {
        return this.accountTradingName;
    }

    public void setAccountTradingName(String accountTradingName) {
        this.accountTradingName = accountTradingName;
    }

    public String getAccountRegisteredAddress() {
        return this.accountRegisteredAddress;
    }

    public void setAccountRegisteredAddress(String accountRegisteredAddress) {
        this.accountRegisteredAddress = accountRegisteredAddress;
    }

    public String getAccountActualAddress() {
        return this.accountActualAddress;
    }

    public void setAccountActualAddress(String accountActualAddress) {
        this.accountActualAddress = accountActualAddress;
    }

    public String getAccountRegisteredNumber() {
        return this.accountRegisteredNumber;
    }

    public void setAccountRegisteredNumber(String accountRegisteredNumber) {
        this.accountRegisteredNumber = accountRegisteredNumber;
    }

    public String getShopUrl() {
        return this.shopUrl;
    }

    public void setShopUrl(String shopUrl) {
        this.shopUrl = shopUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Payout other = (Payout) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        }
        else if (!id.equals(other.id))
            return false;
        if (partyId == null) {
            if (other.partyId != null)
                return false;
        }
        else if (!partyId.equals(other.partyId))
            return false;
        if (shopId == null) {
            if (other.shopId != null)
                return false;
        }
        else if (!shopId.equals(other.shopId))
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        }
        else if (!createdAt.equals(other.createdAt))
            return false;
        if (fromTime == null) {
            if (other.fromTime != null)
                return false;
        }
        else if (!fromTime.equals(other.fromTime))
            return false;
        if (toTime == null) {
            if (other.toTime != null)
                return false;
        }
        else if (!toTime.equals(other.toTime))
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        }
        else if (!status.equals(other.status))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        }
        else if (!type.equals(other.type))
            return false;
        if (amount == null) {
            if (other.amount != null)
                return false;
        }
        else if (!amount.equals(other.amount))
            return false;
        if (shopAcc == null) {
            if (other.shopAcc != null)
                return false;
        }
        else if (!shopAcc.equals(other.shopAcc))
            return false;
        if (shopPayoutAcc == null) {
            if (other.shopPayoutAcc != null)
                return false;
        }
        else if (!shopPayoutAcc.equals(other.shopPayoutAcc))
            return false;
        if (currencyCode == null) {
            if (other.currencyCode != null)
                return false;
        }
        else if (!currencyCode.equals(other.currencyCode))
            return false;
        if (bankAccount == null) {
            if (other.bankAccount != null)
                return false;
        }
        else if (!bankAccount.equals(other.bankAccount))
            return false;
        if (bankLocalCode == null) {
            if (other.bankLocalCode != null)
                return false;
        }
        else if (!bankLocalCode.equals(other.bankLocalCode))
            return false;
        if (bankName == null) {
            if (other.bankName != null)
                return false;
        }
        else if (!bankName.equals(other.bankName))
            return false;
        if (bankPostAccount == null) {
            if (other.bankPostAccount != null)
                return false;
        }
        else if (!bankPostAccount.equals(other.bankPostAccount))
            return false;
        if (inn == null) {
            if (other.inn != null)
                return false;
        }
        else if (!inn.equals(other.inn))
            return false;
        if (purpose == null) {
            if (other.purpose != null)
                return false;
        }
        else if (!purpose.equals(other.purpose))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        }
        else if (!description.equals(other.description))
            return false;
        if (accountLegalAgreementId == null) {
            if (other.accountLegalAgreementId != null)
                return false;
        }
        else if (!accountLegalAgreementId.equals(other.accountLegalAgreementId))
            return false;
        if (accountLegalAgreementSignedAt == null) {
            if (other.accountLegalAgreementSignedAt != null)
                return false;
        }
        else if (!accountLegalAgreementSignedAt.equals(other.accountLegalAgreementSignedAt))
            return false;
        if (accountType == null) {
            if (other.accountType != null)
                return false;
        }
        else if (!accountType.equals(other.accountType))
            return false;
        if (fee == null) {
            if (other.fee != null)
                return false;
        }
        else if (!fee.equals(other.fee))
            return false;
        if (bankAddress == null) {
            if (other.bankAddress != null)
                return false;
        }
        else if (!bankAddress.equals(other.bankAddress))
            return false;
        if (bankBic == null) {
            if (other.bankBic != null)
                return false;
        }
        else if (!bankBic.equals(other.bankBic))
            return false;
        if (bankIban == null) {
            if (other.bankIban != null)
                return false;
        }
        else if (!bankIban.equals(other.bankIban))
            return false;
        if (accountLegalName == null) {
            if (other.accountLegalName != null)
                return false;
        }
        else if (!accountLegalName.equals(other.accountLegalName))
            return false;
        if (accountTradingName == null) {
            if (other.accountTradingName != null)
                return false;
        }
        else if (!accountTradingName.equals(other.accountTradingName))
            return false;
        if (accountRegisteredAddress == null) {
            if (other.accountRegisteredAddress != null)
                return false;
        }
        else if (!accountRegisteredAddress.equals(other.accountRegisteredAddress))
            return false;
        if (accountActualAddress == null) {
            if (other.accountActualAddress != null)
                return false;
        }
        else if (!accountActualAddress.equals(other.accountActualAddress))
            return false;
        if (accountRegisteredNumber == null) {
            if (other.accountRegisteredNumber != null)
                return false;
        }
        else if (!accountRegisteredNumber.equals(other.accountRegisteredNumber))
            return false;
        if (shopUrl == null) {
            if (other.shopUrl != null)
                return false;
        }
        else if (!shopUrl.equals(other.shopUrl))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.partyId == null) ? 0 : this.partyId.hashCode());
        result = prime * result + ((this.shopId == null) ? 0 : this.shopId.hashCode());
        result = prime * result + ((this.createdAt == null) ? 0 : this.createdAt.hashCode());
        result = prime * result + ((this.fromTime == null) ? 0 : this.fromTime.hashCode());
        result = prime * result + ((this.toTime == null) ? 0 : this.toTime.hashCode());
        result = prime * result + ((this.status == null) ? 0 : this.status.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.amount == null) ? 0 : this.amount.hashCode());
        result = prime * result + ((this.shopAcc == null) ? 0 : this.shopAcc.hashCode());
        result = prime * result + ((this.shopPayoutAcc == null) ? 0 : this.shopPayoutAcc.hashCode());
        result = prime * result + ((this.currencyCode == null) ? 0 : this.currencyCode.hashCode());
        result = prime * result + ((this.bankAccount == null) ? 0 : this.bankAccount.hashCode());
        result = prime * result + ((this.bankLocalCode == null) ? 0 : this.bankLocalCode.hashCode());
        result = prime * result + ((this.bankName == null) ? 0 : this.bankName.hashCode());
        result = prime * result + ((this.bankPostAccount == null) ? 0 : this.bankPostAccount.hashCode());
        result = prime * result + ((this.inn == null) ? 0 : this.inn.hashCode());
        result = prime * result + ((this.purpose == null) ? 0 : this.purpose.hashCode());
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        result = prime * result + ((this.accountLegalAgreementId == null) ? 0 : this.accountLegalAgreementId.hashCode());
        result = prime * result + ((this.accountLegalAgreementSignedAt == null) ? 0 : this.accountLegalAgreementSignedAt.hashCode());
        result = prime * result + ((this.accountType == null) ? 0 : this.accountType.hashCode());
        result = prime * result + ((this.fee == null) ? 0 : this.fee.hashCode());
        result = prime * result + ((this.bankAddress == null) ? 0 : this.bankAddress.hashCode());
        result = prime * result + ((this.bankBic == null) ? 0 : this.bankBic.hashCode());
        result = prime * result + ((this.bankIban == null) ? 0 : this.bankIban.hashCode());
        result = prime * result + ((this.accountLegalName == null) ? 0 : this.accountLegalName.hashCode());
        result = prime * result + ((this.accountTradingName == null) ? 0 : this.accountTradingName.hashCode());
        result = prime * result + ((this.accountRegisteredAddress == null) ? 0 : this.accountRegisteredAddress.hashCode());
        result = prime * result + ((this.accountActualAddress == null) ? 0 : this.accountActualAddress.hashCode());
        result = prime * result + ((this.accountRegisteredNumber == null) ? 0 : this.accountRegisteredNumber.hashCode());
        result = prime * result + ((this.shopUrl == null) ? 0 : this.shopUrl.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Payout (");

        sb.append(id);
        sb.append(", ").append(partyId);
        sb.append(", ").append(shopId);
        sb.append(", ").append(createdAt);
        sb.append(", ").append(fromTime);
        sb.append(", ").append(toTime);
        sb.append(", ").append(status);
        sb.append(", ").append(type);
        sb.append(", ").append(amount);
        sb.append(", ").append(shopAcc);
        sb.append(", ").append(shopPayoutAcc);
        sb.append(", ").append(currencyCode);
        sb.append(", ").append(bankAccount);
        sb.append(", ").append(bankLocalCode);
        sb.append(", ").append(bankName);
        sb.append(", ").append(bankPostAccount);
        sb.append(", ").append(inn);
        sb.append(", ").append(purpose);
        sb.append(", ").append(description);
        sb.append(", ").append(accountLegalAgreementId);
        sb.append(", ").append(accountLegalAgreementSignedAt);
        sb.append(", ").append(accountType);
        sb.append(", ").append(fee);
        sb.append(", ").append(bankAddress);
        sb.append(", ").append(bankBic);
        sb.append(", ").append(bankIban);
        sb.append(", ").append(accountLegalName);
        sb.append(", ").append(accountTradingName);
        sb.append(", ").append(accountRegisteredAddress);
        sb.append(", ").append(accountActualAddress);
        sb.append(", ").append(accountRegisteredNumber);
        sb.append(", ").append(shopUrl);

        sb.append(")");
        return sb.toString();
    }
}

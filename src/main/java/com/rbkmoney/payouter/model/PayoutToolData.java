package com.rbkmoney.payouter.model;

import java.time.LocalDateTime;

public class PayoutToolData {

    public long shopAccountId;

    public long shopPayoutAccountId;

    public String currencyCode;

    public String bankAccount;

    public String bankBik;

    public String bankName;

    public String bankPostAccount;

    public String inn;

    public String purpose;

    public String description;

    public String legalAgreementId;

    public LocalDateTime legalAgreementSignedAt;

    public long getShopAccountId() {
        return shopAccountId;
    }

    public void setShopAccountId(long shopAccountId) {
        this.shopAccountId = shopAccountId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public long getShopPayoutAccountId() {
        return shopPayoutAccountId;
    }

    public void setShopPayoutAccountId(long shopPayoutAccountId) {
        this.shopPayoutAccountId = shopPayoutAccountId;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankBik() {
        return bankBik;
    }

    public void setBankBik(String bankBik) {
        this.bankBik = bankBik;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankPostAccount() {
        return bankPostAccount;
    }

    public void setBankPostAccount(String bankPostAccount) {
        this.bankPostAccount = bankPostAccount;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLegalAgreementId() {
        return legalAgreementId;
    }

    public void setLegalAgreementId(String legalAgreementId) {
        this.legalAgreementId = legalAgreementId;
    }

    public LocalDateTime getLegalAgreementSignedAt() {
        return legalAgreementSignedAt;
    }

    public void setLegalAgreementSignedAt(LocalDateTime legalAgreementSignedAt) {
        this.legalAgreementSignedAt = legalAgreementSignedAt;
    }

    @Override
    public String toString() {
        return "PayoutToolData{" +
                "shopAccountId=" + shopAccountId +
                ", shopPayoutAccountId=" + shopPayoutAccountId +
                ", currencyCode='" + currencyCode + '\'' +
                ", bankAccount='" + bankAccount + '\'' +
                ", bankBik='" + bankBik + '\'' +
                ", bankName='" + bankName + '\'' +
                ", bankPostAccount='" + bankPostAccount + '\'' +
                ", inn='" + inn + '\'' +
                ", purpose='" + purpose + '\'' +
                ", description='" + description + '\'' +
                ", legalAgreementId='" + legalAgreementId + '\'' +
                ", legalAgreementSignedAt=" + legalAgreementSignedAt +
                '}';
    }
}

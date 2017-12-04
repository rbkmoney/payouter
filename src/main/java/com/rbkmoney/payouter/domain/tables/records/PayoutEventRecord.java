/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables.records;


import com.rbkmoney.payouter.domain.tables.PayoutEvent;

import java.time.LocalDateTime;

import javax.annotation.Generated;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


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
public class PayoutEventRecord extends UpdatableRecordImpl<PayoutEventRecord> {

    private static final long serialVersionUID = 624809559;

    /**
     * Setter for <code>sht.payout_event.event_id</code>.
     */
    public void setEventId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>sht.payout_event.event_id</code>.
     */
    public Long getEventId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>sht.payout_event.event_created_at</code>.
     */
    public void setEventCreatedAt(LocalDateTime value) {
        set(1, value);
    }

    /**
     * Getter for <code>sht.payout_event.event_created_at</code>.
     */
    public LocalDateTime getEventCreatedAt() {
        return (LocalDateTime) get(1);
    }

    /**
     * Setter for <code>sht.payout_event.event_type</code>.
     */
    public void setEventType(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>sht.payout_event.event_type</code>.
     */
    public String getEventType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>sht.payout_event.payout_id</code>.
     */
    public void setPayoutId(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_id</code>.
     */
    public String getPayoutId() {
        return (String) get(3);
    }

    /**
     * Setter for <code>sht.payout_event.payout_party_id</code>.
     */
    public void setPayoutPartyId(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_party_id</code>.
     */
    public String getPayoutPartyId() {
        return (String) get(4);
    }

    /**
     * Setter for <code>sht.payout_event.payout_shop_id</code>.
     */
    public void setPayoutShopId(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_shop_id</code>.
     */
    public String getPayoutShopId() {
        return (String) get(5);
    }

    /**
     * Setter for <code>sht.payout_event.payout_created_at</code>.
     */
    public void setPayoutCreatedAt(LocalDateTime value) {
        set(6, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_created_at</code>.
     */
    public LocalDateTime getPayoutCreatedAt() {
        return (LocalDateTime) get(6);
    }

    /**
     * Setter for <code>sht.payout_event.payout_status</code>.
     */
    public void setPayoutStatus(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_status</code>.
     */
    public String getPayoutStatus() {
        return (String) get(7);
    }

    /**
     * Setter for <code>sht.payout_event.payout_status_cancel_details</code>.
     */
    public void setPayoutStatusCancelDetails(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_status_cancel_details</code>.
     */
    public String getPayoutStatusCancelDetails() {
        return (String) get(8);
    }

    /**
     * Setter for <code>sht.payout_event.payout_type</code>.
     */
    public void setPayoutType(String value) {
        set(9, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_type</code>.
     */
    public String getPayoutType() {
        return (String) get(9);
    }

    /**
     * Setter for <code>sht.payout_event.payout_cash_flow</code>.
     */
    public void setPayoutCashFlow(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_cash_flow</code>.
     */
    public String getPayoutCashFlow() {
        return (String) get(10);
    }

    /**
     * Setter for <code>sht.payout_event.payout_paid_details_type</code>.
     */
    public void setPayoutPaidDetailsType(String value) {
        set(11, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_paid_details_type</code>.
     */
    public String getPayoutPaidDetailsType() {
        return (String) get(11);
    }

    /**
     * Setter for <code>sht.payout_event.payout_card_token</code>.
     */
    public void setPayoutCardToken(String value) {
        set(12, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_card_token</code>.
     */
    public String getPayoutCardToken() {
        return (String) get(12);
    }

    /**
     * Setter for <code>sht.payout_event.payout_card_payment_system</code>.
     */
    public void setPayoutCardPaymentSystem(String value) {
        set(13, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_card_payment_system</code>.
     */
    public String getPayoutCardPaymentSystem() {
        return (String) get(13);
    }

    /**
     * Setter for <code>sht.payout_event.payout_card_bin</code>.
     */
    public void setPayoutCardBin(String value) {
        set(14, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_card_bin</code>.
     */
    public String getPayoutCardBin() {
        return (String) get(14);
    }

    /**
     * Setter for <code>sht.payout_event.payout_card_masked_pan</code>.
     */
    public void setPayoutCardMaskedPan(String value) {
        set(15, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_card_masked_pan</code>.
     */
    public String getPayoutCardMaskedPan() {
        return (String) get(15);
    }

    /**
     * Setter for <code>sht.payout_event.payout_card_provider_name</code>.
     */
    public void setPayoutCardProviderName(String value) {
        set(16, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_card_provider_name</code>.
     */
    public String getPayoutCardProviderName() {
        return (String) get(16);
    }

    /**
     * Setter for <code>sht.payout_event.payout_card_provider_transaction_id</code>.
     */
    public void setPayoutCardProviderTransactionId(String value) {
        set(17, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_card_provider_transaction_id</code>.
     */
    public String getPayoutCardProviderTransactionId() {
        return (String) get(17);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_id</code>.
     */
    public void setPayoutAccountId(String value) {
        set(18, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_id</code>.
     */
    public String getPayoutAccountId() {
        return (String) get(18);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_bank_name</code>.
     */
    public void setPayoutAccountBankName(String value) {
        set(19, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_bank_name</code>.
     */
    public String getPayoutAccountBankName() {
        return (String) get(19);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_bank_post_id</code>.
     */
    public void setPayoutAccountBankPostId(String value) {
        set(20, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_bank_post_id</code>.
     */
    public String getPayoutAccountBankPostId() {
        return (String) get(20);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_bank_bik</code>.
     */
    public void setPayoutAccountBankBik(String value) {
        set(21, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_bank_bik</code>.
     */
    public String getPayoutAccountBankBik() {
        return (String) get(21);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_inn</code>.
     */
    public void setPayoutAccountInn(String value) {
        set(22, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_inn</code>.
     */
    public String getPayoutAccountInn() {
        return (String) get(22);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_purpose</code>.
     */
    public void setPayoutAccountPurpose(String value) {
        set(23, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_purpose</code>.
     */
    public String getPayoutAccountPurpose() {
        return (String) get(23);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_legal_agreement_id</code>.
     */
    public void setPayoutAccountLegalAgreementId(String value) {
        set(24, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_legal_agreement_id</code>.
     */
    public String getPayoutAccountLegalAgreementId() {
        return (String) get(24);
    }

    /**
     * Setter for <code>sht.payout_event.payout_account_legal_agreement_signed_at</code>.
     */
    public void setPayoutAccountLegalAgreementSignedAt(LocalDateTime value) {
        set(25, value);
    }

    /**
     * Getter for <code>sht.payout_event.payout_account_legal_agreement_signed_at</code>.
     */
    public LocalDateTime getPayoutAccountLegalAgreementSignedAt() {
        return (LocalDateTime) get(25);
    }

    /**
     * Setter for <code>sht.payout_event.user_id</code>.
     */
    public void setUserId(String value) {
        set(26, value);
    }

    /**
     * Getter for <code>sht.payout_event.user_id</code>.
     */
    public String getUserId() {
        return (String) get(26);
    }

    /**
     * Setter for <code>sht.payout_event.user_type</code>.
     */
    public void setUserType(String value) {
        set(27, value);
    }

    /**
     * Getter for <code>sht.payout_event.user_type</code>.
     */
    public String getUserType() {
        return (String) get(27);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PayoutEventRecord
     */
    public PayoutEventRecord() {
        super(PayoutEvent.PAYOUT_EVENT);
    }

    /**
     * Create a detached, initialised PayoutEventRecord
     */
    public PayoutEventRecord(Long eventId, LocalDateTime eventCreatedAt, String eventType, String payoutId, String payoutPartyId, String payoutShopId, LocalDateTime payoutCreatedAt, String payoutStatus, String payoutStatusCancelDetails, String payoutType, String payoutCashFlow, String payoutPaidDetailsType, String payoutCardToken, String payoutCardPaymentSystem, String payoutCardBin, String payoutCardMaskedPan, String payoutCardProviderName, String payoutCardProviderTransactionId, String payoutAccountId, String payoutAccountBankName, String payoutAccountBankPostId, String payoutAccountBankBik, String payoutAccountInn, String payoutAccountPurpose, String payoutAccountLegalAgreementId, LocalDateTime payoutAccountLegalAgreementSignedAt, String userId, String userType) {
        super(PayoutEvent.PAYOUT_EVENT);

        set(0, eventId);
        set(1, eventCreatedAt);
        set(2, eventType);
        set(3, payoutId);
        set(4, payoutPartyId);
        set(5, payoutShopId);
        set(6, payoutCreatedAt);
        set(7, payoutStatus);
        set(8, payoutStatusCancelDetails);
        set(9, payoutType);
        set(10, payoutCashFlow);
        set(11, payoutPaidDetailsType);
        set(12, payoutCardToken);
        set(13, payoutCardPaymentSystem);
        set(14, payoutCardBin);
        set(15, payoutCardMaskedPan);
        set(16, payoutCardProviderName);
        set(17, payoutCardProviderTransactionId);
        set(18, payoutAccountId);
        set(19, payoutAccountBankName);
        set(20, payoutAccountBankPostId);
        set(21, payoutAccountBankBik);
        set(22, payoutAccountInn);
        set(23, payoutAccountPurpose);
        set(24, payoutAccountLegalAgreementId);
        set(25, payoutAccountLegalAgreementSignedAt);
        set(26, userId);
        set(27, userType);
    }
}

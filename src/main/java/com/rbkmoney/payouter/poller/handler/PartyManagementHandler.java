package com.rbkmoney.payouter.poller.handler;

import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.PartyChange;

public interface PartyManagementHandler extends Handler<PartyChange, Event> {
}

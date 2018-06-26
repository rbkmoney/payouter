package com.rbkmoney.payouter.listener;

import com.rbkmoney.eventstock.client.DefaultSubscriberConfig;
import com.rbkmoney.eventstock.client.EventConstraint;
import com.rbkmoney.eventstock.client.EventPublisher;
import com.rbkmoney.eventstock.client.SubscriberConfig;
import com.rbkmoney.eventstock.client.poll.EventFlowFilter;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.domain.tables.pojos.EventStockMeta;
import com.rbkmoney.payouter.service.EventStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OnStart implements ApplicationListener<ApplicationReadyEvent> {

    private final EventPublisher eventPublisher;

    private final EventStockService eventStockService;

    public OnStart(EventPublisher eventPublisher, EventStockService eventStockService) {
        this.eventPublisher = eventPublisher;
        this.eventStockService = eventStockService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        EventConstraint.EventIDRange eventIDRange = new EventConstraint.EventIDRange();
        Optional<EventStockMeta> eventStockMetaOptional = eventStockService.getLastEventId();
        if (eventStockMetaOptional.isPresent() && eventStockMetaOptional.get().getLastEventId() != null) {
            EventStockMeta eventStockMeta = eventStockMetaOptional.get();
            eventIDRange.setFromExclusive(eventStockMeta.getLastEventId());
        }
        SubscriberConfig subscriberConfig = new DefaultSubscriberConfig<>(
                new EventFlowFilter(
                        new EventConstraint(eventIDRange)
                )
        );

        eventPublisher.subscribe(subscriberConfig);
    }
}

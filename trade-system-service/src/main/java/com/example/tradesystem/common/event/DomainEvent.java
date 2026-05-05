package com.example.tradesystem.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类
 */
@Getter
public abstract class DomainEvent extends ApplicationEvent {
    
    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String eventType;

    public DomainEvent(Object source, String eventType) {
        super(source);
        this.eventId = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        this.occurredAt = LocalDateTime.now();
        this.eventType = eventType;
    }
}

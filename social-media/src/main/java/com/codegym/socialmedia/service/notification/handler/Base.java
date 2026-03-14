package com.codegym.socialmedia.service.notification.handler;

import com.codegym.socialmedia.component.NotificationMapper;
import com.codegym.socialmedia.model.social_action.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public abstract class Base {
    protected final SimpMessagingTemplate messagingTemplate;
    protected final NotificationMapper mapper;

    public Base(SimpMessagingTemplate messagingTemplate, NotificationMapper mapper) {
        this.messagingTemplate = messagingTemplate;
        this.mapper = mapper;
    }

    public abstract String getReferenceType();

    public void handle(Notification notification) {
        messagingTemplate.convertAndSendToUser(
                notification.getReceiver().getUsername(),
                "/queue/notifications",
                mapper.toDto(notification, getReferenceType())
        );
    }
}

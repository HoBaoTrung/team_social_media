package com.codegym.socialmedia.service.notification.handler;

import com.codegym.socialmedia.model.social_action.Notification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NotificationHandlerFactory {

    private final Map<Notification.NotificationType, NotificationHandler> handlers;

    public NotificationHandlerFactory(List<NotificationHandler> handlerList) {
        handlers = handlerList.stream()
                .collect(Collectors.toMap(NotificationHandler::getType, h -> h));
    }

    public NotificationHandler getHandler(Notification.NotificationType type) {
        return handlers.get(type);
    }
}
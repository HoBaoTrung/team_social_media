package com.codegym.socialmedia.service.notification.handler;

import com.codegym.socialmedia.model.social_action.Notification;

public interface NotificationHandler {
    Notification.NotificationType getType();
    void handle(Notification notification);
    String getReferenceType();
}

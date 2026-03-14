package com.codegym.socialmedia.service.notification.handler;

import com.codegym.socialmedia.component.NotificationMapper;
import com.codegym.socialmedia.model.social_action.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReplyCommentHandler extends BaseComment implements NotificationHandler {


    public ReplyCommentHandler(SimpMessagingTemplate messagingTemplate, NotificationMapper mapper) {
        super(messagingTemplate, mapper);
    }

    @Override
    public Notification.NotificationType getType() {
        return Notification.NotificationType.REPLY_COMMENT;
    }

}
package com.codegym.socialmedia.service.notification.handler;

import com.codegym.socialmedia.component.NotificationMapper;
import com.codegym.socialmedia.model.social_action.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class LikePostHandler extends BasePost implements NotificationHandler {

    public LikePostHandler(SimpMessagingTemplate messagingTemplate, NotificationMapper mapper) {
        super(messagingTemplate, mapper);
    }

    @Override
    public Notification.NotificationType getType() {
        return Notification.NotificationType.LIKE_POST;
    }

//    @Override
//    public void handle(Notification notification) {
//        messagingTemplate.convertAndSendToUser(
//                notification.getReceiver().getUsername(),
//                "/queue/notifications",
//                mapper.toDto(notification, getReferenceType())
//        );
//    }



}
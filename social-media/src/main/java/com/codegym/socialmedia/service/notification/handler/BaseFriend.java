package com.codegym.socialmedia.service.notification.handler;

import com.codegym.socialmedia.component.NotificationMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class BaseFriend  extends Base{

    public BaseFriend(SimpMessagingTemplate messagingTemplate, NotificationMapper mapper) {
        super(messagingTemplate, mapper);
    }

    public String getReferenceType() {
        return "FRIENDSHIP";
    }
}

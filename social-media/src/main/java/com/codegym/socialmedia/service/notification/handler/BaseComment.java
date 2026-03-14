package com.codegym.socialmedia.service.notification.handler;

import com.codegym.socialmedia.component.NotificationMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class BaseComment extends Base{


    public BaseComment(SimpMessagingTemplate messagingTemplate, NotificationMapper mapper) {
        super(messagingTemplate, mapper);
    }

    public String getReferenceType() {
        return "COMMENT";
    }
}

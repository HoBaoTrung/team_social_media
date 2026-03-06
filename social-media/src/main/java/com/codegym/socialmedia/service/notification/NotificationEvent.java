package com.codegym.socialmedia.service.notification;

import java.io.Serializable;

public record NotificationEvent(Long notificationId,
                                String receiverUsername) implements Serializable {
}

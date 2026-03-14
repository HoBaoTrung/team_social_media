package com.codegym.socialmedia.component;

import com.codegym.socialmedia.dto.NotificationDTO;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Notification;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class NotificationMapper {
    public NotificationDTO toDto(Notification n, String referenceType ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDate = n.getCreatedAt().format(formatter);
        User s = n.getSender();
        boolean isRead = n.isRead();
        return new NotificationDTO(
                n.getId(),
                n.getNotificationType().name(),
                formattedDate,
                n.getReferenceId(),
                referenceType, isRead,
                new NotificationDTO.SenderDTO(s.getId(), s.getUsername(), s.getProfilePicture(),s.getFullName())
        );
    }
}
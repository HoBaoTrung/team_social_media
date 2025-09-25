package com.codegym.socialmedia.dto;

public record NotificationDTO(
        Long id,
        String notificationType,
        String createdAt,
        Long referenceId,
        String referenceType,
        boolean isRead,
        SenderDTO sender
) {
    public record SenderDTO(Long id, String username, String avatarUrl, String fullName) {}
}


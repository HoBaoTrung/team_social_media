package com.codegym.socialmedia.service.notification;

import com.codegym.socialmedia.component.NotificationMapper;
import com.codegym.socialmedia.dto.NotificationDTO;
import com.codegym.socialmedia.model.social_action.Notification;
import com.codegym.socialmedia.repository.NotificationRepository;
import com.codegym.socialmedia.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final NotificationMapper mapper;
    private final SimpMessagingTemplate messaging;

    @Autowired
    private UserService userService;

    @Transactional
    public Notification notify(
            Long senderId, Long receiverId,
            Notification.NotificationType type, Notification.ReferenceType refType, Long refId) {

        if (Objects.equals(senderId, receiverId)) return null; // tránh tự notify chính mình

        Notification n = new Notification();
        n.setSender(userService.getUserById(senderId));
        n.setReceiver(userService.getUserById(receiverId));
        n.setNotificationType(type);
        n.setReferenceType(refType);
        n.setReferenceId(refId);
        n = repo.save(n);

        // Gửi notification thông thường
        String userKey = n.getReceiver().getUsername();
        messaging.convertAndSendToUser(userKey, "/queue/notifications", mapper.toDto(n));

        return n;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> list(Long receiverId, Pageable pageable) {
        return repo.findByReceiverId(receiverId, pageable).map(mapper::toDto);
    }

    @Transactional
    public void markRead(Long id, Long receiverId) {
        Notification n = repo.findById(id).orElseThrow();
        if (!n.getReceiver().getId().equals(receiverId)) throw new AccessDeniedException("Not owner");
        if (!Boolean.TRUE.equals(n.isRead())) {
            n.setRead(true);
        }
    }

    @Transactional
    public int markAllRead(Long receiverId) {
        var page = repo.findByReceiverId(receiverId, PageRequest.of(0, 200)); // batch
        int count = 0;
        for (Notification n : page) {
            if (!n.isRead()) { n.setRead(true); count++; }
        }
        return count;
    }

    public long countUnread(Long receiverId) {
        return repo.countByReceiverIdAndIsReadFalse(receiverId);
    }
}
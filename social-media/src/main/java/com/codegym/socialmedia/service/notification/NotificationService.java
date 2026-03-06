package com.codegym.socialmedia.service.notification;

import com.codegym.socialmedia.component.NotificationMapper;
import com.codegym.socialmedia.dto.NotificationDTO;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Notification;
import com.codegym.socialmedia.repository.notification.NotificationRepository;
import com.codegym.socialmedia.service.user.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final NotificationMapper mapper;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Notification notify(
            Long senderId, Long receiverId,
            Notification.NotificationType type, Notification.ReferenceType refType, Long refId) {

        if (Objects.equals(senderId, receiverId)) return null; // tránh tự notify chính mình

        User sender = entityManager.getReference(User.class, senderId);
        User receiver = entityManager.getReference(User.class, receiverId);

        Notification n = new Notification();
        n.setSender(sender);
        n.setReceiver(receiver);
        n.setNotificationType(type);
        n.setReferenceType(refType);
        n.setReferenceId(refId);
        n = repo.save(n);

        eventPublisher.publishEvent(
                new NotificationEvent(
                        n.getId(),
                        receiver.getUsername()
                )
        );

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
//        var page = repo.findByReceiverId(receiverId, PageRequest.of(0, 200)); // batch
//        int count = 0;
//        for (Notification n : page) {
//            if (!n.isRead()) { n.setRead(true); count++; }
//        }
        return repo.markAllRead(receiverId);
    }

    public long countUnread(Long receiverId) {
        return repo.countByReceiverIdAndIsReadFalse(receiverId);
    }
}
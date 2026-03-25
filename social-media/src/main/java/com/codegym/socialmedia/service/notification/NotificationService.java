package com.codegym.socialmedia.service.notification;

import com.codegym.socialmedia.component.NotificationMapper;
import com.codegym.socialmedia.dto.NotificationDTO;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Notification;
import com.codegym.socialmedia.repository.notification.NotificationRepository;
import com.codegym.socialmedia.service.notification.handler.NotificationHandler;
import com.codegym.socialmedia.service.notification.handler.NotificationHandlerFactory;
import com.codegym.socialmedia.service.user.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationHandlerFactory notificationHandlerFactory;
    private final NotificationRepository repo;
    private final NotificationMapper mapper;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Notification notify(
            Long senderId,
            Long receiverId,
            Notification.NotificationType type,
            Long refId) {

        if (Objects.equals(senderId, receiverId)) {
            return null;
        }

        User sender = entityManager.getReference(User.class, senderId);
        User receiver = entityManager.getReference(User.class, receiverId);

        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setReceiver(receiver);
        notification.setNotificationType(type);
        notification.setReferenceId(refId);

        notification = repo.save(notification);

        eventPublisher.publishEvent(
                new NotificationEvent(
                        notification.getId(),
                        receiver.getUsername()
                )
        );

        return notification;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> list(Long receiverId, Pageable pageable) {

        Page<Notification> notifications = repo.findByReceiverId(receiverId, pageable);

        List<NotificationDTO> result = new ArrayList<>();

        for (Notification notification : notifications.getContent()) {

            Notification.NotificationType type = notification.getNotificationType();

            NotificationHandler handler = notificationHandlerFactory.getHandler(type);

            String referenceType = handler.getReferenceType();

            NotificationDTO dto = mapper.toDto(notification, referenceType);

            result.add(dto);
        }

        return new PageImpl<>(result, pageable, notifications.getTotalElements());
    }

    @Transactional
    public void markRead(Long id, Long receiverId) {
        Notification n = repo.findById(id).orElseThrow();

        if (!n.getReceiver().getId().equals(receiverId)) {
            throw new AccessDeniedException("Not owner");
        }

        if (!n.isRead()) {
            n.setRead(true);
        }
    }

    @Transactional
    public int markAllRead(Long receiverId) {
        return repo.markAllRead(receiverId);
    }

    public long countUnread(Long receiverId) {
        return repo.countByReceiverIdAndIsReadFalse(receiverId);
    }
}
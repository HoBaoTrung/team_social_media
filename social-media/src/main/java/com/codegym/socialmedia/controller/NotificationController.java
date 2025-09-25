package com.codegym.socialmedia.controller;

import com.codegym.socialmedia.dto.NotificationDTO;
import com.codegym.socialmedia.model.social_action.Notification;
import com.codegym.socialmedia.service.notification.NotificationService;
import com.codegym.socialmedia.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    private final UserService userService;

    public NotificationController(NotificationService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping
    public Page<NotificationDTO> list(Pageable pageable) {
        Long me = userService.getCurrentUser().getId();
        return service.list(me, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending()));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        Long me = userService.getCurrentUser().getId();
        return Map.of("count", service.countUnread(me));
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        Long me = userService.getCurrentUser().getId();
        service.markRead(id, me);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAll() {
        Long me = userService.getCurrentUser().getId();
        return Map.of("updated", service.markAllRead(me));
    }
}

package com.codegym.socialmedia.service.notification;

import com.codegym.socialmedia.repository.notification.NotificationRepository;
import com.codegym.socialmedia.service.notification.handler.NotificationHandler;
import com.codegym.socialmedia.service.notification.handler.NotificationHandlerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisConsumer {

    private final StringRedisTemplate redisTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationHandlerFactory handlerFactory;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_KEY = "notification:queue";

    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::consume);
        worker.setName("redis-notification-worker");
        worker.start();
    }

    private void consume() {

        while (true) {
            try {

                String json = redisTemplate.opsForList()
                        .rightPop(QUEUE_KEY, Duration.ofSeconds(5));

                if (json != null) {

                    NotificationEvent event =
                            objectMapper.readValue(json, NotificationEvent.class);

                    process(event);
                }

            } catch (Exception e) {
                log.error("Error consuming notification queue", e);
            }
        }
    }

    private void process(NotificationEvent event) {

        notificationRepository.findById(event.notificationId())
                .ifPresent(notification -> {

                    NotificationHandler handler =
                            handlerFactory.getHandler(notification.getNotificationType());

                    if (handler != null) {
                        handler.handle(notification);
                    }

                    log.info("Notification processed for {}", event.receiverUsername());
                });
    }
}
package com.codegym.socialmedia.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationRedisProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String QUEUE_KEY = "notification:queue";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForList()
                    .leftPush(QUEUE_KEY, json);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
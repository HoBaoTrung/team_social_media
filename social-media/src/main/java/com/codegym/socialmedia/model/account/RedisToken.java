package com.codegym.socialmedia.model.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("RedisHas")
public class RedisToken {
    @Id
    private String jwtId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiredTime;
}

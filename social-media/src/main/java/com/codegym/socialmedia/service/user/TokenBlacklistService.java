package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.model.account.RedisToken;
import com.codegym.socialmedia.repository.user.RedisTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    @Autowired
    private RedisTokenRepository redisTokenRepository;

    public void blacklistToken(String token, long expiryInSeconds) {
        RedisToken redisToken = new RedisToken(token, expiryInSeconds);
        redisTokenRepository.save(redisToken);
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTokenRepository.findById(token).isPresent();
    }
}
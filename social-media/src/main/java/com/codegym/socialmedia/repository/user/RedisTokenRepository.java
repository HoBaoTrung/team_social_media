package com.codegym.socialmedia.repository.user;

import com.codegym.socialmedia.model.account.RedisToken;
import org.springframework.data.repository.CrudRepository;

public interface RedisTokenRepository extends CrudRepository<RedisToken, String> {
}

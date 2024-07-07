package org.zeuscommerce.app.cache;

/*

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisCacheService {

    @Autowired
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public Mono<Object> get(String key) {
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> set(String key, Object value) {
        return reactiveRedisTemplate.opsForValue().set(key, value);
    }

    public Mono<Boolean> delete(String key) {
        return reactiveRedisTemplate.opsForValue().delete(key);
    }


}
*/
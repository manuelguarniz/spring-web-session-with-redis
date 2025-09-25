package com.example.springwebsession.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.RedisSessionExpirationStore;
import org.springframework.session.data.redis.SortedSetRedisSessionExpirationStore;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

//@Configuration
//@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "${spring.session.redis.namespace}")
public class SessionConfig {
  
//  @Value("${spring.session.redis.namespace}")
//  private String namespace;
//
//    @Bean
//    public RedisSessionExpirationStore redisSessionExpirationStore(RedisConnectionFactory redisConnectionFactory) {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setKeySerializer(RedisSerializer.string());
//        redisTemplate.setHashKeySerializer(RedisSerializer.string());
//        redisTemplate.setConnectionFactory(redisConnectionFactory);
//        redisTemplate.afterPropertiesSet();
//        return new SortedSetRedisSessionExpirationStore(redisTemplate, namespace);
//    }

}

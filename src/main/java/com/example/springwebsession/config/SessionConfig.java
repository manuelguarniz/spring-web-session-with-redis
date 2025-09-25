package com.example.springwebsession.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

/**
 * Configuración de Spring Session con Redis para WebFlux
 * Usa el namespace configurado en application.yml
 */
@Slf4j
@Configuration
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 1800, // 30 minutos
    redisNamespace = "${spring.session.redis.namespace}")
public class SessionConfig {

  // Spring Boot configura automáticamente:
  // - ReactiveRedisConnectionFactory
  // - ReactiveSessionRepository
  // - WebSessionStore
  // - WebSessionManager
  // - ReactiveRedisTemplate
}

package com.example.springwebsession.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

/**
 * Configuración de Spring Session con Redis para WebFlux
 * Spring Boot se encarga automáticamente de la configuración
 */
@Slf4j
@Configuration
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 1800) // 30 minutos
public class SessionConfig {

  // Spring Boot configura automáticamente:
  // - ReactiveRedisConnectionFactory
  // - ReactiveSessionRepository
  // - WebSessionStore
  // - WebSessionManager
}

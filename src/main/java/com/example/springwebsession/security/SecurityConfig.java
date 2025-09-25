package com.example.springwebsession.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

/**
 * Configuración de seguridad reactiva para WebFlux
 * Protege el endpoint /api/hello y mantiene /actuator disponible
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  /**
   * Configura la cadena de filtros de seguridad reactiva
   * 
   * @param http ServerHttpSecurity para configurar la seguridad reactiva
   * @return SecurityWebFilterChain configurada
   */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    log.info("Configurando cadena de filtros de seguridad reactiva");

    return http
        // Deshabilitar CSRF para APIs REST
        .csrf(org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec::disable)

        // Configurar autorización
        .authorizeExchange(auth -> auth
            // Permitir acceso a actuator sin autenticación
            .pathMatchers("/actuator/**").permitAll()

            // Permitir endpoints de autenticación sin autenticación
            .pathMatchers("/auth/**").permitAll()

            // Permitir endpoints de sesión sin autenticación
            .pathMatchers("/api/session/**").permitAll()

            // Proteger el endpoint /api/hello
            .pathMatchers("/api/hello").authenticated()

            // Permitir otros endpoints públicos si los hay
            .anyExchange().permitAll())

        .build();
  }

  // Configuración simplificada - Spring Security manejará la autenticación
  // automáticamente
}

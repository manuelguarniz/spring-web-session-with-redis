package com.example.springwebsession.security;

import com.example.springwebsession.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * Configuración de seguridad reactiva para WebFlux Protege el endpoint
 * /api/hello y mantiene /actuator disponible
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final OtpService otpService;

  @Bean
  public ServerSecurityContextRepository securityContextRepository() {
    return new WebSessionServerSecurityContextRepository();
  }

  // ReactiveAuthenticationManager eliminado - ahora se maneja en
  // OTPAuthenticationWebFilter

  /**
   * Configura la cadena de filtros de seguridad reactiva
   * 
   * @param http ServerHttpSecurity para configurar la seguridad reactiva
   * @return SecurityWebFilterChain configurada
   */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    log.info("Configurando cadena de filtros de seguridad reactiva con OTP simplificado");

    // Crear filtro de autenticación OTP simplificado
    OTPAuthenticationWebFilter otpFilter = new OTPAuthenticationWebFilter(otpService);

    // ✅ CONFIGURAR MATCHER - Procesar /auth/validate y endpoints protegidos
    otpFilter.setRequiresAuthenticationMatcher(
        ServerWebExchangeMatchers.pathMatchers("/auth/validate", "/api/hello", "/api/protected/**"));

    log.info("Filtro OTP configurado para: /auth/validate, /api/hello, /api/protected/**");

    return http
        // Deshabilitar CSRF para APIs REST
        .csrf(ServerHttpSecurity.CsrfSpec::disable)

        // Configurar autorización
        .authorizeExchange(auth -> auth
            // Permitir acceso a actuator sin autenticación
            .pathMatchers("/actuator/**").permitAll()

            // Permitir endpoints de autenticación sin autenticación (excepto
            // /auth/validate)
            .pathMatchers("/auth/login", "/auth/status", "/auth/logout").permitAll()

            // Permitir endpoints de sesión sin autenticación
            .pathMatchers("/api/session/**").permitAll()

            // Proteger endpoints que requieren autenticación
            .pathMatchers("/auth/validate", "/api/hello", "/api/protected/**").authenticated()

            // Permitir otros endpoints públicos si los hay
            .anyExchange().permitAll())

        // Agregar filtro de autenticación OTP simplificado
        .addFilterAt(otpFilter, SecurityWebFiltersOrder.AUTHENTICATION)

        .build();
  }

  // Configuración simplificada - Spring Security manejará la autenticación
  // automáticamente
}

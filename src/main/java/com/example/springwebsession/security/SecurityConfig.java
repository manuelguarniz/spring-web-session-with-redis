package com.example.springwebsession.security;

import com.example.springwebsession.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

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

//  @Bean
//  public ReactiveAuthenticationManager otpAuthenticationManager() {
//      return new OTPReactiveAuthenticationManager(otpService);
//  }
  
  /**
   * Configura la cadena de filtros de seguridad reactiva
   * 
   * @param http ServerHttpSecurity para configurar la seguridad reactiva
   * @return SecurityWebFilterChain configurada
   */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveAuthenticationManager otpAuthenticationManager) {
    log.info("Configurando cadena de filtros de seguridad reactiva con OTP");
 // Creamos el filtro de autenticación
    AuthenticationWebFilter otpAuthenticationFilter = new AuthenticationWebFilter(otpAuthenticationManager);

    // Configuramos la ruta que el filtro procesará
    otpAuthenticationFilter.setRequiresAuthenticationMatcher(
        new PathPatternParserServerWebExchangeMatcher("/auth/validate"));

    // Configuramos el convertidor que extrae las credenciales
    otpAuthenticationFilter.setServerAuthenticationConverter(new OTPAuthenticationConverter(otpService));

    return http
        // Deshabilitar CSRF para APIs REST
        .csrf(ServerHttpSecurity.CsrfSpec::disable)

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

        // Agregar filtro de autenticación OTP personalizado
        .addFilterAt(otpAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

        .build();
  }

  // Configuración simplificada - Spring Security manejará la autenticación
  // automáticamente
}

package com.example.springwebsession.security;

import com.example.springwebsession.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Filtro de autenticación que maneja:
 * 1. Validación de OTP en /auth/validate
 * 2. Verificación de autenticación en endpoints protegidos basado en Spring
 * Session
 */
@Slf4j
public class OTPAuthenticationWebFilter extends AuthenticationWebFilter {

  private final OtpService otpService;

  public OTPAuthenticationWebFilter(OtpService otpService) {
    super(createAuthenticationManager());
    this.otpService = otpService;
    setServerAuthenticationConverter(createAuthenticationConverter(otpService));
  }

  /**
   * Crea el manager de autenticación simplificado
   * 
   * @return ReactiveAuthenticationManager
   */
  private static ReactiveAuthenticationManager createAuthenticationManager() {
    return authentication -> {
      log.info("Autenticación procesada exitosamente: {}", authentication.getName());
      // La validación ya se hizo en el converter
      return Mono.just(authentication);
    };
  }

  /**
   * Crea el convertidor de autenticación que maneja:
   * 1. Validación de OTP en /auth/validate
   * 2. Verificación de autenticación en endpoints protegidos
   * 
   * @param otpService Servicio OTP
   * @return ServerAuthenticationConverter
   */
  private static ServerAuthenticationConverter createAuthenticationConverter(OtpService otpService) {
    return exchange -> {
      String path = exchange.getRequest().getURI().getPath();
      log.info("Procesando autenticación para: {}", path);

      return exchange.getSession()
          .flatMap(session -> {
            String documentNumber = (String) session.getAttributes().get("documentNumber");
            Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");

            // 1. Si es /auth/validate - validar OTP y marcar como autenticado
            if ("/auth/validate".equals(path)) {
              return handleOtpValidation(exchange, session, documentNumber, otpService);
            }

            // 2. Para endpoints protegidos - solo verificar si está autenticado
            if (authenticated != null && authenticated && documentNumber != null) {
              log.info("Usuario autenticado accediendo a: {} (documento: {})", path, documentNumber);
              return Mono.just(createAuthentication(documentNumber));
            }

            log.warn("Usuario no autenticado intentando acceder a: {}", path);
            return Mono.error(new BadCredentialsException("Authentication required"));
          })
          .switchIfEmpty(Mono.error(new BadCredentialsException("No active session")));
    };
  }

  /**
   * Maneja la validación de OTP para /auth/validate
   */
  private static Mono<Authentication> handleOtpValidation(ServerWebExchange exchange,
      org.springframework.web.server.WebSession session,
      String documentNumber,
      OtpService otpService) {
    // Obtener OTP de los parámetros de query
    String otp = exchange.getRequest().getQueryParams().getFirst("otp");

    if (documentNumber == null) {
      log.warn("No hay número de documento en la sesión para validar OTP");
      return Mono.error(new BadCredentialsException("No login session found"));
    }

    if (otp == null || otp.trim().isEmpty()) {
      log.warn("No se proporcionó OTP para validar");
      return Mono.error(new BadCredentialsException("OTP parameter is required"));
    }

    log.info("Validando OTP: {} para documento: {}", otp, documentNumber);

    return otpService.validateOtp(documentNumber, otp)
        .flatMap(isValid -> {
          if (isValid) {
            // Marcar como autenticado en la sesión
            session.getAttributes().put("authenticated", true);
            session.getAttributes().put("authTime", java.time.LocalDateTime.now());
            session.getAttributes().remove("otp"); // Limpiar OTP usado

            log.info("Autenticación OTP exitosa para documento: {}", documentNumber);
            return Mono.just(createAuthentication(documentNumber));
          } else {
            log.warn("OTP inválido para documento: {}", documentNumber);
            return Mono.error(new BadCredentialsException("Invalid or expired OTP"));
          }
        });
  }

  /**
   * Crea una autenticación exitosa
   * 
   * @param documentNumber Número de documento
   * @return Authentication
   */
  private static Authentication createAuthentication(String documentNumber) {
    return new UsernamePasswordAuthenticationToken(
        documentNumber,
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }
}

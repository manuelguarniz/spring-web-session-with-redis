package com.example.springwebsession.security;

import java.util.Collections;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;

import com.example.springwebsession.service.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class OTPAuthenticationConverter implements ServerAuthenticationConverter {

  private final OtpService otpService;
  
  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    return exchange.getSession().flatMap(session -> {
      String documentNumber = (String) session.getAttributes().get("documentNumber");
      Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
      String otp = (String) session.getAttributes().get("otp");

      // Si ya está autenticado, crear autenticación
      if (authenticated != null && authenticated && documentNumber != null) {
        log.info("Usuario ya autenticado en sesión: {} (documento: {})", session.getId(), documentNumber);
        return Mono.just(createAuthentication(documentNumber, otp));
      }

      // Si no está autenticado, verificar si hay OTP en la sesión
//      String otp = (String) session.getAttributes().get("otp"); // TODO: Eliminar
      if (documentNumber != null && otp != null) {
        log.info("Validando OTP para documento: {} con OTP: {}", documentNumber, otp);

        // Validar OTP de forma síncrona para simplificar
        try {
          boolean isValid = otpService.validateOtp(documentNumber, otp).block();
          if (isValid) {
            // Marcar como autenticado y limpiar OTP
            session.getAttributes().put("authenticated", true);
//            session.getAttributes().remove("otp"); // TODO: Revisar obligatoriedad

            log.info("Autenticación OTP exitosa para documento: {}", documentNumber);
            return Mono.just(createAuthentication(documentNumber, otp));
          } else {
            log.warn("OTP inválido para documento: {}", documentNumber);
            return Mono.error(new BadCredentialsException("Invalid OTP."));
          }
        } catch (Exception e) {
          log.error("Error validando OTP: {}", e.getMessage());
          return Mono.error(new BadCredentialsException("Invalid OTP."));
        }
      }

      log.info("No hay credenciales válidas en la sesión: {}", session.getId());
      return Mono.error(new BadCredentialsException("Invalid Session."));
    });
  }

  /**
   * Crea una autenticación exitosa
   * 
   * @param documentNumber Número de documento
   * @return Authentication
   */
  private Authentication createAuthentication(String documentNumber, String otp) {
    return new UsernamePasswordAuthenticationToken(documentNumber, otp,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }
}

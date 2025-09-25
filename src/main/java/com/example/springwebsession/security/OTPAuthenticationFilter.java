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
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.function.Function;

/**
 * Filtro de autenticación personalizado que usa número de documento y OTP como
 * credenciales de Spring Security
 */
@Slf4j
public class OTPAuthenticationFilter extends AuthenticationWebFilter {

  private final OtpService otpService;

  public OTPAuthenticationFilter(ReactiveAuthenticationManager authenticationManager, OtpService otpService) {
    super(authenticationManager);
    this.otpService = otpService;
    this.setRequiresAuthenticationMatcher(new PathPatternParserServerWebExchangeMatcher("/validate"));
  }

  protected Mono<Authentication> convert(ServerWebExchange exchange) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // TODO Auto-generated method stub
    return super.filter(exchange, chain);
  }

  /**
   * Crea el convertidor de autenticación que extrae credenciales de la sesión
   * 
   * @return ServerAuthenticationConverter
   */
  private Mono<Authentication> createAuthenticationConverter(ServerWebExchange exchange) {
    log.info("Procesando autenticación OTP para: {}", exchange.getRequest().getURI());

    return exchange.getSession().flatMap(session -> {
      String documentNumber = (String) session.getAttributes().get("documentNumber");
      Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");

      // Si ya está autenticado, crear autenticación
      if (authenticated != null && authenticated && documentNumber != null) {
        log.info("Usuario ya autenticado en sesión: {} (documento: {})", session.getId(), documentNumber);
        return Mono.just(createAuthentication(documentNumber));
      }

      // Si no está autenticado, verificar si hay OTP en la sesión
      String otp = (String) session.getAttributes().get("otp");
      if (documentNumber != null && otp != null) {
        log.info("Validando OTP para documento: {} con OTP: {}", documentNumber, otp);

        // Validar OTP de forma síncrona para simplificar
        try {
          boolean isValid = otpService.validateOtp(documentNumber, otp).block();
          if (isValid) {
            // Marcar como autenticado y limpiar OTP
            session.getAttributes().put("authenticated", true);
            session.getAttributes().remove("otp");

            log.info("Autenticación OTP exitosa para documento: {}", documentNumber);
            return Mono.just(createAuthentication(documentNumber));
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
  private Authentication createAuthentication(String documentNumber) {
    return new UsernamePasswordAuthenticationToken(documentNumber, null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }
}

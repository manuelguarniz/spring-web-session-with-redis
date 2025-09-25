package com.example.springwebsession.security;

import java.util.Collections;

import org.springframework.http.HttpMethod;
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
    return exchange.getSession().filter(session -> HttpMethod.PUT.equals(exchange.getRequest().getMethod()))
        .switchIfEmpty(Mono.error(new BadCredentialsException("Authentication methot not supported")))
        .flatMap(session -> {
          String documentNumber = (String) session.getAttributes().get("documentNumber");
          Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
          String otp = exchange.getRequest().getQueryParams().getFirst("otp");

          // Si ya está autenticado, crear autenticación
          if ((authenticated != null && authenticated && documentNumber != null)
              || (documentNumber != null && otp != null)) {
            log.info("Usuario ya autenticado en sesión: {} (documento: {})", session.getId(), documentNumber);
            return Mono.just(new UsernamePasswordAuthenticationToken(documentNumber, otp,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
          }

          log.info("No hay credenciales válidas en la sesión: {}", session.getId());
          return Mono.error(new BadCredentialsException("Invalid Session."));
        });
  }
}

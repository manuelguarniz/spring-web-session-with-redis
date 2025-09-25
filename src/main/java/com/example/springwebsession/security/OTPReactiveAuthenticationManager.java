package com.example.springwebsession.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.example.springwebsession.service.OtpService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class OTPReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  private final OtpService otpService;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    return Mono.just(authentication).flatMap(token -> {
      String documentNumber = (String) token.getPrincipal();
      String otp = (String) token.getCredentials();
      // Si el OTP es invalido se devuelve error
      return otpService.validateOtp(documentNumber, otp).filter(isAuth -> Boolean.TRUE.equals(isAuth))
          .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid OTP"))).map(isAuth -> {

            // Si el OTP es válido, creamos un token de autenticación completo
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                documentNumber, otp, token.getAuthorities());
            return authenticationToken;
          });
    });
  }

}

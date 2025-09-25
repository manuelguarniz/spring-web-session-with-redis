package com.example.springwebsession.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;

import com.example.springwebsession.service.OtpService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class OTPReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  private final OtpService otpService;
  
  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    return Mono.just(authentication)
//        .cast(OTPAuthenticationToken.class)
        .flatMap(token -> {
            String documentNumber = (String) token.getPrincipal();
            String otp = (String) token.getCredentials();
            if (otpService.validateOtp(documentNumber, otp).block()) {
                // Si el OTP es válido, creamos un token de autenticación completo
                return Mono.just(new OTPAuthenticationToken(documentNumber, otp, token.getAuthorities()));
            } else {
                return Mono.error(new BadCredentialsException("Invalid OTP"));
            }
        });
  }

}

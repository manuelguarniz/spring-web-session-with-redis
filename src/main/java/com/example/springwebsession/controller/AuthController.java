package com.example.springwebsession.controller;

import com.example.springwebsession.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para autenticación con OTP
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final OtpService otpService;

  /**
   * Endpoint de login que recibe número de documento y email
   * Genera un OTP y lo almacena en la sesión
   * 
   * @param documentNumber Número de documento de identidad
   * @param email          Correo electrónico
   * @param exchange       ServerWebExchange para manejar la sesión
   * @return Respuesta con el OTP generado
   */
  @PostMapping("/login")
  public Mono<Map<String, Object>> login(
      @RequestParam String documentNumber,
      @RequestParam String email,
      ServerWebExchange exchange) {

    log.info("Iniciando login para documento: {} y email: {}", documentNumber, email);

    return exchange.getSession()
        .flatMap(session -> {
          // Almacenar datos en la sesión
          session.getAttributes().put("documentNumber", documentNumber);
          session.getAttributes().put("email", email);
          session.getAttributes().put("loginTime", LocalDateTime.now());

          return session.save();
        })
        .then(otpService.generateOtp(documentNumber))
        .map(otp -> {
          Map<String, Object> response = new HashMap<>();
          response.put("message", "OTP generado exitosamente");
          response.put("otp", otp); // En producción, esto se enviaría por email/SMS
          response.put("documentNumber", documentNumber);
          response.put("email", email);
          response.put("expiresIn", "5 minutos");
          response.put("timestamp", LocalDateTime.now());

          log.info("Login exitoso para documento: {} - OTP: {}", documentNumber, otp);
          return response;
        });
  }

  /**
   * Endpoint para validar OTP
   * Valida el OTP con el número de documento almacenado en la sesión
   * 
   * @param otp      Código OTP a validar
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Resultado de la validación
   */
  @PostMapping("/validate")
  public Mono<Map<String, Object>> validateOtp(
      @RequestParam String otp,
      ServerWebExchange exchange) {

    log.info("Validando OTP: {}", otp);

    return exchange.getSession()
        .flatMap(session -> {
          String documentNumber = (String) session.getAttributes().get("documentNumber");

          if (documentNumber == null) {
            log.warn("No se encontró número de documento en la sesión");
            return Mono.just(createErrorResponse("No hay sesión de login activa"));
          }

          return otpService.validateOtp(documentNumber, otp)
              .map(isValid -> {
                if (isValid) {
                  // Marcar como autenticado en la sesión
                  session.getAttributes().put("authenticated", true);
                  session.getAttributes().put("authTime", LocalDateTime.now());

                  log.info("Autenticación exitosa para documento: {}", documentNumber);

                  Map<String, Object> response = new HashMap<>();
                  response.put("message", "Autenticación exitosa");
                  response.put("authenticated", true);
                  response.put("documentNumber", documentNumber);
                  response.put("timestamp", LocalDateTime.now());

                  return response;
                } else {
                  log.warn("Autenticación fallida para documento: {}", documentNumber);

                  Map<String, Object> response = new HashMap<>();
                  response.put("message", "OTP inválido o expirado");
                  response.put("authenticated", false);
                  response.put("timestamp", LocalDateTime.now());

                  return response;
                }
              });
        })
        .switchIfEmpty(Mono.fromCallable(() -> {
          log.warn("No se encontró sesión activa");
          return createErrorResponse("No hay sesión activa");
        }));
  }

  /**
   * Endpoint para verificar el estado de autenticación
   * 
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Estado de autenticación
   */
  @PostMapping("/status")
  public Mono<Map<String, Object>> getAuthStatus(ServerWebExchange exchange) {
    log.info("Verificando estado de autenticación");

    return exchange.getSession()
        .map(session -> {
          Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
          String documentNumber = (String) session.getAttributes().get("documentNumber");
          String email = (String) session.getAttributes().get("email");
          LocalDateTime loginTime = (LocalDateTime) session.getAttributes().get("loginTime");
          LocalDateTime authTime = (LocalDateTime) session.getAttributes().get("authTime");

          Map<String, Object> response = new HashMap<>();
          response.put("authenticated", authenticated != null ? authenticated : false);
          response.put("documentNumber", documentNumber);
          response.put("email", email);
          response.put("loginTime", loginTime);
          response.put("authTime", authTime);
          response.put("sessionId", session.getId());
          response.put("timestamp", LocalDateTime.now());

          log.info("Estado de autenticación: {} para documento: {}", authenticated, documentNumber);
          return response;
        })
        .switchIfEmpty(Mono.fromCallable(() -> {
          Map<String, Object> response = new HashMap<>();
          response.put("authenticated", false);
          response.put("message", "No hay sesión activa");
          response.put("timestamp", LocalDateTime.now());
          return response;
        }));
  }

  /**
   * Endpoint para cerrar sesión
   * 
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Confirmación de cierre de sesión
   */
  @PostMapping("/logout")
  public Mono<Map<String, Object>> logout(ServerWebExchange exchange) {
    log.info("Cerrando sesión");

    return exchange.getSession()
        .flatMap(session -> {
          String documentNumber = (String) session.getAttributes().get("documentNumber");
          log.info("Cerrando sesión para documento: {}", documentNumber);

          return session.invalidate();
        })
        .then(Mono.fromCallable(() -> {
          Map<String, Object> response = new HashMap<>();
          response.put("message", "Sesión cerrada exitosamente");
          response.put("timestamp", LocalDateTime.now());
          return response;
        }));
  }

  /**
   * Crea una respuesta de error
   * 
   * @param message Mensaje de error
   * @return Map con la respuesta de error
   */
  private Map<String, Object> createErrorResponse(String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("error", message);
    response.put("timestamp", LocalDateTime.now());
    return response;
  }
}

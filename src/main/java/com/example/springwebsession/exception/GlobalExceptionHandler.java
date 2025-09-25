package com.example.springwebsession.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador de excepciones global para controladores
 * Complementa el GlobalErrorHandler para casos específicos
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Maneja excepciones de autenticación
   */
  @ExceptionHandler(BadCredentialsException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleBadCredentials(BadCredentialsException ex) {
    log.warn("Error de credenciales: {}", ex.getMessage());

    Map<String, Object> errorResponse = createErrorResponse(
        "AUTHENTICATION_ERROR",
        "Credenciales inválidas",
        ex.getMessage(),
        HttpStatus.UNAUTHORIZED);

    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
  }

  /**
   * Maneja excepciones de autenticación generales
   */
  @ExceptionHandler(AuthenticationException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleAuthentication(AuthenticationException ex) {
    log.warn("Error de autenticación: {}", ex.getMessage());

    Map<String, Object> errorResponse = createErrorResponse(
        "AUTHENTICATION_ERROR",
        "Error de autenticación",
        ex.getMessage(),
        HttpStatus.UNAUTHORIZED);

    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
  }

  /**
   * Maneja excepciones de autorización
   */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleAccessDenied(
      org.springframework.security.access.AccessDeniedException ex) {
    log.warn("Acceso denegado: {}", ex.getMessage());

    Map<String, Object> errorResponse = createErrorResponse(
        "AUTHORIZATION_ERROR",
        "Acceso denegado",
        "No tiene permisos para acceder a este recurso",
        HttpStatus.FORBIDDEN);

    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse));
  }

  /**
   * Maneja excepciones de estado HTTP personalizadas
   */
  @ExceptionHandler(ResponseStatusException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleResponseStatus(ResponseStatusException ex) {
    log.warn("Error de estado HTTP: {}", ex.getMessage());

    Map<String, Object> errorResponse = createErrorResponse(
        "HTTP_ERROR",
        ex.getReason(),
        ex.getMessage(),
        HttpStatus.valueOf(ex.getStatusCode().value()));

    return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(errorResponse));
  }

  /**
   * Maneja excepciones generales
   */
  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleGeneric(Exception ex) {
    log.error("Error no manejado: {}", ex.getMessage(), ex);

    Map<String, Object> errorResponse = createErrorResponse(
        "GENERAL_ERROR",
        "Error interno del servidor",
        "Ha ocurrido un error inesperado. Por favor, intente nuevamente.",
        HttpStatus.INTERNAL_SERVER_ERROR);

    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
  }

  /**
   * Crea una respuesta de error estructurada
   * 
   * @param type    Tipo de error
   * @param title   Título del error
   * @param message Mensaje detallado
   * @param status  Código de estado HTTP
   * @return Map con la respuesta de error
   */
  private Map<String, Object> createErrorResponse(String type, String title, String message, HttpStatus status) {
    Map<String, Object> errorResponse = new HashMap<>();

    errorResponse.put("timestamp", LocalDateTime.now());
    errorResponse.put("status", status.value());
    errorResponse.put("error", status.getReasonPhrase());
    errorResponse.put("type", type);
    errorResponse.put("title", title);
    errorResponse.put("message", message);

    return errorResponse;
  }
}

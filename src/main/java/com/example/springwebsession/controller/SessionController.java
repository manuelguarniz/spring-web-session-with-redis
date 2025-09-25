package com.example.springwebsession.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
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
 * Controlador para probar funcionalidad de sesiones con Redis
 */
@Slf4j
@RestController
@RequestMapping("/api/session")
public class SessionController {

  /**
   * Obtiene información de la sesión actual
   * 
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Información de la sesión
   */
  @GetMapping("/info")
  public Mono<Map<String, Object>> getSessionInfo(ServerWebExchange exchange) {
    log.info("Obteniendo información de sesión");

    return exchange.getSession()
        .map(session -> {
          Map<String, Object> sessionInfo = new HashMap<>();
          sessionInfo.put("sessionId", session.getId());
          sessionInfo.put("creationTime", session.getCreationTime());
          sessionInfo.put("attributes", session.getAttributes());
          sessionInfo.put("currentTime", LocalDateTime.now());

          log.info("Sesión encontrada: {}", session.getId());
          return sessionInfo;
        })
        .switchIfEmpty(Mono.fromCallable(() -> {
          Map<String, Object> noSession = new HashMap<>();
          noSession.put("message", "No hay sesión activa");
          noSession.put("currentTime", LocalDateTime.now());
          log.info("No se encontró sesión activa");
          return noSession;
        }));
  }

  /**
   * Establece un atributo en la sesión
   * 
   * @param key      Clave del atributo
   * @param value    Valor del atributo
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Confirmación de la operación
   */
  @PostMapping("/set")
  public Mono<Map<String, Object>> setSessionAttribute(
      @RequestParam String key,
      @RequestParam String value,
      ServerWebExchange exchange) {

    log.info("Estableciendo atributo de sesión: {} = {}", key, value);

    return exchange.getSession()
        .flatMap(session -> {
          session.getAttributes().put(key, value);
          return session.save();
        })
        .then(Mono.fromCallable(() -> {
          Map<String, Object> response = new HashMap<>();
          response.put("message", "Atributo establecido correctamente");
          response.put("key", key);
          response.put("value", value);
          response.put("timestamp", LocalDateTime.now());
          return response;
        }));
  }

  /**
   * Obtiene un atributo de la sesión
   * 
   * @param key      Clave del atributo
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Valor del atributo
   */
  @GetMapping("/get")
  public Mono<Map<String, Object>> getSessionAttribute(
      @RequestParam String key,
      ServerWebExchange exchange) {

    log.info("Obteniendo atributo de sesión: {}", key);

    return exchange.getSession()
        .map(session -> {
          Object value = session.getAttributes().get(key);
          Map<String, Object> response = new HashMap<>();
          response.put("key", key);
          response.put("value", value);
          response.put("found", value != null);
          response.put("timestamp", LocalDateTime.now());

          log.info("Atributo obtenido: {} = {}", key, value);
          return response;
        })
        .switchIfEmpty(Mono.fromCallable(() -> {
          Map<String, Object> response = new HashMap<>();
          response.put("key", key);
          response.put("value", null);
          response.put("found", false);
          response.put("message", "No se encontró sesión activa");
          response.put("timestamp", LocalDateTime.now());
          return response;
        }));
  }

  /**
   * Elimina un atributo de la sesión
   * 
   * @param key      Clave del atributo a eliminar
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Confirmación de la operación
   */
  @PostMapping("/remove")
  public Mono<Map<String, Object>> removeSessionAttribute(
      @RequestParam String key,
      ServerWebExchange exchange) {

    log.info("Eliminando atributo de sesión: {}", key);

    return exchange.getSession()
        .flatMap(session -> {
          Object removedValue = session.getAttributes().remove(key);
          return session.save();
        })
        .then(Mono.fromCallable(() -> {
          Map<String, Object> response = new HashMap<>();
          response.put("message", "Atributo eliminado correctamente");
          response.put("key", key);
          response.put("timestamp", LocalDateTime.now());
          return response;
        }));
  }

  /**
   * Invalida la sesión actual
   * 
   * @param exchange ServerWebExchange para acceder a la sesión
   * @return Confirmación de la operación
   */
  @PostMapping("/invalidate")
  public Mono<Map<String, Object>> invalidateSession(ServerWebExchange exchange) {
    log.info("Invalidando sesión actual");

    return exchange.getSession()
        .flatMap(session -> {
          log.info("Invalidando sesión: {}", session.getId());
          return session.invalidate();
        })
        .then(Mono.fromCallable(() -> {
          Map<String, Object> response = new HashMap<>();
          response.put("message", "Sesión invalidada correctamente");
          response.put("timestamp", LocalDateTime.now());
          return response;
        }));
  }
}

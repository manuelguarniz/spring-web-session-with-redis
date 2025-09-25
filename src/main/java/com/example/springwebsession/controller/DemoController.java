package com.example.springwebsession.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controlador demo con endpoint que devuelve Hello World
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class DemoController {

  /**
   * Endpoint demo que devuelve Hello World
   * 
   * @return Mono<String> con el mensaje Hello World
   */
  @GetMapping("/hello")
  public Mono<String> hello() {
    log.info("Endpoint /api/hello llamado");
    return Mono.just("Hello World");
  }
}

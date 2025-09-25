package com.example.springwebsession.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para generar y validar códigos OTP
 */
@Slf4j
@Service
public class OtpService {

  private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
  private final SecureRandom random = new SecureRandom();
  private static final int OTP_LENGTH = 6;
  private static final int OTP_EXPIRY_MINUTES = 5;

  /**
   * Genera un código OTP para un número de documento
   * 
   * @param documentNumber Número de documento de identidad
   * @return Código OTP generado
   */
  public Mono<String> generateOtp(String documentNumber) {
    log.info("Generando OTP para documento: {}", documentNumber);

    String otp = generateRandomOtp();
    LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

    OtpData otpData = new OtpData(otp, documentNumber, expiryTime);
    otpStorage.put(documentNumber, otpData);

    log.info("OTP generado: {} para documento: {} (expira: {})", otp, documentNumber, expiryTime);

    return Mono.just(otp);
  }

  /**
   * Valida un código OTP para un número de documento
   * 
   * @param documentNumber Número de documento de identidad
   * @param otp            Código OTP a validar
   * @return true si el OTP es válido, false en caso contrario
   */
  public Mono<Boolean> validateOtp(String documentNumber, String otp) {
    log.info("Validando OTP: {} para documento: {}", otp, documentNumber);

    OtpData storedOtpData = otpStorage.get(documentNumber);

    if (storedOtpData == null) {
      log.warn("No se encontró OTP para documento: {}", documentNumber);
      return Mono.just(false);
    }

    if (LocalDateTime.now().isAfter(storedOtpData.getExpiryTime())) {
      log.warn("OTP expirado para documento: {} (expiraba: {})", documentNumber, storedOtpData.getExpiryTime());
      otpStorage.remove(documentNumber);
      return Mono.just(false);
    }

    boolean isValid = storedOtpData.getOtp().equals(otp);

    if (isValid) {
      log.info("OTP válido para documento: {}", documentNumber);
      otpStorage.remove(documentNumber); // Eliminar OTP después de uso exitoso
    } else {
      log.warn("OTP inválido para documento: {}", documentNumber);
    }

    return Mono.just(isValid);
  }

  /**
   * Obtiene información del OTP almacenado
   * 
   * @param documentNumber Número de documento
   * @return Información del OTP o null si no existe
   */
  public Mono<OtpData> getOtpInfo(String documentNumber) {
    OtpData otpData = otpStorage.get(documentNumber);
    if (otpData != null && LocalDateTime.now().isAfter(otpData.getExpiryTime())) {
      otpStorage.remove(documentNumber);
      return Mono.empty();
    }
    return Mono.justOrEmpty(otpData);
  }

  /**
   * Genera un código OTP aleatorio de 6 dígitos
   * 
   * @return Código OTP
   */
  private String generateRandomOtp() {
    int otp = 100000 + random.nextInt(900000);
    return String.valueOf(otp);
  }

  /**
   * Clase interna para almacenar datos del OTP
   */
  public static class OtpData {
    private final String otp;
    private final String documentNumber;
    private final LocalDateTime expiryTime;

    public OtpData(String otp, String documentNumber, LocalDateTime expiryTime) {
      this.otp = otp;
      this.documentNumber = documentNumber;
      this.expiryTime = expiryTime;
    }

    public String getOtp() {
      return otp;
    }

    public String getDocumentNumber() {
      return documentNumber;
    }

    public LocalDateTime getExpiryTime() {
      return expiryTime;
    }

    @Override
    public String toString() {
      return "OtpData{" +
          "otp='" + otp + '\'' +
          ", documentNumber='" + documentNumber + '\'' +
          ", expiryTime=" + expiryTime +
          '}';
    }
  }
}

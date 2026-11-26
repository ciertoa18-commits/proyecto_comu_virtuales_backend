package pe.perueduca.comunidades.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ClaveSeguraTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private boolean valida(String clave) {
    return validator.validate(new CuentaRequests.ResetPassword(clave)).isEmpty();
  }

  @Test
  void aceptaClaveConMayusculaMinusculaNumeroYSimbolo() {
    assertThat(valida("ClaveSegura2026!")).isTrue();
    assertThat(valida("Comunidad#2026pe")).isTrue();
  }

  @Test
  void rechazaClaveSinAlgunTipoDeCaracter() {
    assertThat(valida("clavesegura2026!")).as("sin mayúscula").isFalse();
    assertThat(valida("CLAVESEGURA2026!")).as("sin minúscula").isFalse();
    assertThat(valida("ClaveSeguraSinNum!")).as("sin número").isFalse();
    assertThat(valida("ClaveSegura2026")).as("sin símbolo").isFalse();
  }

  @Test
  void rechazaClaveCorta() {
    assertThat(valida("Ab1!")).isFalse();
  }
}

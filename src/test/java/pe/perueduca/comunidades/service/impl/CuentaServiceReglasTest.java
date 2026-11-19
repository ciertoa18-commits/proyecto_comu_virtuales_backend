package pe.perueduca.comunidades.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.exception.ApiException;

class CuentaServiceReglasTest {

  @Test
  void estudianteConCorreoPerueducaEsValido() {
    assertDoesNotThrow(
        () -> CuentaService.validarDominio("ana@perueduca.pe", RolCodigo.ESTUDIANTE));
  }

  @Test
  void estudianteConGmailEsRechazado() {
    ApiException e =
        assertThrows(
            ApiException.class,
            () -> CuentaService.validarDominio("ana@gmail.com", RolCodigo.ESTUDIANTE));
    assertTrue(e.getMessage().contains("@perueduca.pe"));
  }

  @Test
  void docenteDebeUsarCorreoMinedu() {
    assertDoesNotThrow(
        () -> CuentaService.validarDominio("jcampos@minedu.gob.pe", RolCodigo.DOCENTE));
    assertThrows(
        ApiException.class,
        () -> CuentaService.validarDominio("jcampos@perueduca.pe", RolCodigo.DOCENTE));
  }

  @Test
  void normalizarQuitaEspaciosYPasaAMinusculas() {
    assertEquals("ana@perueduca.pe", CuentaService.normalizar("  Ana@PeruEduca.pe "));
  }

  @Test
  void blancoANuloConvierteTextoVacioEnNull() {
    assertNull(CuentaService.blancoANulo("   "));
    assertNull(CuentaService.blancoANulo(null));
    assertEquals("987654321", CuentaService.blancoANulo(" 987654321 "));
  }

  @Test
  void contrasenaDeMasDe72BytesEsRechazada() {
    assertThrows(ApiException.class, () -> CuentaService.validarBytes("ñ".repeat(40)));
    assertDoesNotThrow(() -> CuentaService.validarBytes("Comunidades2026!"));
  }
}

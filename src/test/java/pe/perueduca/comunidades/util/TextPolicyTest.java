package pe.perueduca.comunidades.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pe.perueduca.comunidades.exception.ApiException;

class TextPolicyTest {

  private final TextPolicy text = new TextPolicy("tonto,feo");

  @Test
  void cleanDevuelveNullSiElTextoEsNuloOVacio() {
    assertNull(text.clean(null));
    assertNull(text.clean("   "));
  }

  @Test
  void cleanQuitaEspaciosAlInicioYAlFinal() {
    assertEquals("Saberes andinos", text.clean("  Saberes andinos  "));
  }

  @Test
  void cleanRechazaEtiquetasHtml() {
    assertThrows(ApiException.class, () -> text.clean("<script>alert(1)</script>"));
  }

  @Test
  void cleanRechazaPalabrasBloqueadas() {
    assertThrows(ApiException.class, () -> text.clean("Eres muy tonto"));
  }

  @Test
  void cleanNoBloqueaPalabrasQueSoloContienenLaPalabra() {
    assertEquals("Fernando", text.clean("Fernando"));
  }

  @Test
  void requiredLanzaErrorSiElTextoEstaVacio() {
    ApiException e = assertThrows(ApiException.class, () -> text.required(" "));
    assertEquals("El contenido no puede estar vacío", e.getMessage());
  }

  @Test
  void urlAceptaHttpYHttps() {
    assertDoesNotThrow(() -> text.url("https://meet.google.com/abc-defg-hij"));
    assertDoesNotThrow(() -> text.url("http://zoom.us/j/123"));
    assertDoesNotThrow(() -> text.url(null));
  }

  @Test
  void urlRechazaOtrosProtocolos() {
    assertThrows(ApiException.class, () -> text.url("ftp://servidor/archivo"));
    assertThrows(ApiException.class, () -> text.url("javascript:alert(1)"));
  }
}

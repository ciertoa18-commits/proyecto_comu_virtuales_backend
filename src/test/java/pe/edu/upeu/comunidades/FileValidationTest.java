package pe.edu.upeu.comunidades;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.comunidades.exception.ApiException;
import pe.edu.upeu.comunidades.service.impl.ArchivoService;

class FileValidationTest {
  @Test
  void rejectsExecutableRenamedPdf() {
    assertThrows(
        ApiException.class, () -> ArchivoService.validar("pdf", new byte[] {77, 90, 0, 0}, false));
  }

  @Test
  void rejectsEmptyAndOversize() {
    assertThrows(ApiException.class, () -> ArchivoService.validar("pdf", new byte[0], false));
    assertThrows(ApiException.class, () -> ArchivoService.validar("jpg", new byte[2097153], true));
  }

  @Test
  void rejectsZipRenamedDocx() {
    assertThrows(
        ApiException.class, () -> ArchivoService.validar("docx", new byte[] {80, 75, 3, 4}, false));
  }

  @Test
  void rejectsDocumentAsProfilePhoto() {
    assertThrows(
        ApiException.class, () -> ArchivoService.validar("pdf", "%PDF-1.7\n".getBytes(), true));
  }

  @Test
  void acceptsPdfSignature() {
    assertDoesNotThrow(() -> ArchivoService.validar("pdf", "%PDF-1.7\n".getBytes(), false));
  }
}

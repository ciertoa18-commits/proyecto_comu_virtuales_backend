package pe.perueduca.comunidades.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.Comunidad;
import pe.perueduca.comunidades.enums.ComunidadEstado;
import pe.perueduca.comunidades.enums.ComunidadVisibilidad;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.util.TextPolicy;

@ExtendWith(MockitoExtension.class)
class ComunidadServiceImplTest {

  @Mock ComunidadRepository repo;
  @Mock MiembroComunidadRepository miembros;
  @Mock SolicitudIngresoRepository solicitudes;
  @Mock AccessService access;
  @Mock NotificationService notifications;
  @Spy TextPolicy text = new TextPolicy("");
  @Mock CategoriaRepository categorias;
  @Mock IdiomaRepository idiomas;
  @Mock PuebloOriginarioRepository pueblos;

  @InjectMocks ComunidadServiceImpl service;

  private Requests.ComunidadDatos datos(String nombre, Long categoria) {
    return new Requests.ComunidadDatos(
        nombre, "Comunidad de prueba", categoria, null, null,
        ComunidadVisibilidad.PUBLICA, false, null);
  }

  @Test
  void guardarConCategoriaInexistenteLanzaErrorYNoGuarda() {
    when(categorias.existsById(99L)).thenReturn(false);

    ApiException e =
        assertThrows(ApiException.class, () -> service.guardar(null, datos("Saberes", 99L)));

    assertEquals(HttpStatus.NOT_FOUND, e.status);
    verify(repo, never()).save(any());
  }

  @Test
  void editarComunidadActualizaLosDatosYLaFecha() {
    Comunidad existente = new Comunidad();
    existente.setNombre("Nombre antiguo");
    existente.setEstado(ComunidadEstado.ACTIVA);
    when(categorias.existsById(1L)).thenReturn(true);
    when(access.comunidad(5L)).thenReturn(existente);
    when(repo.save(any(Comunidad.class))).thenAnswer(i -> i.getArgument(0));

    Comunidad r = service.guardar(5L, datos("  Saberes andinos  ", 1L));

    assertEquals("Saberes andinos", r.getNombre());
    assertEquals(ComunidadVisibilidad.PUBLICA, r.getVisibilidad());
    assertNotNull(r.getFechaActualizacion());
    verify(repo).save(existente);
  }

  @Test
  void guardarConNombreVacioLanzaError() {
    Comunidad existente = new Comunidad();
    when(categorias.existsById(1L)).thenReturn(true);
    when(access.comunidad(5L)).thenReturn(existente);

    assertThrows(ApiException.class, () -> service.guardar(5L, datos("   ", 1L)));
    verify(repo, never()).save(any());
  }

  @Test
  void desactivarCambiaElEstadoAInactivaSinBorrar() {
    Comunidad c = new Comunidad();
    c.setEstado(ComunidadEstado.ACTIVA);
    when(access.comunidad(1L)).thenReturn(c);

    service.desactivar(1L);

    assertEquals(ComunidadEstado.INACTIVA, c.getEstado());
    verify(repo, never()).delete(any(Comunidad.class));
  }
}

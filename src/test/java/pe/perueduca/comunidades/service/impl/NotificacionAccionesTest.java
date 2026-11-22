package pe.perueduca.comunidades.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.perueduca.comunidades.entity.Notificacion;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.NotificacionRepository;
import pe.perueduca.comunidades.security.Actor;

@ExtendWith(MockitoExtension.class)
class NotificacionAccionesTest {

  @Mock NotificacionRepository repo;
  @InjectMocks NotificationService notificaciones;

  @BeforeEach
  void sesion() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(new Actor(1L, "ana@perueduca.pe", "ESTUDIANTE"), null));
  }

  @AfterEach
  void limpiar() {
    SecurityContextHolder.clearContext();
  }

  private Notificacion de(long destinatario) {
    Notificacion n = new Notificacion();
    n.setIdNotificacion(5L);
    n.setIdDestinatario(destinatario);
    n.setLeida(true);
    n.setFechaLectura(LocalDateTime.now());
    when(repo.findById(5L)).thenReturn(Optional.of(n));
    return n;
  }

  @Test
  void unaNotificacionLeidaSigueLeidaAlVolverAMarcarla() {
    Notificacion n = de(1L);
    LocalDateTime primera = n.getFechaLectura();
    notificaciones.read(5L);
    assertTrue(n.getLeida());
    assertEquals(primera, n.getFechaLectura());
  }

  @Test
  void elDestinatarioPuedeEliminarSuNotificacion() {
    Notificacion n = de(1L);
    notificaciones.delete(5L);
    verify(repo).delete(n);
  }

  @Test
  void nadiePuedeEliminarNiLeerNotificacionesDeOtro() {
    Notificacion n = de(2L);
    assertThrows(ApiException.class, () -> notificaciones.delete(5L));
    assertThrows(ApiException.class, () -> notificaciones.read(5L));
    verify(repo, never()).delete(any(Notificacion.class));
    assertTrue(n.getLeida());
  }
}

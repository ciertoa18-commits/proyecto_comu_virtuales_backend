package pe.perueduca.comunidades.realtime;

import jakarta.persistence.PostPersist;
import org.springframework.stereotype.Component;
import pe.perueduca.comunidades.entity.Notificacion;

@Component
@lombok.RequiredArgsConstructor
public class NotificacionListener {
  private final TiempoRealService tiempoReal;

  @PostPersist
  public void creada(Notificacion n) {
    if (n.getIdDestinatario() != null) tiempoReal.avisar(n.getIdDestinatario(), "NOTIFICACION");
  }
}

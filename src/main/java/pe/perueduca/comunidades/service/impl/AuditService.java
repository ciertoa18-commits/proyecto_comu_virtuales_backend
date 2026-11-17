package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.AuditoriaAccion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.repository.AuditoriaRepository;
import pe.perueduca.comunidades.security.Actor;

@Service
@lombok.RequiredArgsConstructor
public class AuditService {
  private final AuditoriaRepository repo;

  @Value("${app.db.triggers-activos:false}")
  private boolean triggers;

  public void record(String table, Long id, AuditoriaAccion action, String before, String after) {
    if (triggers && java.util.Set.of("NOTA", "PUBLICACION", "COMENTARIO").contains(table)) return;
    Auditoria a = new Auditoria();
    a.setTabla(table);
    a.setIdRegistro(id);
    a.setAccion(action);
    a.setIdUsuario(Actor.actual().id());
    a.setFecha(java.time.LocalDateTime.now());
    a.setValoresAntes(before);
    a.setValoresDespues(after);
    repo.save(a);
  }
}

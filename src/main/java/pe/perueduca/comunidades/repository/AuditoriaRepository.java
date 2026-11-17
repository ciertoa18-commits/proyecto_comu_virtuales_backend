package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface AuditoriaRepository
    extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
  java.util.List<Auditoria> findByIdRegistro(Long id);

  java.util.List<Auditoria> findByIdUsuario(Long id);
}

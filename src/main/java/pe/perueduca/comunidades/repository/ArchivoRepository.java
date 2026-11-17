package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface ArchivoRepository
    extends JpaRepository<Archivo, Long>, JpaSpecificationExecutor<Archivo> {
  java.util.List<Archivo> findByIdSubidoPor(Long id);
}

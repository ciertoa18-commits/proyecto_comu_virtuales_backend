package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface ArchivoRepository
    extends JpaRepository<Archivo, Long>, JpaSpecificationExecutor<Archivo> {
  java.util.List<Archivo> findByIdSubidoPor(Long id);
}

package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface MatriculaRepository
    extends JpaRepository<Matricula, Long>, JpaSpecificationExecutor<Matricula> {
  java.util.List<Matricula> findByIdSeccion(Long id);

  java.util.List<Matricula> findByIdUsuario(Long id);
}

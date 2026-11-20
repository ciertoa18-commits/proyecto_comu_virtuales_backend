package pe.perueduca.comunidades.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.perueduca.comunidades.entity.Silabo;

public interface SilaboRepository extends JpaRepository<Silabo, Long> {
  Optional<Silabo> findByIdSeccion(Long idSeccion);

  boolean existsByIdSeccion(Long idSeccion);
}

package pe.perueduca.comunidades.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import pe.perueduca.comunidades.entity.ComunidadIdioma;
import pe.perueduca.comunidades.entity.ComunidadIdiomaId;

public interface ComunidadIdiomaRepository
    extends JpaRepository<ComunidadIdioma, ComunidadIdiomaId> {
  List<ComunidadIdioma> findByIdComunidad(Long idComunidad);

  boolean existsByIdIdioma(Long idIdioma);

  @Modifying
  void deleteByIdComunidad(Long idComunidad);
}

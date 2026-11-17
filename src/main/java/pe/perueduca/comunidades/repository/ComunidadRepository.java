package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface ComunidadRepository
    extends JpaRepository<Comunidad, Long>, JpaSpecificationExecutor<Comunidad> {
  java.util.List<Comunidad> findByIdCategoria(Long id);

  java.util.List<Comunidad> findByIdIdioma(Long id);

  java.util.List<Comunidad> findByIdPueblo(Long id);

  java.util.List<Comunidad> findByIdImagen(Long id);

  java.util.List<Comunidad> findByIdCreador(Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select e from Comunidad e where e.idComunidad = :id")
  java.util.Optional<Comunidad> bloquear(
      @org.springframework.data.repository.query.Param("id") Long id);
}

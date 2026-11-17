package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.perueduca.comunidades.entity.CodigoRecuperacion;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {
  java.util.List<CodigoRecuperacion> findByIdUsuarioAndUsadoOrderByFechaCreacionDesc(
      Long idUsuario, Boolean usado);
}

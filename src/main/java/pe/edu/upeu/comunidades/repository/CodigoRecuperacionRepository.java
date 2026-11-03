package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.comunidades.entity.CodigoRecuperacion;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {
  java.util.List<CodigoRecuperacion> findByIdUsuarioAndUsadoOrderByFechaCreacionDesc(
      Long idUsuario, Boolean usado);
}

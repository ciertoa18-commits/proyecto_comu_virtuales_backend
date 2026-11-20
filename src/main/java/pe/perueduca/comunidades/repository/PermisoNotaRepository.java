package pe.perueduca.comunidades.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.perueduca.comunidades.entity.PermisoNota;

public interface PermisoNotaRepository extends JpaRepository<PermisoNota, Long> {
  List<PermisoNota> findByIdSeccionAndIdDocenteOrderByFechaSolicitudDesc(Long idSeccion, Long idDocente);

  List<PermisoNota> findByEstadoOrderByFechaSolicitudDesc(String estado);
}

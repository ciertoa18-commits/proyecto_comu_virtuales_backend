package pe.perueduca.comunidades.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import pe.perueduca.comunidades.entity.UsuarioIdioma;
import pe.perueduca.comunidades.entity.UsuarioIdiomaId;

public interface UsuarioIdiomaRepository extends JpaRepository<UsuarioIdioma, UsuarioIdiomaId> {
  List<UsuarioIdioma> findByIdUsuario(Long idUsuario);

  @Modifying
  void deleteByIdUsuario(Long idUsuario);
}

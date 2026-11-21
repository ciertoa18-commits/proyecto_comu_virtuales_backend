package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface UsuarioRepository
    extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
  java.util.List<Usuario> findByIdRol(Long id);

  java.util.List<Usuario> findByIdFotoPerfil(Long id);

  java.util.List<Usuario> findByIdIdiomaPref(Long id);

  java.util.Optional<Usuario> findByCorreo(String correo);

  java.util.List<Usuario> findByCorreoRecuperacion(String correoRecuperacion);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.correo = :correo")
  java.util.Optional<Usuario> bloquearPorCorreo(
      @org.springframework.data.repository.query.Param("correo") String correo);
}

package pe.edu.upeu.comunidades.demo;

import java.time.LocalDateTime;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.upeu.comunidades.ComunidadesApplication;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.repository.*;

/** Demostración aislada: solo vive en el classpath de pruebas; no se incluye en el JAR productivo. */
@Configuration
@Profile("demo")
public class DemoApplication {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(ComunidadesApplication.class);
    app.setAdditionalProfiles("demo");
    app.run(args);
  }

  @Bean
  ApplicationRunner datosDemo(RolRepository roles, UsuarioRepository usuarios,
      CategoriaRepository categorias, ComunidadRepository comunidades,
      MiembroComunidadRepository miembros, PasswordEncoder encoder) {
    return args -> {
      String password = System.getenv("DEMO_PASSWORD");
      if (password == null || password.length() < 12) {
        throw new IllegalStateException("Defina DEMO_PASSWORD con al menos 12 caracteres");
      }
      Usuario admin = null;
      for (RolCodigo codigo : RolCodigo.values()) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setNombre(codigo.name());
        rol = roles.save(rol);
        Usuario usuario = new Usuario();
        usuario.setIdRol(rol.getIdRol());
        usuario.setCorreo(codigo.name().toLowerCase(java.util.Locale.ROOT) + "@perueduca.pe");
        usuario.setNombres(codigo.name());
        usuario.setApellidos("Demostración local");
        usuario.setPasswordHash(encoder.encode(password));
        usuario.setIntentosFallidos(0L);
        usuario.setEstado(UsuarioEstado.ACTIVO);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario = usuarios.save(usuario);
        if (codigo == RolCodigo.ADMIN) admin = usuario;
      }
      Categoria categoria = new Categoria();
      categoria.setNombre("Educación intercultural");
      categoria = categorias.save(categoria);
      Comunidad comunidad = new Comunidad();
      comunidad.setNombre("Saberes de nuestras comunidades");
      comunidad.setDescripcion("Datos de demostración. Se reinician al detener la aplicación.");
      comunidad.setIdCategoria(categoria.getIdCategoria());
      comunidad.setIdCreador(admin.getIdUsuario());
      comunidad.setVisibilidad(ComunidadVisibilidad.PUBLICA);
      comunidad.setModeracionPrevia(false);
      comunidad.setEstado(ComunidadEstado.ACTIVA);
      comunidad.setFechaCreacion(LocalDateTime.now());
      comunidad = comunidades.save(comunidad);
      for (Usuario usuario : usuarios.findAll()) {
        MiembroComunidad miembro = new MiembroComunidad();
        miembro.setIdComunidad(comunidad.getIdComunidad());
        miembro.setIdUsuario(usuario.getIdUsuario());
        miembro.setRolComunidad(usuario.getIdUsuario().equals(admin.getIdUsuario())
            ? MiembroComunidadRolComunidad.ADMINISTRADOR : MiembroComunidadRolComunidad.MIEMBRO);
        miembro.setEstado(MiembroComunidadEstado.ACTIVO);
        miembro.setFechaIngreso(LocalDateTime.now());
        miembros.save(miembro);
      }
    };
  }
}

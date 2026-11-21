package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.util.TextPolicy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.dto.CuentaRequests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;

@Service
@Transactional
@lombok.RequiredArgsConstructor
public class CuentaService {
  private static final int MINUTOS_VIGENCIA = 15;
  private static final int SEGUNDOS_ENTRE_CODIGOS = 60;
  private final SecureRandom random = new SecureRandom();

  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final IdiomaRepository idiomas;
  private final CodigoRecuperacionRepository codigos;
  private final NotificacionRepository notificaciones;
  private final PasswordEncoder encoder;
  private final TextPolicy text;
  private final CorreoService correo;

  public record Resultado(String estado, String mensaje) {}

  public Resultado registrar(CuentaRequests.Registro d) {
    if (d.rol() == RolCodigo.ADMIN)
      throw ApiException.denied();
    String mail = normalizar(d.correo());
    validarDominio(mail, d.rol());
    if (usuarios.findByCorreo(mail).isPresent())
      throw ApiException.conflict("Ya existe una cuenta con ese correo");
    if (d.idIdiomaPref() != null && !idiomas.existsById(d.idIdiomaPref()))
      throw ApiException.missing();
    validarBytes(d.password());

    Usuario u = new Usuario();
    u.setIdRol(idRol(d.rol()));
    u.setCorreo(mail);
    u.setNombres(text.required(d.nombres()));
    u.setApellidos(text.required(d.apellidos()));
    u.setEspecialidad(d.rol() == RolCodigo.DOCENTE ? text.clean(d.especialidad()) : null);
    u.setTelefono(blancoANulo(d.telefono()));
    u.setCorreoRecuperacion(normalizar(d.correoRecuperacion()));
    u.setIdIdiomaPref(d.idIdiomaPref() != null ? d.idIdiomaPref() : 1L);
    u.setPasswordHash(encoder.encode(d.password()));
    u.setIntentosFallidos(0L);
    u.setFechaRegistro(LocalDateTime.now());
    boolean docente = d.rol() == RolCodigo.DOCENTE;
    u.setEstado(docente ? UsuarioEstado.PENDIENTE : UsuarioEstado.ACTIVO);
    usuarios.save(u);

    if (docente) {
      avisarAdmins(u);
      return new Resultado(
          "PENDIENTE",
          "Tu cuenta de docente fue creada. Un administrador debe aprobarla antes de que puedas"
              + " ingresar.");
    }
    return new Resultado("ACTIVO", "Tu cuenta fue creada. Ya puedes iniciar sesión.");
  }

  private void avisarAdmins(Usuario docente) {
    Long admin = idRol(RolCodigo.ADMIN);
    usuarios.findByIdRol(admin).stream()
        .filter(a -> a.getEstado() == UsuarioEstado.ACTIVO)
        .forEach(
            a -> {
              Notificacion n = new Notificacion();
              n.setIdDestinatario(a.getIdUsuario());
              n.setTipo(NotificacionTipo.SISTEMA);
              n.setTitulo("Nuevo docente pendiente de aprobación");
              n.setContenido(
                  docente.getNombres() + " " + docente.getApellidos() + " (" + docente.getCorreo()
                      + ")");
              n.setEntidadTipo("USUARIO");
              n.setEntidadId(docente.getIdUsuario());
              n.setLeida(false);
              n.setFechaCreacion(LocalDateTime.now());
              notificaciones.save(n);
            });
  }

  public Resultado solicitarCodigo(CuentaRequests.Recuperacion d) {
    Resultado generico =
        new Resultado(
            "ENVIADO",
            "Si el correo está registrado, te enviamos un código de 6 dígitos a tu Gmail personal."
                + " Vence en "
                + MINUTOS_VIGENCIA
                + " minutos.");
    Optional<Usuario> ou = buscarParaRecuperar(d.correo());
    if (ou.isEmpty() || ou.get().getEstado() == UsuarioEstado.INACTIVO) return generico;
    Usuario u = ou.get();
    String codigo = nuevoCodigo(u);
    if (codigo == null) return generico;

    String destino =
        u.getCorreoRecuperacion() != null && !u.getCorreoRecuperacion().isBlank()
            ? u.getCorreoRecuperacion()
            : u.getCorreo();
    correo.enviar(
        destino,
        "Código para restablecer tu contraseña - Comunidades Virtuales",
        "Hola "
            + u.getNombres()
            + ",\n\nTu código para restablecer la contraseña es: "
            + codigo
            + "\n\nVence en "
            + MINUTOS_VIGENCIA
            + " minutos y solo se puede usar una vez.\nSi no lo pediste, ignora este correo."
            + "\n\nComunidades Virtuales - PerúEduca");
    return generico;
  }

  @Transactional(noRollbackFor = ApiException.class)
  public Resultado restablecer(CuentaRequests.Restablecer d) {
    ApiException invalido = ApiException.rule("El código no es válido o ya venció");
    Usuario u =
        buscarParaRecuperar(d.correo())
            .flatMap(x -> usuarios.bloquearPorCorreo(x.getCorreo()))
            .orElseThrow(() -> invalido);
    LocalDateTime ahora = LocalDateTime.now();
    validarBytes(d.nueva());
    usarCodigo(u, d.codigo(), invalido);
    u.setPasswordHash(encoder.encode(d.nueva()));
    u.setIntentosFallidos(0L);
    u.setBloqueadoHasta(null);
    u.setFechaActualizacion(ahora);
    return new Resultado("OK", "Tu contraseña fue cambiada. Ya puedes iniciar sesión.");
  }

  Optional<Usuario> buscarParaRecuperar(String correo) {
    String c = normalizar(correo);
    Optional<Usuario> institucional = usuarios.findByCorreo(c);
    if (institucional.isPresent()) return institucional;
    List<Usuario> personales =
        usuarios.findByCorreoRecuperacion(c).stream()
            .filter(u -> u.getEstado() != UsuarioEstado.INACTIVO)
            .toList();
    return personales.size() == 1 ? Optional.of(personales.get(0)) : Optional.empty();
  }

  String nuevoCodigo(Usuario u) {
    LocalDateTime ahora = LocalDateTime.now();
    List<CodigoRecuperacion> previos =
        codigos.findByIdUsuarioAndUsadoOrderByFechaCreacionDesc(u.getIdUsuario(), false);
    if (!previos.isEmpty()
        && previos.get(0).getFechaCreacion().isAfter(ahora.minusSeconds(SEGUNDOS_ENTRE_CODIGOS)))
      return null;
    previos.forEach(c -> c.setUsado(true));
    String codigo = String.format("%06d", random.nextInt(1_000_000));
    CodigoRecuperacion c = new CodigoRecuperacion();
    c.setIdUsuario(u.getIdUsuario());
    c.setCodigoHash(encoder.encode(codigo));
    c.setIntentos(0L);
    c.setUsado(false);
    c.setFechaCreacion(ahora);
    c.setFechaExpiracion(ahora.plusMinutes(MINUTOS_VIGENCIA));
    codigos.save(c);
    return codigo;
  }

  void usarCodigo(Usuario u, String codigo, ApiException invalido) {
    LocalDateTime ahora = LocalDateTime.now();
    CodigoRecuperacion c =
        codigos.findByIdUsuarioAndUsadoOrderByFechaCreacionDesc(u.getIdUsuario(), false).stream()
            .findFirst()
            .filter(x -> x.vigente(ahora))
            .orElseThrow(() -> invalido);
    if (!encoder.matches(codigo, c.getCodigoHash())) {
      c.setIntentos(c.getIntentos() + 1);
      if (c.getIntentos() >= 5) c.setUsado(true);
      throw invalido;
    }
    c.setUsado(true);
  }

  static int minutosVigencia() {
    return MINUTOS_VIGENCIA;
  }

  Long idRol(RolCodigo codigo) {
    return roles.findAll().stream()
        .filter(r -> r.getCodigo() == codigo)
        .findFirst()
        .orElseThrow(ApiException::missing)
        .getIdRol();
  }

  static void validarDominio(String correo, RolCodigo rol) {
    String dominio = CuentaRequests.dominioDe(rol);
    if (!correo.endsWith(dominio))
      throw ApiException.rule(
          (rol == RolCodigo.ESTUDIANTE ? "Los estudiantes" : "Los docentes y administradores")
              + " deben usar un correo " + dominio);
  }

  static String normalizar(String correo) {
    return correo.trim().toLowerCase(Locale.ROOT);
  }

  static String blancoANulo(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  static void validarBytes(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw ApiException.rule("La contraseña admite hasta 72 bytes");
  }
}

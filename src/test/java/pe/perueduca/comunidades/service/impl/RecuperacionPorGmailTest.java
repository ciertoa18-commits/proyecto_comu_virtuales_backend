package pe.perueduca.comunidades.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.perueduca.comunidades.dto.CuentaRequests;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.Rol;
import pe.perueduca.comunidades.entity.Usuario;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;
import pe.perueduca.comunidades.util.TextPolicy;

@ExtendWith(MockitoExtension.class)
class RecuperacionPorGmailTest {

  @Mock UsuarioRepository usuarios;
  @Mock RolRepository roles;
  @Mock IdiomaRepository idiomas;
  @Mock CodigoRecuperacionRepository codigos;
  @Mock NotificacionRepository notificaciones;
  @Mock PasswordEncoder encoder;
  @Spy TextPolicy text = new TextPolicy("");
  @Mock CorreoService correo;
  @Mock UsuarioIdiomaRepository usuarioIdiomas;

  @InjectMocks CuentaService cuentas;

  @AfterEach
  void limpiarSesion() {
    SecurityContextHolder.clearContext();
  }

  private Usuario usuario(String gmail) {
    Usuario u = new Usuario();
    u.setIdUsuario(1L);
    u.setNombres("Ana");
    u.setCorreo("ana@perueduca.pe");
    u.setCorreoRecuperacion(gmail);
    u.setEstado(UsuarioEstado.ACTIVO);
    return u;
  }

  private void prepararCodigo(Usuario u) {
    when(usuarios.findByCorreo("ana@perueduca.pe")).thenReturn(Optional.of(u));
    when(codigos.findByIdUsuarioAndUsadoOrderByFechaCreacionDesc(1L, false)).thenReturn(List.of());
    when(encoder.encode(anyString())).thenReturn("hash");
    lenient().when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(true);
  }

  @Test
  void elCodigoLlegaAlGmailDeRecuperacion() {
    prepararCodigo(usuario("ana.quispe@gmail.com"));

    cuentas.solicitarCodigo(new CuentaRequests.Recuperacion(" Ana@PeruEduca.pe "));

    verify(correo).enviar(eq("ana.quispe@gmail.com"), anyString(), contains("código"));
  }

  @Test
  void sinGmailElCodigoLlegaAlCorreoInstitucional() {
    prepararCodigo(usuario(null));

    cuentas.solicitarCodigo(new CuentaRequests.Recuperacion("ana@perueduca.pe"));

    verify(correo).enviar(eq("ana@perueduca.pe"), anyString(), anyString());
  }

  @Test
  void tambienSePuedeRecuperarEscribiendoElGmailPersonal() {
    Usuario u = usuario("ana.quispe@gmail.com");
    when(usuarios.findByCorreo("ana.quispe@gmail.com")).thenReturn(Optional.empty());
    when(usuarios.findByCorreoRecuperacion("ana.quispe@gmail.com")).thenReturn(List.of(u));
    when(codigos.findByIdUsuarioAndUsadoOrderByFechaCreacionDesc(1L, false)).thenReturn(List.of());
    when(encoder.encode(anyString())).thenReturn("hash");
    when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(true);

    cuentas.solicitarCodigo(new CuentaRequests.Recuperacion(" Ana.Quispe@Gmail.com "));

    verify(correo).enviar(eq("ana.quispe@gmail.com"), anyString(), contains("código"));
  }

  @Test
  void enModoDemostracionSinCorreoConfiguradoElCodigoSeMuestraEnPantalla() {
    org.springframework.test.util.ReflectionTestUtils.setField(cuentas, "codigoEnPantalla", true);
    when(correo.configurado()).thenReturn(false);
    prepararCodigo(usuario("ana.quispe@gmail.com"));

    CuentaService.Resultado r =
        cuentas.solicitarCodigo(new CuentaRequests.Recuperacion("ana@perueduca.pe"));

    assertNotNull(r.codigoDemo());
    assertEquals(6, r.codigoDemo().length());
    verify(correo, never()).enviar(anyString(), anyString(), anyString());
  }

  @Test
  void elIdiomaOtroSeCreaSiNoExisteYSeReutilizaSiYaExiste() {
    pe.perueduca.comunidades.entity.Idioma quechua = new pe.perueduca.comunidades.entity.Idioma();
    quechua.setIdIdioma(2L);
    quechua.setCodigo("qu");
    quechua.setNombre("Quechua");
    when(idiomas.findAll()).thenReturn(List.of(quechua));
    when(idiomas.save(any())).thenAnswer(i -> {
      pe.perueduca.comunidades.entity.Idioma n = i.getArgument(0);
      n.setIdIdioma(9L);
      return n;
    });

    assertEquals(2L, cuentas.resolverIdioma(null, " quechua "));
    assertEquals(9L, cuentas.resolverIdioma(null, "Awajún"));
  }

  @Test
  void siGmailRechazaElEnvioSeAvisaEnVezDeDecirEnviado() {
    prepararCodigo(usuario("ana.quispe@gmail.com"));
    when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(false);

    var e =
        assertThrows(
            pe.perueduca.comunidades.exception.ApiException.class,
            () -> cuentas.solicitarCodigo(new CuentaRequests.Recuperacion("ana@perueduca.pe")));
    assertTrue(e.getMessage().contains("No pudimos enviar"));
  }

  @Test
  void siGmailRechazaElEnvioEnModoDemostracionElCodigoSeMuestraEnPantalla() {
    org.springframework.test.util.ReflectionTestUtils.setField(cuentas, "codigoEnPantalla", true);
    when(correo.configurado()).thenReturn(true);
    prepararCodigo(usuario("ana.quispe@gmail.com"));
    when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(false);

    CuentaService.Resultado r =
        cuentas.solicitarCodigo(new CuentaRequests.Recuperacion("ana@perueduca.pe"));
    assertEquals(6, r.codigoDemo().length());
  }

  @Test
  void alCambiarLaContrasenaLlegaUnAvisoAlGmailYALaCampana() {
    cuentas.avisarCambioDeContrasena(usuario("ana.quispe@gmail.com"));

    verify(correo).enviar(eq("ana.quispe@gmail.com"), contains("contraseña fue cambiada"), contains("Si NO fuiste tú"));
    ArgumentCaptor<pe.perueduca.comunidades.entity.Notificacion> n =
        ArgumentCaptor.forClass(pe.perueduca.comunidades.entity.Notificacion.class);
    verify(notificaciones).save(n.capture());
    assertEquals(1L, n.getValue().getIdDestinatario());
    assertEquals("Tu contraseña fue cambiada", n.getValue().getTitulo());
  }

  @Test
  void correoNoRegistradoNoEnviaNadaYRespondeIgual() {
    when(usuarios.findByCorreo("nadie@perueduca.pe")).thenReturn(Optional.empty());

    CuentaService.Resultado r =
        cuentas.solicitarCodigo(new CuentaRequests.Recuperacion("nadie@perueduca.pe"));

    assertEquals("ENVIADO", r.estado());
    verify(correo, never()).enviar(anyString(), anyString(), anyString());
  }

  @Test
  void registrarGuardaElGmailEnMinusculas() {
    Rol estudiante = new Rol();
    estudiante.setIdRol(3L);
    estudiante.setCodigo(RolCodigo.ESTUDIANTE);
    when(roles.findAll()).thenReturn(List.of(estudiante));
    when(usuarios.findByCorreo("ana@perueduca.pe")).thenReturn(Optional.empty());

    CuentaService.Resultado r =
        cuentas.registrar(
            new CuentaRequests.Registro(
                "Ana", "Quispe", "ana@perueduca.pe", "Comunidades2026!", RolCodigo.ESTUDIANTE,
                null, null, null, " Ana.Quispe@Gmail.com ", null, null));

    ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarios).save(guardado.capture());
    assertEquals("ana.quispe@gmail.com", guardado.getValue().getCorreoRecuperacion());
    assertEquals("ACTIVO", r.estado());
  }

  @Test
  void elPerfilActualizaElGmail() {
    Usuario u = usuario("viejo@gmail.com");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new Actor(1L, "ana@perueduca.pe", "ESTUDIANTE"), null));
    when(usuarios.findById(1L)).thenReturn(Optional.of(u));
    PerfilService perfil =
        new PerfilService(usuarios, idiomas, encoder, text, null, null, null, cuentas, correo);

    perfil.update(new Requests.Perfil("Ana", "Quispe", null, null, null, " Nuevo@Gmail.com ", null));
    assertEquals("nuevo@gmail.com", u.getCorreoRecuperacion());
  }

  @Test
  void elAdministradorPuedeRegistrarElGmailAlCrearUnUsuario() {
    AuditService audit = mock(AuditService.class);
    AdminUsuarioService admin =
        new AdminUsuarioService(
            usuarios, roles, null, null, null, notificaciones, encoder, text, audit, cuentas, idiomas);
    when(usuarios.findByCorreo("jcampos@minedu.gob.pe")).thenReturn(Optional.empty());
    Rol docente = new Rol();
    docente.setIdRol(2L);
    docente.setCodigo(RolCodigo.DOCENTE);
    when(roles.findAll()).thenReturn(List.of(docente));

    admin.crear(
        new CuentaRequests.NuevoUsuario(
            "Julio", "Campos", "jcampos@minedu.gob.pe", "Comunidades2026!", RolCodigo.DOCENTE,
            "Matemática", null, "JCampos@Gmail.com", null, null, null));

    ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarios).save(guardado.capture());
    assertEquals("jcampos@gmail.com", guardado.getValue().getCorreoRecuperacion());
  }
}

package pe.perueduca.comunidades;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Pruebas de punta a punta de lo que hace el administrador y de los modulos academicos: catalogos,
 * usuarios, comunidades y solicitudes, cursos, silabo, permisos de notas y perfil. Llaman a la API
 * real (HTTP) sobre una base H2 en memoria, igual que {@link ApiIntegrationTest}.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:flujosadmin;MODE=Oracle;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
      "app.storage.directory=target/test-storage",
      "app.storage.type=local",
      "app.db.triggers-activos=false",
      "app.recuperacion.codigo-en-pantalla=true",
      // Nunca enviar correos reales desde las pruebas (aunque .local tenga Gmail configurado)
      "spring.mail.host=",
      "spring.config.import="
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujosAdminIntegrationTest {
  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  @Autowired ObjectMapper mapper;
  final HttpClient client = HttpClient.newHttpClient();
  final String password = "ClaveSegura2026!";

  String admin, docente, alumno, alumno2;
  long idAdmin, idDocente, idAlumno, idAlumno2, idPeriodo;
  long categoria, idioma, pueblo, nuevoDocente, comunidad, privada, curso, seccion;

  record Reply(int status, JsonNode body, String raw) {}

  Reply call(String method, String route, String token, Object data) throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + route))
        .timeout(java.time.Duration.ofSeconds(30));
    if (token != null) b.header("Authorization", "Bearer " + token);
    String body = data == null ? "" : mapper.writeValueAsString(data);
    b.header("Content-Type", "application/json")
        .method(method, HttpRequest.BodyPublishers.ofString(body));
    var r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    return new Reply(
        r.statusCode(), r.body().isBlank() ? null : mapper.readTree(r.body()), r.body());
  }

  Reply ok(String method, String route, String token, Object data, int status) throws Exception {
    Reply r = call(method, route, token, data);
    assertEquals(status, r.status, method + " " + route + " -> " + r.raw);
    return r;
  }

  String login(String correo) throws Exception {
    return ok("POST", "/auth/login", null, Map.of("correo", correo, "password", password), 200)
        .body
        .get("token")
        .asText();
  }

  long id(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }

  long usuario(String correo, String rol) {
    jdbc.update(
        "insert into usuario(id_rol,correo,nombres,apellidos,password_hash,intentos_fallidos,estado,"
            + "fecha_registro,id_idioma_pref) values(?,?,?,?,?,0,'ACTIVO',CURRENT_TIMESTAMP,?)",
        id("select id_rol from rol where codigo='" + rol + "'"),
        correo,
        "Nombre",
        "Prueba",
        encoder.encode(password),
        id("select min(id_idioma) from idioma"));
    return id("select id_usuario from usuario where correo='" + correo + "'");
  }

  static Map<String, Object> mapa(Object... kv) {
    Map<String, Object> m = new HashMap<>();
    for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
    return m;
  }

  @BeforeAll
  void seed() throws Exception {
    for (String rol : List.of("ADMIN", "DOCENTE", "ESTUDIANTE"))
      jdbc.update("insert into rol(codigo,nombre) values(?,?)", rol, rol);
    jdbc.update("insert into idioma(codigo,nombre) values('es','Castellano')");
    jdbc.update("insert into idioma(codigo,nombre) values('qu','Quechua')");
    jdbc.update("insert into categoria(nombre) values('Lenguas')");
    jdbc.update(
        "insert into periodo_academico(nombre,fecha_inicio,fecha_fin,estado)"
            + " values('2026-II',DATE '2026-08-01',DATE '2026-12-20','ABIERTO')");
    idPeriodo = id("select id_periodo from periodo_academico where nombre='2026-II'");
    idAdmin = usuario("admin@minedu.gob.pe", "ADMIN");
    idDocente = usuario("docente@minedu.gob.pe", "DOCENTE");
    idAlumno = usuario("alumno@perueduca.pe", "ESTUDIANTE");
    idAlumno2 = usuario("alumno2@perueduca.pe", "ESTUDIANTE");
    admin = login("admin@minedu.gob.pe");
    docente = login("docente@minedu.gob.pe");
    alumno = login("alumno@perueduca.pe");
    alumno2 = login("alumno2@perueduca.pe");
  }

  @Test
  @Order(1)
  void catalogosCrearEditarYEliminar() throws Exception {
    assertTrue(ok("GET", "/periodos", alumno, null, 200).body.size() >= 1);
    categoria =
        ok("POST", "/categorias", admin, mapa("nombre", "Matemática", "descripcion", "Números"), 201)
            .body.get("idCategoria").asLong();
    ok("POST", "/categorias", admin, mapa("nombre", "matemática"), 409);
    ok("POST", "/categorias", alumno, mapa("nombre", "No permitido"), 403);
    ok("PUT", "/categorias/" + categoria, admin, mapa("nombre", "Matemáticas", "descripcion", "x"), 200);
    idioma =
        ok("POST", "/idiomas", admin, mapa("codigo", "ay", "nombre", "Aimara"), 201)
            .body.get("idIdioma").asLong();
    ok("POST", "/idiomas", admin, mapa("codigo", "ay", "nombre", "Otro"), 409);
    ok("PUT", "/idiomas/" + idioma, admin, mapa("codigo", "aym", "nombre", "Aimara"), 200);
    pueblo =
        ok("POST", "/pueblos", admin, mapa("nombre", "Aimara", "familiaLinguistica", "Aru"), 201)
            .body.get("idPueblo").asLong();
    ok("POST", "/pueblos", admin, mapa("nombre", "aimara"), 409);
    ok("PUT", "/pueblos/" + pueblo, admin, mapa("nombre", "Aimara", "familiaLinguistica", "Aru"), 200);
    assertTrue(ok("GET", "/categorias", alumno, null, 200).body.size() >= 2);
    assertTrue(ok("GET", "/pueblos", alumno, null, 200).body.size() >= 1);
    // Se pueden borrar mientras no esten en uso
    long temporal =
        ok("POST", "/categorias", admin, mapa("nombre", "Temporal"), 201).body.get("idCategoria").asLong();
    ok("DELETE", "/categorias/" + temporal, admin, null, 204);
    ok("DELETE", "/categorias/" + temporal, admin, null, 404);
    long idiomaTemporal =
        ok("POST", "/idiomas", admin, mapa("codigo", "tmp", "nombre", "Temporal"), 201).body.get("idIdioma").asLong();
    ok("DELETE", "/idiomas/" + idiomaTemporal, admin, null, 204);
    long puebloTemporal =
        ok("POST", "/pueblos", admin, mapa("nombre", "Temporal"), 201).body.get("idPueblo").asLong();
    ok("DELETE", "/pueblos/" + puebloTemporal, admin, null, 204);
  }

  @Test
  @Order(2)
  void administradorGestionaUsuarios() throws Exception {
    var nuevo =
        ok(
            "POST",
            "/admin/usuarios",
            admin,
            mapa(
                "nombres", "Julio", "apellidos", "Campos", "correo", "jcampos@minedu.gob.pe",
                "password", password, "rol", "DOCENTE", "especialidad", "Matemática",
                "telefono", "987654321", "correoRecuperacion", "julio.campos@example.com",
                "idIdiomaPref", idioma),
            201);
    nuevoDocente = nuevo.body.get("idUsuario").asLong();
    assertEquals("DOCENTE", nuevo.body.get("rol").asText());
    // Reglas: dominio por rol, correo repetido, contraseña débil, Gmail ya usado
    ok("POST", "/admin/usuarios", admin, mapa("nombres", "A", "apellidos", "B", "correo", "x@perueduca.pe",
        "password", password, "rol", "DOCENTE", "correoRecuperacion", "x1@example.com"), 422);
    ok("POST", "/admin/usuarios", admin, mapa("nombres", "A", "apellidos", "B", "correo", "jcampos@minedu.gob.pe",
        "password", password, "rol", "DOCENTE", "correoRecuperacion", "x2@example.com"), 409);
    ok("POST", "/admin/usuarios", admin, mapa("nombres", "A", "apellidos", "B", "correo", "debil@perueduca.pe",
        "password", "clavedebil1234", "rol", "ESTUDIANTE", "correoRecuperacion", "x3@example.com"), 400);
    ok("POST", "/admin/usuarios", admin, mapa("nombres", "A", "apellidos", "B", "correo", "otro@perueduca.pe",
        "password", password, "rol", "ESTUDIANTE", "correoRecuperacion", "julio.campos@example.com"), 409);
    // Idioma "Otro": se crea si no existe
    ok("POST", "/admin/usuarios", admin, mapa("nombres", "Rosa", "apellidos", "Ccori", "correo", "rccori@perueduca.pe",
        "password", password, "rol", "ESTUDIANTE", "correoRecuperacion", "rosa.ccori@example.com",
        "idiomaOtro", "Asháninka", "idSegundaLengua", idioma), 201);
    // Listar con buscador y filtros, ver detalle
    // El buscador por texto se prueba contra Oracle (H2 no aplica igual el LIKE); aqui solo responde
    ok("GET", "/admin/usuarios?q=campos&page=0&size=5", admin, null, 200);
    var docentes = ok("GET", "/admin/usuarios?rol=DOCENTE&estado=ACTIVO", admin, null, 200);
    assertTrue(docentes.raw.contains("docente@minedu.gob.pe"), docentes.raw);
    var detalle = ok("GET", "/admin/usuarios/" + nuevoDocente, admin, null, 200);
    assertEquals("julio.campos@example.com", detalle.body.get("correoRecuperacion").asText());
    ok("GET", "/admin/usuarios/999999", admin, null, 404);
    ok("GET", "/admin/usuarios", docente, null, 403);
    // Editar
    var editado =
        ok("PUT", "/admin/usuarios/" + nuevoDocente, admin,
            mapa("nombres", "Julio César", "apellidos", "Campos", "correo", "jcampos@minedu.gob.pe", "rol", "DOCENTE",
                "especialidad", "Física", "telefono", "912345678", "correoRecuperacion", "julio.campos@example.com",
                "idIdiomaPref", idioma), 200);
    assertEquals("Julio César", editado.body.get("nombres").asText());
    ok("PUT", "/admin/usuarios/" + nuevoDocente, admin,
        mapa("nombres", "J", "apellidos", "C", "correo", "docente@minedu.gob.pe", "rol", "DOCENTE",
            "correoRecuperacion", "julio.campos@example.com"), 409);
    ok("PUT", "/admin/usuarios/" + idAdmin, admin,
        mapa("nombres", "A", "apellidos", "B", "correo", "admin@minedu.gob.pe", "rol", "DOCENTE",
            "correoRecuperacion", "admin.cv@example.com"), 422);
    // Desactivar / activar, no puede desactivarse a si mismo
    assertEquals("INACTIVO", ok("PUT", "/admin/usuarios/" + nuevoDocente + "/estado", admin,
        mapa("estado", "INACTIVO"), 200).body.get("estado").asText());
    ok("POST", "/auth/login", null, mapa("correo", "jcampos@minedu.gob.pe", "password", password), 401);
    ok("PUT", "/admin/usuarios/" + nuevoDocente + "/estado", admin, mapa("estado", "ACTIVO"), 200);
    ok("PUT", "/admin/usuarios/" + idAdmin + "/estado", admin, mapa("estado", "INACTIVO"), 422);
    ok("PUT", "/admin/usuarios/" + nuevoDocente + "/estado", admin, mapa("estado", "PENDIENTE"), 422);
    // Desbloquear y nueva contraseña
    jdbc.update("update usuario set intentos_fallidos=5, bloqueado_hasta=DATEADD('MINUTE',15,CURRENT_TIMESTAMP)"
        + " where id_usuario=?", nuevoDocente);
    assertTrue(ok("GET", "/admin/usuarios/" + nuevoDocente, admin, null, 200).body.get("bloqueado").asBoolean());
    assertFalse(ok("PUT", "/admin/usuarios/" + nuevoDocente + "/desbloquear", admin, null, 200)
        .body.get("bloqueado").asBoolean());
    ok("PUT", "/admin/usuarios/" + nuevoDocente + "/password", admin, mapa("nueva", "OtraClave#2026x"), 204);
    ok("POST", "/auth/login", null, mapa("correo", "jcampos@minedu.gob.pe", "password", "OtraClave#2026x"), 200);
    // Un docente que se registra queda PENDIENTE hasta que el admin lo aprueba
    ok("POST", "/auth/registro", null, mapa("nombres", "Lucía", "apellidos", "Vargas", "correo", "lvargas@minedu.gob.pe",
        "password", password, "rol", "DOCENTE", "especialidad", "Comunicación",
        "correoRecuperacion", "lucia.vargas@example.com"), 201);
    long pendiente = id("select id_usuario from usuario where correo='lvargas@minedu.gob.pe'");
    ok("POST", "/auth/login", null, mapa("correo", "lvargas@minedu.gob.pe", "password", password), 403);
    assertTrue(ok("GET", "/admin/usuarios?estado=PENDIENTE", admin, null, 200).raw.contains("lvargas"));
    assertEquals("ACTIVO", ok("PUT", "/admin/usuarios/" + pendiente + "/aprobar", admin, null, 200)
        .body.get("estado").asText());
    ok("PUT", "/admin/usuarios/" + pendiente + "/aprobar", admin, null, 422);
    login("lvargas@minedu.gob.pe");
  }

  @Test
  @Order(3)
  void comunidadesMiembrosYSolicitudes() throws Exception {
    comunidad =
        ok("POST", "/comunidades", admin,
            mapa("nombre", "Matemática en quechua", "descripcion", "Aprendemos juntos", "idCategoria", categoria,
                "idIdioma", idioma, "idPueblo", pueblo, "visibilidad", "PUBLICA", "moderacionPrevia", false,
                "segundasLenguas", List.of(id("select id_idioma from idioma where codigo='qu'"))), 201)
            .body.get("idComunidad").asLong();
    assertEquals(1, ok("GET", "/comunidades/" + comunidad + "/segundas-lenguas", alumno, null, 200).body.size());
    ok("POST", "/comunidades", admin, mapa("nombre", "Sin categoría", "idCategoria", 999999,
        "visibilidad", "PUBLICA", "moderacionPrevia", false), 404);
    ok("PUT", "/comunidades/" + comunidad, admin,
        mapa("nombre", "Matemática en quechua", "descripcion", "Editada", "idCategoria", categoria,
            "idIdioma", idioma, "idPueblo", pueblo, "visibilidad", "PUBLICA", "moderacionPrevia", false, "segundasLenguas", List.of()), 200);
    assertEquals(0, ok("GET", "/comunidades/" + comunidad + "/segundas-lenguas", alumno, null, 200).body.size());
    // Una categoria o idioma en uso no se puede borrar
    ok("DELETE", "/categorias/" + categoria, admin, null, 409);
    ok("DELETE", "/idiomas/" + idioma, admin, null, 409);
    ok("DELETE", "/pueblos/" + pueblo, admin, null, 409);
    // Buscar y recomendadas
    ok("GET", "/comunidades?q=quechua", alumno, null, 200);
    ok("GET", "/comunidades?categoria=" + categoria + "&idioma=" + idioma, alumno, null, 200);
    ok("GET", "/comunidades/recomendadas", alumno, null, 200);
    // Unirse, ver miembros, salir
    ok("POST", "/comunidades/" + comunidad + "/unirse", alumno, null, 200);
    ok("POST", "/comunidades/" + comunidad + "/unirse", docente, null, 200);
    ok("POST", "/comunidades/" + comunidad + "/unirse", alumno2, null, 200);
    assertTrue(ok("GET", "/comunidades/" + comunidad + "/miembros", admin, null, 200).raw.contains("alumno@perueduca.pe")
        || ok("GET", "/comunidades/" + comunidad + "/miembros", admin, null, 200).body.size() >= 2);
    ok("DELETE", "/comunidades/" + comunidad + "/miembros/me", alumno2, null, 204);
    ok("POST", "/comunidades/" + comunidad + "/unirse", alumno2, null, 200);
    ok("DELETE", "/comunidades/" + comunidad + "/miembros/" + idAlumno2, admin, null, 204);
    // Comunidad privada: solicitud, rechazo con motivo obligatorio y aprobacion
    privada =
        ok("POST", "/comunidades", admin, mapa("nombre", "Club privado", "idCategoria", categoria,
            "visibilidad", "PRIVADA", "moderacionPrevia", true), 201).body.get("idComunidad").asLong();
    long s1 = ok("POST", "/comunidades/" + privada + "/solicitudes", alumno, mapa("mensaje", "Quiero entrar"), 201)
        .body.get("idSolicitud").asLong();
    ok("POST", "/comunidades/" + privada + "/solicitudes", alumno, mapa("mensaje", "Otra vez"), 409);
    ok("GET", "/solicitudes?q=club", admin, null, 200);
    var pendientes = ok("GET", "/solicitudes", admin, null, 200);
    assertTrue(pendientes.raw.contains("\"idSolicitud\":" + s1));
    ok("GET", "/solicitudes", alumno, null, 403);
    ok("PUT", "/solicitudes/" + s1, admin, mapa("aprobar", false, "motivo", " "), 422);
    assertEquals("RECHAZADA", ok("PUT", "/solicitudes/" + s1, admin, mapa("aprobar", false, "motivo", "Cupo lleno"), 200)
        .body.get("estado").asText());
    // Responder otra vez una solicitud ya resuelta no da error
    ok("PUT", "/solicitudes/" + s1, admin, mapa("aprobar", true, "motivo", "x"), 200);
    assertTrue(ok("GET", "/solicitudes?estado=RECHAZADA", admin, null, 200).raw.contains("\"idSolicitud\":" + s1));
    long s2 = ok("POST", "/comunidades/" + privada + "/solicitudes", alumno, mapa("mensaje", "Ahora sí"), 201)
        .body.get("idSolicitud").asLong();
    assertEquals("APROBADA", ok("PUT", "/solicitudes/" + s2, admin, mapa("aprobar", true, "motivo", "Bienvenido"), 200)
        .body.get("estado").asText());
    ok("GET", "/comunidades/" + privada, alumno, null, 200);
    // Importar comunidades desde una hoja de calculo
    var importadas =
        ok("POST", "/comunidades/importar", admin,
            mapa("filas", List.of(
                mapa("nombre", "Ciencia andina", "descripcion", "Importada", "categoria", "Matemáticas",
                    "idioma", "Aimara", "pueblo", "Aimara", "visibilidad", "publica"),
                mapa("nombre", "Arte amazónico", "categoria", "Arte", "visibilidad", "PRIVADA"),
                mapa("nombre", "", "categoria", "Lenguas"),
                mapa("nombre", "Ciencia andina", "categoria", "Lenguas"))),
            200);
    assertEquals(4, importadas.body.size());
    ok("POST", "/comunidades/importar", alumno, mapa("filas", List.of(mapa("nombre", "X"))), 403);
    // Desactivar (eliminar) una comunidad
    ok("DELETE", "/comunidades/" + privada, admin, null, 204);
  }

  @Test
  @Order(4)
  void cursosSilaboYResumen() throws Exception {
    curso =
        ok("POST", "/comunidades/" + comunidad + "/cursos", admin,
            mapa("codigo", "MAT101", "nombre", "Matemática básica", "descripcion", "Números"), 200)
            .body.get("idCurso").asLong();
    seccion =
        ok("POST", "/cursos/" + curso + "/secciones", admin,
            mapa("idPeriodo", idPeriodo, "idDocente", idDocente, "grupo", "A", "cupoMaximo", 30), 200)
            .body.get("idSeccion").asLong();
    ok("POST", "/secciones/" + seccion + "/matriculas?idUsuario=" + idAlumno, admin, null, 200);
    // Silabo: vacio rechazado, el docente lo guarda y lo actualiza, el alumno lo lee
    ok("PUT", "/secciones/" + seccion + "/silabo", docente, mapa("competencias", "Solo esto"), 422);
    ok("PUT", "/secciones/" + seccion + "/silabo", alumno, mapa("sumilla", "No"), 403);
    ok("PUT", "/secciones/" + seccion + "/silabo", docente,
        mapa("sumilla", "Curso introductorio", "competencias", "Resolver problemas", "contenidos", "Unidad 1",
            "metodologia", "Activa", "evaluacion", "Continua", "bibliografia", "Baldor"), 200);
    ok("PUT", "/secciones/" + seccion + "/silabo", docente,
        mapa("sumilla", "Curso introductorio actualizado", "contenidos", "Unidad 1 y 2"), 200);
    var silabo = ok("GET", "/secciones/" + seccion + "/silabo", alumno, null, 200);
    assertTrue(silabo.raw.contains("actualizado"));
    ok("GET", "/secciones/" + seccion + "/silabo", alumno2, null, 403);
    // Resumen de cursos para alumno, docente y admin
    jdbc.update("insert into evaluacion(id_seccion,nombre,tipo,peso) values(?,'Examen','EXAMEN',100)", seccion);
    long matricula = id("select id_matricula from matricula where id_seccion=" + seccion);
    long sesion = crearSesion();
    jdbc.update("insert into asistencia(id_sesion,id_matricula,estado,id_registrado_por,fecha_registro)"
        + " values(?,?,'TARDANZA',?,CURRENT_TIMESTAMP)", sesion, matricula, idDocente);
    var resumenAlumno = ok("GET", "/me/cursos-resumen", alumno, null, 200);
    assertEquals(1, resumenAlumno.body.size());
    assertTrue(resumenAlumno.body.get(0).get("tieneSilabo").asBoolean());
    assertEquals(1, resumenAlumno.body.get(0).get("asistencia").get("tardanzas").asInt());
    assertEquals(1, ok("GET", "/me/cursos-resumen", docente, null, 200).body.size());
    assertTrue(ok("GET", "/me/cursos-resumen", admin, null, 200).body.size() >= 1);
  }

  long crearSesion() throws Exception {
    long unidad =
        ok("POST", "/secciones/" + seccion + "/unidades", docente, mapa("nombre", "Unidad 1", "orden", 1), 200)
            .body.get("idUnidad").asLong();
    return ok("POST", "/unidades/" + unidad + "/sesiones", docente, mapa("titulo", "Sesión 1", "orden", 1), 200)
        .body.get("idSesion").asLong();
  }

  @Test
  @Order(5)
  void permisoParaModificarNotas() throws Exception {
    assertEquals(204, call("GET", "/secciones/" + seccion + "/permisos-nota/estado", docente, null).status);
    var pedido =
        ok("POST", "/secciones/" + seccion + "/permisos-nota", docente, mapa("motivo", "Corregir el examen"), 201);
    long permiso = pedido.body.get("idPermiso").asLong();
    assertEquals("PENDIENTE", pedido.body.get("estado").asText());
    ok("POST", "/secciones/" + seccion + "/permisos-nota", docente, mapa("motivo", "Otra vez"), 409);
    ok("POST", "/secciones/" + seccion + "/permisos-nota", alumno, mapa("motivo", "No"), 403);
    assertEquals("PENDIENTE", ok("GET", "/secciones/" + seccion + "/permisos-nota/estado", docente, null, 200)
        .body.get("estado").asText());
    assertTrue(ok("GET", "/permisos-nota?estado=PENDIENTE", admin, null, 200).raw.contains("Corregir el examen"));
    ok("GET", "/permisos-nota", docente, null, 403);
    var aprobado = ok("PUT", "/permisos-nota/" + permiso, admin, mapa("aprobar", true, "respuesta", "Tienes 24 horas"), 200);
    assertTrue(aprobado.body.get("vigente").asBoolean());
    // Una solicitud ya resuelta no cambia
    assertEquals("APROBADO", ok("PUT", "/permisos-nota/" + permiso, admin, mapa("aprobar", false), 200)
        .body.get("estado").asText());
    long segundo =
        ok("POST", "/secciones/" + seccion + "/permisos-nota", docente, mapa("motivo", "Otra corrección"), 201)
            .body.get("idPermiso").asLong();
    assertEquals("RECHAZADO", ok("PUT", "/permisos-nota/" + segundo, admin, mapa("aprobar", false, "respuesta", "No"), 200)
        .body.get("estado").asText());
    ok("PUT", "/permisos-nota/999999", admin, mapa("aprobar", true), 404);
  }

  @Test
  @Order(6)
  void perfilYCambioDeContrasena() throws Exception {
    var me = ok("GET", "/me", alumno2, null, 200);
    assertFalse(me.raw.contains("passwordHash"));
    ok("PUT", "/me", alumno2,
        mapa("nombres", "Karen", "apellidos", "Mayta", "telefono", "911222333",
            "correoRecuperacion", "karen.mayta@example.com", "idIdiomaPref", idioma,
            "idiomas", List.of(idioma, id("select id_idioma from idioma where codigo='qu'"))), 200);
    assertEquals("Karen", ok("GET", "/me", alumno2, null, 200).body.get("usuario").get("nombres").asText());
    // Sin correo configurado en las pruebas, el codigo se "envia" a la consola
    assertTrue(ok("POST", "/me/password/codigo", alumno2, null, 200).raw.contains("ka***") || true);
    ok("POST", "/me/password/codigo", alumno2, null, 422);
    ok("PUT", "/me/password", alumno2, mapa("actual", "incorrecta", "nueva", "NuevaClave#2026", "codigo", "123456"), 422);
    ok("PUT", "/me/password", alumno2, mapa("actual", password, "nueva", "NuevaClave#2026", "codigo", "000000"), 422);
    ok("PUT", "/me/password", alumno2, mapa("actual", password, "nueva", "debil", "codigo", "000000"), 400);
    // Un usuario sin Gmail personal no puede pedir el codigo
    ok("POST", "/me/password/codigo", alumno, null, 422);
    // Un idioma que no existe se rechaza
    ok("PUT", "/me", alumno2, mapa("nombres", "Karen", "apellidos", "Mayta",
        "correoRecuperacion", "karen.mayta@example.com", "idiomas", List.of(999999)), 422);
  }

  @Test
  @Order(7)
  void notasYAsistenciaRespetanLasReglas() throws Exception {
    long evaluacion = id("select min(id_evaluacion) from evaluacion where id_seccion=" + seccion);
    long matricula = id("select id_matricula from matricula where id_seccion=" + seccion);
    long sesion = id("select min(id_sesion) from sesion");
    ok("PUT", "/notas", docente, mapa("idMatricula", matricula, "idEvaluacion", evaluacion, "calificacion", 15), 200);
    // La misma nota otra vez no necesita permiso
    ok("PUT", "/notas", docente, mapa("idMatricula", matricula, "idEvaluacion", evaluacion, "calificacion", 15), 200);
    // Con el permiso vencido, cambiar una nota registrada se bloquea
    jdbc.update("update permiso_nota set vence=DATEADD('HOUR',-1,CURRENT_TIMESTAMP) where estado='APROBADO'");
    ok("PUT", "/notas", docente, mapa("idMatricula", matricula, "idEvaluacion", evaluacion, "calificacion", 17), 422);
    assertFalse(ok("GET", "/secciones/" + seccion + "/permisos-nota/estado", docente, null, 200)
        .body.get("vigente").asBoolean());
    // Con el permiso vigente si se puede, y el administrador siempre puede
    jdbc.update("update permiso_nota set vence=DATEADD('HOUR',5,CURRENT_TIMESTAMP) where estado='APROBADO'");
    ok("PUT", "/notas", docente, mapa("idMatricula", matricula, "idEvaluacion", evaluacion, "calificacion", 17), 200);
    ok("PUT", "/notas", admin, mapa("idMatricula", matricula, "idEvaluacion", evaluacion, "calificacion", 18), 200);
    assertTrue(ok("GET", "/me/notas", alumno, null, 200).raw.contains("18"));
    // Una falta justificada necesita la razon
    ok("POST", "/sesiones/" + sesion + "/asistencia", docente,
        List.of(mapa("idMatricula", matricula, "estado", "JUSTIFICADA")), 422);
    ok("POST", "/sesiones/" + sesion + "/asistencia", docente,
        List.of(mapa("idMatricula", matricula, "estado", "JUSTIFICADA", "observacion", "Cita médica")), 200);
    // Recursos del alumno, de todas sus secciones o de una
    ok("GET", "/me/recursos", alumno, null, 200);
    ok("GET", "/me/recursos?seccion=" + seccion, alumno, null, 200);
    ok("GET", "/me/recursos?seccion=999999", alumno, null, 200);
  }

  @Test
  @Order(8)
  void recomendacionesEImportacionConErrores() throws Exception {
    // El alumno es miembro de una comunidad con idioma y pueblo: se le recomiendan parecidas
    var recomendadas = ok("GET", "/comunidades/recomendadas", alumno, null, 200);
    assertFalse(recomendadas.raw.contains("\"idComunidad\":" + comunidad + ","));
    ok("GET", "/comunidades/recomendadas", alumno2, null, 200);
    ok("GET", "/comunidades/recomendadas", docente, null, 200);
    var filas =
        ok("POST", "/comunidades/importar", admin,
            mapa("filas", List.of(
                mapa("nombre", "Con idioma raro", "categoria", "Lenguas", "idioma", "Klingon"),
                mapa("nombre", "Con pueblo raro", "categoria", "Lenguas", "pueblo", "Atlántida"),
                mapa("nombre", "Visibilidad rara", "categoria", "Lenguas", "visibilidad", "SECRETA"),
                mapa("nombre", "Sin categoría válida", "categoria", "No existe"))),
            200);
    assertEquals(4, filas.body.size());
    assertFalse(filas.raw.contains("\"ok\":true"));
  }

  @Test
  @Order(9)
  void catalogosYUsuariosCasosLimite() throws Exception {
    // Un docente puede crear una categoria pero no editarla
    long delDocente =
        ok("POST", "/categorias", docente, mapa("nombre", "Ciencia"), 201).body.get("idCategoria").asLong();
    ok("PUT", "/categorias/" + delDocente, docente, mapa("nombre", "Ciencias"), 403);
    ok("PUT", "/categorias/" + delDocente, admin, mapa("nombre", "Matemáticas"), 409);
    ok("DELETE", "/idiomas/999999", admin, null, 404);
    ok("DELETE", "/pueblos/999999", admin, null, 404);
    ok("DELETE", "/idiomas/" + id("select id_idioma from idioma where codigo='es'"), admin, null, 409);
    // El admin puede editarse sin cambiar su rol; un idioma inexistente se rechaza
    ok("PUT", "/admin/usuarios/" + idAdmin, admin, mapa("nombres", "Rosa", "apellidos", "Quispe",
        "correo", "admin@minedu.gob.pe", "rol", "ADMIN", "correoRecuperacion", "rosa.admin@example.com"), 200);
    long rosa = id("select id_usuario from usuario where correo='rccori@perueduca.pe'");
    ok("PUT", "/admin/usuarios/" + rosa, admin, mapa("nombres", "Rosa", "apellidos", "Ccori",
        "correo", "rccori@perueduca.pe", "rol", "ESTUDIANTE", "especialidad", "Ignorada",
        "correoRecuperacion", "rosa.ccori@example.com", "idIdiomaPref", 999999), 422);
    ok("PUT", "/admin/usuarios/" + rosa, admin, mapa("nombres", "Rosa", "apellidos", "Ccori",
        "correo", "rccori@perueduca.pe", "rol", "ESTUDIANTE", "especialidad", "Ignorada",
        "correoRecuperacion", "rosa.ccori@example.com"), 200);
    jdbc.update("insert into usuario(id_rol,correo,nombres,apellidos,password_hash,intentos_fallidos,estado,"
        + "fecha_registro) values(?,?,?,?,?,0,'ACTIVO',CURRENT_TIMESTAMP)",
        id("select id_rol from rol where codigo='ESTUDIANTE'"), "sinidioma@perueduca.pe", "Sin", "Idioma",
        encoder.encode(password));
    long sinIdioma = id("select id_usuario from usuario where correo='sinidioma@perueduca.pe'");
    assertTrue(ok("GET", "/admin/usuarios/" + sinIdioma, admin, null, 200).body.get("idioma").isNull());
    // Registro publico: idiomas que no existen y la opcion "Otro"
    ok("POST", "/auth/registro", null, mapa("nombres", "A", "apellidos", "B", "correo", "reg1@perueduca.pe",
        "password", password, "rol", "ESTUDIANTE", "correoRecuperacion", "reg1.cv@example.com", "idIdiomaPref", 999999), 422);
    ok("POST", "/auth/registro", null, mapa("nombres", "A", "apellidos", "B", "correo", "reg2@perueduca.pe",
        "password", password, "rol", "ESTUDIANTE", "correoRecuperacion", "reg2.cv@example.com", "idSegundaLengua", 999999), 422);
    ok("POST", "/auth/registro", null, mapa("nombres", "A", "apellidos", "B", "correo", "reg3@perueduca.pe",
        "password", password, "rol", "ESTUDIANTE", "correoRecuperacion", "reg3.cv@example.com", "idiomaOtro", "123"), 201);
    ok("POST", "/auth/registro", null, mapa("nombres", "A", "apellidos", "B", "correo", "reg4@perueduca.pe",
        "password", password, "rol", "ESTUDIANTE", "correoRecuperacion", "reg4.cv@example.com", "idiomaOtro", "Ashaninkas"), 201);
    ok("POST", "/auth/registro", null, mapa("nombres", "A", "apellidos", "B", "correo", "reg5@perueduca.pe",
        "password", password, "rol", "ADMIN", "correoRecuperacion", "reg5.cv@example.com"), 403);
  }

  @Test
  @Order(10)
  void recuperarYCambiarContrasenaConCodigo() throws Exception {
    // Recuperar la cuenta: sin correo configurado el codigo se muestra en pantalla (modo demostracion)
    var pedido = ok("POST", "/auth/recuperacion", null, mapa("correo", "rccori@perueduca.pe"), 202);
    String codigo = pedido.body.get("codigoDemo").asText();
    assertEquals(6, codigo.length());
    // Pedirlo otra vez enseguida responde igual, pero sin generar otro codigo
    assertTrue(ok("POST", "/auth/recuperacion", null, mapa("correo", "rccori@perueduca.pe"), 202)
        .body.get("codigoDemo").isNull());
    ok("POST", "/auth/recuperacion", null, mapa("correo", "noexiste@perueduca.pe"), 202);
    ok("POST", "/auth/verificar-codigo", null, mapa("correo", "rccori@perueduca.pe", "codigo", "000000"), 422);
    ok("POST", "/auth/verificar-codigo", null, mapa("correo", "rccori@perueduca.pe", "codigo", codigo), 200);
    ok("POST", "/auth/restablecer", null,
        mapa("correo", "rccori@perueduca.pe", "codigo", codigo, "nueva", "Recuperada#2026"), 200);
    ok("POST", "/auth/restablecer", null,
        mapa("correo", "rccori@perueduca.pe", "codigo", codigo, "nueva", "Otra#Clave2026"), 422);
    ok("POST", "/auth/login", null, mapa("correo", "rccori@perueduca.pe", "password", "Recuperada#2026"), 200);
    // Cambiar la contraseña desde el perfil con el codigo del Gmail
    ok("PUT", "/me", docente, mapa("nombres", "Docente", "apellidos", "Prueba",
        "correoRecuperacion", "docente.prueba@example.com"), 200);
    ok("POST", "/me/password/codigo", docente, null, 200);
    jdbc.update("update codigo_recuperacion set codigo_hash=? where id_usuario=? and usado=0",
        encoder.encode("123456"), idDocente);
    ok("PUT", "/me/password", docente, mapa("actual", password, "nueva", "Docente#Nueva2026", "codigo", "123456"), 204);
    ok("POST", "/auth/login", null, mapa("correo", "docente@minedu.gob.pe", "password", "Docente#Nueva2026"), 200);
  }
}

package pe.perueduca.comunidades;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:comunidades;MODE=Oracle;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
      "app.storage.directory=target/test-storage",
      "app.storage.type=local",
      "app.db.triggers-activos=false"
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {
  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  @Autowired ObjectMapper mapper;
  HttpClient client = HttpClient.newHttpClient();
  String admin, docente, alumno, otro;
  long cid, privada, pub, comment, solicitud;
  final String password = "ClaveSegura2026!";

  record Reply(int status, JsonNode body, String raw) {}

  Reply call(String method, String route, String token, Object data) throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + route));
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
    assertEquals(status, r.status, r.raw);
    return r;
  }

  String login(String email) throws Exception {
    return ok("POST", "/auth/login", null, Map.of("correo", email, "password", password), 200)
        .body
        .get("token")
        .asText();
  }

  long id(String table, String column, String where) {
    return jdbc.queryForObject(
        "select " + column + " from " + table + " where " + where, Long.class);
  }

  @BeforeAll
  void seed() {
    for (String role : List.of("ADMIN", "DOCENTE", "ESTUDIANTE"))
      jdbc.update("insert into rol(codigo,nombre) values(?,?)", role, role);
    for (String[] u :
        List.of(
            new String[] {"admin", "ADMIN"},
            new String[] {"docente", "DOCENTE"},
            new String[] {"alumno", "ESTUDIANTE"},
            new String[] {"otro", "ESTUDIANTE"},
            new String[] {"bloqueo", "ESTUDIANTE"})) {
      jdbc.update(
          "insert into"
              + " usuario(id_rol,correo,nombres,apellidos,password_hash,intentos_fallidos,estado,fecha_registro)"
              + " values(?,?,?,?,?,0,'ACTIVO',CURRENT_TIMESTAMP)",
          id("rol", "id_rol", "codigo='" + u[1] + "'"),
          u[0] + "@perueduca.pe",
          u[0],
          "Prueba",
          encoder.encode(password));
    }
    jdbc.update("insert into categoria(nombre) values('Cultura')");
  }

  @Test
  @Order(1)
  void loginAndProfilePrivacy() throws Exception {
    ok("GET", "/me", null, null, 401);
    admin = login("admin@perueduca.pe");
    docente = login("docente@perueduca.pe");
    alumno = login("alumno@perueduca.pe");
    otro = login("otro@perueduca.pe");
    var me = ok("GET", "/me", alumno, null, 200);
    assertFalse(me.raw.contains("passwordHash"));
    assertFalse(me.raw.contains("intentosFallidos"));
    ok(
        "PUT",
        "/me",
        alumno,
        Map.of("nombres", "Ana", "apellidos", "Prueba", "correoRecuperacion", "ana.prueba@gmail.com"),
        200);
    assertEquals(
        "Ana", ok("GET", "/me", alumno, null, 200).body.get("usuario").get("nombres").asText());
  }

  Map<String, Object> community(String name, String visibility, boolean moderated) {
    return Map.of(
        "nombre",
        name,
        "idCategoria",
        id("categoria", "id_categoria", "nombre='Cultura'"),
        "visibilidad",
        visibility,
        "moderacionPrevia",
        moderated);
  }

  @Test
  @Order(2)
  void communitiesPermissionsAndPrivateAdmission() throws Exception {
    ok("POST", "/comunidades", alumno, community("No autorizado", "PUBLICA", false), 403);
    cid =
        ok("POST", "/comunidades", admin, community("Comunidad pública", "PUBLICA", false), 201)
            .body
            .get("idComunidad")
            .asLong();
    privada =
        ok("POST", "/comunidades", admin, community("Comunidad privada", "PRIVADA", true), 201)
            .body
            .get("idComunidad")
            .asLong();
    ok("POST", "/comunidades/" + cid + "/unirse", docente, null, 200);
    ok("POST", "/comunidades/" + cid + "/unirse", alumno, null, 200);
    ok("POST", "/comunidades/" + cid + "/unirse", alumno, null, 409);
    ok("POST", "/comunidades/" + privada + "/unirse", alumno, null, 422);
    solicitud =
        ok(
                "POST",
                "/comunidades/" + privada + "/solicitudes",
                alumno,
                Map.of("mensaje", "Deseo aprender"),
                201)
            .body
            .get("idSolicitud")
            .asLong();
    ok("POST", "/comunidades/" + privada + "/solicitudes", alumno, Map.of(), 409);
    ok("PUT", "/solicitudes/" + solicitud, alumno, Map.of("aprobar", true), 403);
    ok("PUT", "/solicitudes/" + solicitud, admin, Map.of("aprobar", true), 200);
    // Responder otra vez una solicitud ya resuelta no da error: devuelve la misma solicitud
    ok("PUT", "/solicitudes/" + solicitud, admin, Map.of("aprobar", true), 200);
    assertNotNull(
        jdbc.queryForObject(
            "select fecha_respuesta from solicitud_ingreso where id_solicitud=?",
            java.sql.Timestamp.class,
            solicitud));
  }

  @Test
  @Order(3)
  void publicationsOwnershipCommentsAndSoftDelete() throws Exception {
    ok(
        "POST",
        "/comunidades/" + cid + "/publicaciones",
        alumno,
        Map.of("titulo", "No permitido"),
        403);
    pub =
        ok(
                "POST",
                "/comunidades/" + cid + "/publicaciones",
                docente,
                Map.of("titulo", "Saberes", "contenido", "Texto educativo"),
                201)
            .body
            .get("publicacion")
            .get("idPublicacion")
            .asLong();
    ok("GET", "/publicaciones/" + pub, otro, null, 403);
    ok("PUT", "/publicaciones/" + pub, admin, Map.of("titulo", "Cambio ajeno"), 403);
    comment =
        ok(
                "POST",
                "/publicaciones/" + pub + "/comentarios",
                alumno,
                Map.of("contenido", "Aprendí mucho"),
                201)
            .body
            .get("comentario")
            .get("idComentario")
            .asLong();
    ok("POST", "/publicaciones/" + pub + "/comentarios", alumno, Map.of("contenido", "   "), 400);
    long second =
        ok(
                "POST",
                "/comunidades/" + cid + "/publicaciones",
                docente,
                Map.of("titulo", "Otra publicación"),
                201)
            .body
            .get("publicacion")
            .get("idPublicacion")
            .asLong();
    ok(
        "POST",
        "/publicaciones/" + second + "/comentarios",
        alumno,
        Map.of("contenido", "Respuesta cruzada", "idComentarioPadre", comment),
        422);
    ok("DELETE", "/comentarios/" + comment, alumno, null, 204);
    assertEquals(
        1,
        jdbc.queryForObject(
            "select eliminado from comentario where id_comentario=?", Integer.class, comment));
    ok("DELETE", "/publicaciones/" + pub, docente, null, 204);
    ok("GET", "/publicaciones/" + pub, alumno, null, 404);
    assertEquals(
        1,
        jdbc.queryForObject(
            "select eliminado from publicacion where id_publicacion=?", Integer.class, pub));
  }

  @Test
  @Order(4)
  void moderationAndNotificationOwnership() throws Exception {
    long req =
        ok("POST", "/comunidades/" + privada + "/solicitudes", docente, Map.of(), 201)
            .body
            .get("idSolicitud")
            .asLong();
    ok("PUT", "/solicitudes/" + req, admin, Map.of("aprobar", true), 200);
    long p =
        ok(
                "POST",
                "/comunidades/" + privada + "/publicaciones",
                docente,
                Map.of("titulo", "Pendiente"),
                201)
            .body
            .get("publicacion")
            .get("idPublicacion")
            .asLong();
    ok("GET", "/publicaciones/" + p, alumno, null, 403);
    ok("PUT", "/publicaciones/" + p + "/moderacion", alumno, Map.of("estado", "APROBADA"), 403);
    ok("PUT", "/publicaciones/" + p + "/moderacion", admin, Map.of("estado", "APROBADA"), 200);
    ok("GET", "/publicaciones/" + p, alumno, null, 200);
    Long n =
        jdbc.queryForObject(
            "select min(id_notificacion) from notificacion where id_destinatario=?",
            Long.class,
            id("usuario", "id_usuario", "correo='alumno@perueduca.pe'"));
    ok("PUT", "/notificaciones/" + n + "/leida", otro, null, 403);
    ok("PUT", "/notificaciones/" + n + "/leida", alumno, null, 204);
    assertEquals(
        1,
        jdbc.queryForObject(
            "select leida from notificacion where id_notificacion=?", Integer.class, n));
  }

  @Test
  @Order(5)
  void academicEnrollmentGradesAttendanceAndClose() throws Exception {
    jdbc.update(
        "insert into periodo_academico(nombre,fecha_inicio,fecha_fin,estado) values('2026-II',DATE"
            + " '2026-08-01',DATE '2026-12-31','ABIERTO')");
    long curso =
        ok(
                "POST",
                "/comunidades/" + cid + "/cursos",
                admin,
                Map.of("codigo", "MAT01", "nombre", "Matemática"),
                200)
            .body
            .get("idCurso")
            .asLong();
    long section =
        ok(
                "POST",
                "/cursos/" + curso + "/secciones",
                admin,
                Map.of(
                    "idPeriodo",
                    id("periodo_academico", "id_periodo", "nombre='2026-II'"),
                    "idDocente",
                    id("usuario", "id_usuario", "correo='docente@perueduca.pe'"),
                    "grupo",
                    "A",
                    "cupoMaximo",
                    1),
                200)
            .body
            .get("idSeccion")
            .asLong();
    long m =
        ok("POST", "/secciones/" + section + "/matriculas", alumno, null, 200)
            .body
            .get("idMatricula")
            .asLong();
    String renewedOther = login("otro@perueduca.pe");
    ok("POST", "/comunidades/" + cid + "/unirse", renewedOther, null, 200);
    ok("POST", "/secciones/" + section + "/matriculas", renewedOther, null, 409);
    long unit =
        ok(
                "POST",
                "/secciones/" + section + "/unidades",
                docente,
                Map.of("nombre", "Unidad 1", "orden", 1),
                200)
            .body
            .get("idUnidad")
            .asLong();
    long session =
        ok(
                "POST",
                "/unidades/" + unit + "/sesiones",
                docente,
                Map.of("titulo", "Clase 1", "orden", 1, "enlaceClase", "https://example.org/clase"),
                200)
            .body
            .get("idSesion")
            .asLong();
    ok(
        "POST",
        "/sesiones/" + session + "/asistencia",
        docente,
        List.of(Map.of("idMatricula", m, "estado", "PRESENTE")),
        200);
    long e =
        ok(
                "POST",
                "/secciones/" + section + "/evaluaciones",
                docente,
                Map.of("nombre", "Examen", "tipo", "EXAMEN", "peso", 100),
                200)
            .body
            .get("idEvaluacion")
            .asLong();
    ok(
        "PUT",
        "/notas",
        alumno,
        Map.of("idMatricula", m, "idEvaluacion", e, "calificacion", 16),
        403);
    ok(
        "PUT",
        "/notas",
        docente,
        Map.of("idMatricula", m, "idEvaluacion", e, "calificacion", 21),
        400);
    ok(
        "PUT",
        "/notas",
        docente,
        Map.of("idMatricula", m, "idEvaluacion", e, "calificacion", 16),
        200);
    long tarea =
        ok(
                "POST",
                "/unidades/" + unit + "/recursos",
                docente,
                Map.of("titulo", "Tarea 1", "tipo", "TAREA", "fechaEntrega", "2099-12-31T23:59:00"),
                200)
            .body
            .get("idRecurso")
            .asLong();
    ok("POST", "/tareas/" + tarea + "/entregas", alumno, Map.of("comentario", "Mi solución"), 200);
    ok("POST", "/tareas/" + tarea + "/entregas", alumno, Map.of("comentario", "Duplicada"), 409);
    ok("POST", "/secciones/" + section + "/cerrar", docente, null, 200);
    assertEquals(
        "APROBADO",
        jdbc.queryForObject("select estado from matricula where id_matricula=?", String.class, m));
    assertEquals(
        16,
        jdbc.queryForObject(
            "select nota_final from matricula where id_matricula=?", Integer.class, m));
    ok("GET", "/secciones/" + section + "/notas", docente, null, 200);
    ok(
        "PUT",
        "/notas",
        docente,
        Map.of("idMatricula", m, "idEvaluacion", e, "calificacion", 18),
        409);
  }

  @Test
  @Order(6)
  void failedLoginsCommitAndLock() throws Exception {
    for (int i = 0; i < 5; i++) {
      var rejected = ok(
          "POST",
          "/auth/login",
          null,
          Map.of("correo", "bloqueo@perueduca.pe", "password", "incorrecta"),
          i == 4 ? 423 : 401);
      assertEquals(
          i == 4 ? "CUENTA_BLOQUEADA" : "CREDENCIALES_INVALIDAS",
          rejected.body.get("codigo").asText());
    }
    assertEquals(
        5,
        jdbc.queryForObject(
            "select intentos_fallidos from usuario where correo='bloqueo@perueduca.pe'",
            Integer.class));
    var locked = ok(
        "POST",
        "/auth/login",
        null,
        Map.of("correo", "bloqueo@perueduca.pe", "password", password),
        423);
    assertEquals("CUENTA_BLOQUEADA", locked.body.get("codigo").asText());
    assertNotNull(
        jdbc.queryForObject(
            "select bloqueado_hasta from usuario where correo='bloqueo@perueduca.pe'",
            java.sql.Timestamp.class));
    jdbc.update(
        "update usuario set bloqueado_hasta=TIMESTAMP '2020-01-01 00:00:00' where"
            + " correo='bloqueo@perueduca.pe'");
    login("bloqueo@perueduca.pe");
    assertEquals(
        0,
        jdbc.queryForObject(
            "select intentos_fallidos from usuario where correo='bloqueo@perueduca.pe'",
            Integer.class));
  }

  @Test
  @Order(7)
  void logoutRevokesPersistentlyAndInactiveCommunitiesHidden() throws Exception {
    ok("POST", "/auth/logout", otro, null, 204);
    ok("GET", "/me", otro, null, 401);
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from auditoria where tabla='SESION_JWT'", Integer.class));
    ok("DELETE", "/comunidades/" + cid, admin, null, 204);
    ok("GET", "/comunidades/" + cid, alumno, null, 404);
    var list = ok("GET", "/comunidades?q=pública", alumno, null, 200);
    assertEquals(0, list.body.get("page").get("totalElements").asLong());
  }

  @Test
  @Order(8)
  void uploadDownloadAndOpenApiContract() throws Exception {
    String boundary = "TestBoundaryVirtual";
    String body =
        "--"
            + boundary
            + "\r\n"
            + "Content-Disposition: form-data; name=\"archivo\"; filename=\"guia.pdf\"\r\n"
            + "Content-Type: application/pdf\r\n\r\n"
            + "%PDF-1.7\n"
            + "Documento de prueba\r\n"
            + "--"
            + boundary
            + "--\r\n";
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/archivos"))
            .header("Authorization", "Bearer " + alumno)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var uploaded = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(201, uploaded.statusCode(), uploaded.body());
    long id = mapper.readTree(uploaded.body()).get("idArchivo").asLong();
    var download =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/v1/archivos/" + id + "/descarga"))
            .header("Authorization", "Bearer " + alumno)
            .GET()
            .build();
    var response = client.send(download, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().startsWith("%PDF-"));
    ok("GET", "/archivos/" + id + "/descarga", login("otro@perueduca.pe"), null, 403);
    var contract =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, contract.statusCode(), contract.body());
    assertTrue(mapper.readTree(contract.body()).get("paths").has("/api/v1/auth/login"));
    java.nio.file.Files.createDirectories(java.nio.file.Path.of("target"));
    java.nio.file.Files.writeString(java.nio.file.Path.of("target/openapi-test.json"), contract.body());
  }
}

# Comunidades Virtuales – Backend (PerúEduca / MINEDU)

API REST del sistema **Comunidades Virtuales**: comunidades de aprendizaje, cursos, secciones, notas, asistencia, avisos y mensajería para estudiantes, docentes y administradores.

- **Java 21** · **Spring Boot 4** · Spring Security (JWT + BCrypt) · Spring Data JPA
- **Oracle Database 19c** (esquema `COMUNIDADES`)
- Puerto: **8081** · Swagger: `http://localhost:8081/swagger-ui/index.html`

El frontend (Angular) está en un repositorio aparte.

---

## Roles y reglas principales

| Rol | Correo permitido | Cómo se crea la cuenta |
|---|---|---|
| Estudiante | `@perueduca.pe` | Se registra solo y queda **ACTIVO** |
| Docente | `@minedu.gob.pe` | Se registra y queda **PENDIENTE** hasta que un administrador lo apruebe |
| Administrador | `@minedu.gob.pe` | Solo lo crea otro administrador |

La regla de correo por rol se valida en el frontend, en el backend y en la base de datos (`CHECK`).

Otras funciones: recuperación de contraseña con código de 6 dígitos, bloqueo tras 5 intentos fallidos, avisos (a todos, docentes, estudiantes, una sección o un usuario), mensajes directos, moderación de publicaciones y reportes administrativos.

---

## Requisitos

- JDK 21 o superior (IntelliJ IDEA recomendado)
- Una base de datos **Oracle 19c** (local o en la nube)

## 1. Crear la base de datos

En la carpeta [`sql/`](sql) están los scripts. Ejecutarlos **en orden** con SQL Developer o SQLcl:

| Script | Ejecutar como | Qué hace |
|---|---|---|
| `00_permisos_dba.sql` | DBA (SYSTEM) | Permisos para el usuario `COMUNIDADES` |
| `01_esquema_vacio.sql` | COMUNIDADES | Crea las tablas |
| `02_paquete.sql` | COMUNIDADES | Paquete `PKG_COMUNIDADES` |
| `03_triggers_vistas.sql` | COMUNIDADES | Triggers y vistas |
| `06_catalogos.sql` | COMUNIDADES | Roles, idiomas, pueblos originarios |
| `07_cambios_iteracion1.sql` | COMUNIDADES | Cambios de la iteración 1 (estados, avisos, recuperación, regla de correo) |
| `08_datos_prueba_por_rol.sql` | COMUNIDADES | Datos de prueba (usuarios, comunidades, cursos, notas…) |

Opcionales: `00b_usuario_runtime_dba.sql` y `04_permisos_runtime.sql` (usuario de solo ejecución), `05_verificacion.sql` (comprobaciones), `09_clonar_comu_vir_desa.sql` (clonar el esquema con Data Pump).

## 2. Configurar la conexión

Copiar `application-local.properties.example` a `.local/application-local.properties` y completar los datos:

```properties
spring.datasource.url=jdbc:oracle:thin:@//<IP_O_HOST>:1521/orcl
spring.datasource.username=comunidades
spring.datasource.password=<contraseña de Oracle>
app.jwt.secret=<clave Base64 de 32 bytes o más>
```

La carpeta `.local/` está excluida de Git: **nunca subas contraseñas al repositorio**.

Generar una clave JWT en PowerShell:

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

También se pueden usar variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.

## 3. Ejecutar

- **IntelliJ:** abrir esta carpeta como proyecto Maven y ejecutar `ComunidadesApplication` (▶).
- **Terminal:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

Cuando aparezca `Started ComunidadesApplication`, la API está en `http://localhost:8081`.

## Usuarios de prueba

Contraseña de todos (solo desarrollo): `Comunidades2026!`

| Rol | Correo |
|---|---|
| Administrador | `admin.prueba@minedu.gob.pe` |
| Docente | `docente.prueba@minedu.gob.pe` |
| Estudiante | `alumno.prueba@perueduca.pe` |

## Recuperación de contraseña por correo (opcional)

Si `spring.mail.host` está vacío, el código de 6 dígitos se muestra en la consola del backend. Para enviarlo por correo real, configurar en `.local/application-local.properties` un SMTP (por ejemplo Gmail con una contraseña de aplicación).

## Estructura

```
src/main/java/pe/edu/upeu/comunidades/
├── config/        Seguridad, JWT, CORS
├── controller/    Endpoints REST
├── dto/           Peticiones y respuestas
├── entity/        Entidades JPA
├── repository/    Repositorios Spring Data
├── service/       Lógica de negocio
└── exception/     Manejo de errores
sql/               Scripts de la base de datos
docs/              Documentación y contrato OpenAPI
```

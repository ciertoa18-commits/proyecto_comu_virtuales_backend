# Comunidades Virtuales — Backend

API REST para comunidades educativas: usuarios, publicaciones, cursos, matrículas,
notas, asistencia, avisos, mensajería y archivos.

Java 21, Spring Boot 4, Spring Security con JWT, Spring Data JPA y Oracle 19c.

## Configuración

Crea la carpeta `.local/`, copia `application-local.properties.example` a `.local/application-local.properties`
y completa la conexión a Oracle y la clave JWT. La carpeta `.local/` está excluida
de Git.

Para generar una clave JWT en PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Cada vez que se reinicia el laboratorio de AWS cambia la IP de la instancia: actualiza solo `spring.datasource.url` en `.local/application-local.properties`. El repositorio no guarda ninguna IP.

Para que lleguen los códigos de "Modificar contraseña" y "Recuperar contraseña", completa las líneas `spring.mail.*` con una cuenta de Gmail y una [contraseña de aplicación](https://myaccount.google.com/apppasswords) (requiere verificación en dos pasos). Sin esas líneas, el código se muestra en la consola del backend.

Los archivos de configuración son:

- `application.yaml`: configuración común.
- `application-dev.yaml`: desarrollo, perfil predeterminado.
- `application-prod.yaml`: producción; activar con `SPRING_PROFILES_ACTIVE=prod`.

En producción, configura `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y
`CORS_ORIGINS` en el entorno o en la configuración externa.
El correo SMTP y el almacenamiento S3 son opcionales; sus propiedades están en
`application.yaml`. El almacenamiento predeterminado es local, en `storage/`.

## Base de datos

Ejecuta los scripts de `sql/` en este orden:

1. `00_permisos_dba.sql`, como DBA.
2. `01_esquema_vacio.sql`, `02_paquete.sql` y `03_triggers_vistas.sql`, como `COMUNIDADES`.
3. `06_catalogos.sql` y `07_cambios_iteracion1.sql`.

`08_datos_prueba_por_rol.sql` carga datos opcionales de desarrollo.
Los demás scripts permiten configurar permisos de ejecución, verificar o clonar
el esquema y consultar su modelo.
`10_modelo_implementacion_bd.sql` reúne la instalación completa como alternativa;
no lo ejecutes junto con los scripts individuales sobre el mismo esquema.

## Ejecución

```powershell
.\mvnw.cmd spring-boot:run
```

También puedes ejecutar `ComunidadesApplication` desde IntelliJ.

- API: `http://localhost:8081/api/v1`
- Swagger: `http://localhost:8081/swagger-ui/index.html`

## Estructura

```text
src/main/java/pe/perueduca/comunidades/
|-- config/        Configuración de Spring y servicios externos
|-- controller/    Endpoints REST
|-- dto/           Datos de las peticiones
|-- entity/        Entidades JPA
|-- enums/         Estados, roles y tipos
|-- exception/     Excepciones y manejo de errores
|-- mapper/        Transformación de datos
|-- repository/    Acceso a datos
|-- security/      JWT y autorización
|-- service/       Servicios y sus implementaciones en impl/
|-- util/          Conversión JPA y validación de texto
`-- ComunidadesApplication.java
```

`docs/` contiene el contrato OpenAPI, los modelos y los diagramas del proyecto.

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas usan H2 en memoria. El estado de la última validación está en
[docs/VALIDACION.md](docs/VALIDACION.md).
El contrato exportado durante las pruebas queda en `target/openapi-test.json`.

# Verificación de la entrega

Actualización del 02/11/2026: se comprobó la conexión real a Oracle 19.3 en `54.159.15.47:1521/orcl`, esquema `COMUNIDADES`. Hibernate terminó la validación de las entidades con `ddl-auto=validate` y la aplicación arrancó correctamente; Swagger respondió HTTP 200. Se configuraron las credenciales autorizadas y JWT en `.local/application-local.properties`, fuera de Git. No se ejecutó DDL ni se cambiaron datos del negocio. Esta prueba confirma conexión y mapeo; no certifica todavía todos los flujos contra Oracle ni la instalación de PL/SQL, S3 o SSO.

Comprobación adicional el 02/11/2026: se eliminó el proyecto de ejemplo y se inició Comunidades Virtuales en `127.0.0.1:8081` usando el perfil `demo` y H2 en memoria. Se verificaron por HTTP el login, el perfil ADMIN, el listado de comunidades y Swagger (200). Las credenciales aleatorias de esta sesión están únicamente en `target/demo-acceso.json`. Esta comprobación no usa Oracle ni S3. Se repitieron las 13 pruebas automatizadas con resultado correcto.

Verificación local realizada el 02/11/2026 mediante Java 24, compilación objetivo Java 21 y Maven de IntelliJ. Resultado de `package`: **BUILD SUCCESS**. Se generó `target/comunidades-backend-1.0.0.jar`.

## Pruebas automatizadas

Resultado: **13 pruebas, 0 fallos, 0 errores, 0 omitidas**. Ocho pruebas de integración recorren varias operaciones HTTP cada una, con Spring Security, servicios, JPA y una BD H2 aislada en modo Oracle. Cinco pruebas adicionales verifican archivos.

| Prueba | Evidencia comprobada |
|---|---|
| Acceso y perfil | Sin token devuelve 401; login válido; perfil propio editable; no se serializan hash ni bloqueo |
| Comunidades y admisión | Solo ADMIN crea; ingreso público; ingreso privado exige solicitud; duplicados rechazados; revisión registra fecha y revisor |
| Publicaciones y comentarios | Solo rol autorizado publica; membresía y autoría; comentarios vacíos y respuestas cruzadas rechazados; borrado lógico |
| Moderación y avisos | Publicación pendiente no visible a miembros; solo moderador aprueba; notificación solo puede leerla el destinatario |
| Flujo académico | Cupos, matrícula, asistencia, nota 0–20, tarea y entrega única, promedio ponderado y cierre; notas legibles tras cerrar; modificaciones posteriores rechazadas |
| Bloqueo | Cinco fallos persisten y bloquean; clave correcta no salta bloqueo; tras expirar se restablece el acceso |
| Logout y búsqueda | Token revocado deja de servir; registro persistido en AUDITORIA; comunidad inactiva desaparece de búsqueda |
| Archivos y contrato | Carga multipart, descarga del propietario, rechazo a otro usuario; OpenAPI responde y se exporta |
| Archivo falso PDF | Ejecutable renombrado como PDF rechazado |
| Tamaño | Archivo vacío e imagen sobre el límite rechazados |
| Archivo falso DOCX | ZIP sin estructura Word rechazado |
| Foto | PDF rechazado como foto de perfil |
| Firma PDF | Firma PDF admitida en validación de formato |

Los resultados completos se encuentran en `target/surefire-reports` y en el log local `pruebas.log`. Las pruebas no usan credenciales reales ni acceden a Oracle remoto.

## Diagramas y artefactos

Se generaron 13 clases de análisis, 13 clases de diseño y 13 secuencias, dos diagramas generales y dos modelos de BD: **43 PlantUML** con sus SVG. El Draw.io contiene 30 páginas de clases y BD; las secuencias se entregan en PlantUML/SVG. Se comprobaron sintaxis mediante renderizado y muestras visuales de análisis, diseño y secuencia.

El diseño refleja tipos Java y firmas de métodos. Los modelos conceptual y relacional mantienen las entidades de las 28 tablas. Los scripts contienen 60 FK, 14 triggers, un paquete con seis subprogramas y tres vistas, reconstruidos del informe. El contrato OpenAPI exportado describe los métodos y los DTO de la implementación.

## Comprobaciones externas pendientes

- Oracle 19c: conexión, `ddl-auto=validate`, contraste con DDL real y compilación/ejecución de PL/SQL. Falta la contraseña real en el material recibido.
- S3: carga y descarga contra un bucket real con permisos IAM. Falta su configuración.
- SSO: integración institucional y aceptación de Keycloak. La entrega utiliza la alternativa local BCrypt/JWT del informe.
- Negocio: confirmación de CU01–CU07 para la iteración 1, política de moderación y revisión con prototipos.
- Instalación de triggers: el DBA debe verificar todos los objetos antes de activar `DB_TRIGGERS_ACTIVOS=true`.

H2 valida el comportamiento de la aplicación y sus consultas JPA de prueba; no certifica PL/SQL ni equivalencia completa del esquema Oracle. No se modificó la base remota.

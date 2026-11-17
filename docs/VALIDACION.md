# Verificación del proyecto

Última comprobación: 06/11/2026. El código compila y la aplicación arranca con
los perfiles YAML. Las pruebas usan H2 en memoria y no acceden a Oracle remoto.

## Pruebas automatizadas

Resultado: **13 pruebas, 0 fallos, 0 errores, 0 omitidas**. Se corrigió el permiso de creación de publicaciones educativas (DOCENTE o ADMIN) y se verificó HTTP 423 al alcanzar cinco intentos fallidos y mientras la cuenta permanece bloqueada. Ocho pruebas de integración recorren varias operaciones HTTP cada una, con Spring Security, servicios, JPA y una BD H2 aislada en modo Oracle. Cinco pruebas adicionales verifican archivos.

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

Al ejecutar las pruebas, los resultados se generan en `target/surefire-reports`. Las pruebas no usan credenciales reales ni acceden a Oracle remoto.

## Diagramas y artefactos

Se generaron 13 clases de análisis, 13 clases de diseño y 13 secuencias, dos diagramas generales y dos modelos de BD: **43 PlantUML** con sus SVG. El Draw.io contiene 30 páginas de clases y BD; las secuencias se entregan en PlantUML/SVG. Se comprobaron sintaxis mediante renderizado y muestras visuales de análisis, diseño y secuencia.

El diseño refleja tipos Java y firmas de métodos. Los modelos conceptual y relacional mantienen las entidades de las 28 tablas. Los scripts contienen 60 FK, 14 triggers, un paquete con seis subprogramas y tres vistas, reconstruidos del informe. El contrato OpenAPI exportado describe los métodos y los DTO de la implementación. Las pruebas generan su copia en `target/openapi-test.json` y no sobrescriben `docs/openapi.json`.

## Comprobaciones externas pendientes

- Oracle 19c: comprobar conexión, `ddl-auto=validate`, contraste con DDL real y compilación/ejecución de PL/SQL en el entorno de destino. Esta ejecución de pruebas solo utiliza H2.
- S3: carga y descarga contra un bucket real con permisos IAM. Falta su configuración.
- SSO: integración institucional y aceptación de Keycloak. La entrega utiliza la alternativa local BCrypt/JWT del informe.
- Negocio: confirmación de CU01–CU07 para la iteración 1, política de moderación y revisión con prototipos.
- Instalación de triggers: el DBA debe verificar todos los objetos antes de activar `DB_TRIGGERS_ACTIVOS=true`.

H2 valida el comportamiento de la aplicación y sus consultas JPA de prueba; no certifica PL/SQL ni equivalencia completa del esquema Oracle. No se modificó la base remota.

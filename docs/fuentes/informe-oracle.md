# Informe técnico para construir el backend – PerúEduca «Comunidades Virtuales»

> **Para quien lea esto (persona o IA):** este documento describe la base de datos Oracle **ya creada y probada** del proyecto. Tu tarea es construir el **backend** sobre ella **sin cambiar el modelo de datos**. Si algo no cuadra, pregunta antes de modificar tablas.

Fecha: 2026-11-01 (actualizado 23:30) · Autor del modelo: equipo del proyecto (Mendoza, Moran, Reynoso, Cierto, Osorio).

---

## 1. Contexto del proyecto

- **Cliente:** Ministerio de Educación del Perú (MINEDU) / PerúEduca.
- **Problema:** estudiantes de comunidades nativas tienen poco acceso a material educativo en su idioma y poca interacción con docentes.
- **Solución:** plataforma web donde se crean **comunidades** (públicas o privadas, por pueblo originario o idioma), con **cursos**, **recursos educativos**, **publicaciones y foros**, **notas, asistencia y tareas**, **notificaciones** y **mensajería**.
- **Fuera de alcance (v1):** clases en vivo propias (solo se publica el enlace), app móvil, integración con otros sistemas del Estado.
- **Roles globales:** `ESTUDIANTE`, `DOCENTE`, `ADMIN`.
- **Roles dentro de una comunidad:** `MIEMBRO`, `MODERADOR`, `ADMINISTRADOR`.
- **Requerimientos de origen:** Excel «Plantilla de requerimientos V3» (RF01–RF55, RN01–RN26, RES01–RES24) y el Brief del proyecto. En este informe se citan por código.

## 2. Stack recomendado

Tomado del benchmarking del Excel (Java + Oracle + VS Code, SSO institucional):

| Capa | Recomendación |
|---|---|
| Lenguaje / framework | **Java 17+ con Spring Boot 3** (Web, Validation, Data JPA, Security) |
| Base de datos | **Oracle 19c** (driver `ojdbc11`), dialecto Hibernate Oracle |
| Autenticación | **Keycloak (SSO, RES10)** como OAuth2/OIDC → backend como *Resource Server* JWT. Alternativa sin SSO: login propio con BCrypt (RF53) |
| Archivos | **Amazon S3** (la BD está en AWS). En la BD solo se guarda la ruta/key (`archivo.ruta_almacenamiento`) |
| Documentación API | springdoc-openapi (Swagger) |
| Migraciones | Opcional: Flyway/Liquibase con los scripts 01–02 como línea base |

## 3. Conexión a la base de datos

| Dato | Valor |
|---|---|
| Motor | Oracle Database 19c (19.3) en AWS |
| Host | `34.224.28.94` |
| Puerto | `1521` |
| Servicio | `orcl` |
| Usuario / esquema | `comunidades` (dueño de las tablas) |
| Contraseña | **`ESCRIBE_AQUI_TU_CONTRASEÑA`** (la misma de la conexión `comunidades_dev` de SQL Developer) |
| Tablespace | `TS_COMUNIDADES` |
| Driver | `com.oracle.database.jdbc:ojdbc11` |

`application.properties` listo para copiar (reemplaza la contraseña):

```properties
spring.datasource.url=jdbc:oracle:thin:@//34.224.28.94:1521/orcl
spring.datasource.username=comunidades
spring.datasource.password=ESCRIBE_AQUI_TU_CONTRASEÑA
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.time_zone=America/Lima
spring.jpa.show-sql=true
```

Dependencia Maven:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc11</artifactId>
  <scope>runtime</scope>
</dependency>
```

Requisitos para que conecte:

- `ddl-auto=validate`: **nunca** `create` ni `update`, porque el esquema ya existe y Hibernate lo rompería.
- Si el backend corre en tu misma PC, conecta tal cual (tu PC ya llega a la BD). Si corre en otro servidor, abre el puerto **1521** para la IP de ese servidor en el *Security Group* de la instancia AWS.
- No subas la contraseña a GitHub: en producción usa una variable de entorno (`spring.datasource.password=${DB_PASSWORD}`).
- Recomendado a futuro: un usuario `APP_COMUNIDADES` solo con permisos de datos (sección 1 del archivo SQL).

## 4. Estado actual de la base de datos

| Elemento | Estado |
|---|---|
| 28 tablas, 60 FK, checks, 99 índices | ✅ Creado y probado |
| Catálogos (roles, idiomas, pueblos) y datos de prueba | ✅ Cargados |
| 14 triggers, paquete `PKG_COMUNIDADES`, 3 vistas | ⏳ **Pendiente.** Código listo (secciones 5–7 de `comunidades_virtuales_completo.sql`); el paquete y las vistas se probaron en la BD sin guardarlos. Falta que el DBA dé `CREATE TRIGGER`, `CREATE PROCEDURE`, `CREATE VIEW` (sección 1) |
| Tablas antiguas (24) | Eliminadas (estaban vacías). Respaldo en `respaldo_tablas_antiguas.sql` |

**Importante para el backend:** mientras el script 02 no esté instalado, el backend debe validar él mismo las reglas de la sección 7 marcadas como «trigger». Cuando se instale, esas validaciones quedan duplicadas (no es un problema), pero **las notificaciones automáticas sí se duplicarían**: el backend **no** debe insertar notificaciones de los tipos que generan los triggers (sección 8.1) una vez instalado el 02. Recomendación: una propiedad `app.db.triggers-activos=true/false`.

Todo el código está en **`comunidades_virtuales_completo.sql`**, en este orden:

1. Tablespace y permisos (lo ejecuta el administrador).
2. Tablas (**ya creadas**).
3. Índices (**ya creados**).
4. Catálogos: roles, idiomas, pueblos (**ya cargados**).
5. Triggers (**pendiente**).
6. Paquete `PKG_COMUNIDADES` (**pendiente**).
7. Vistas (**pendiente**).
8. 14 consultas de negocio C01–C14, probadas (base para los endpoints de reportes).
9. Datos de prueba (**ya cargados**).

## 4b. Historial de lo realizado en la base de datos

1. Se revisó el esquema original (24 tablas, vacías) contra el Brief, la plantilla de requerimientos (RF01–RF55, RN01–RN26, RES01–RES24) y las notas del equipo.
2. Se diseñó un modelo nuevo de **28 tablas**: catálogos, usuarios, archivos, comunidades, estructura académica (curso → sección → unidad → sesión → recurso), matrícula, evaluaciones, notas, asistencia, tareas, publicaciones, comentarios, notificaciones, mensajes y auditoría.
3. Se respaldó la estructura de las 24 tablas antiguas y se eliminaron; quedan en la papelera de Oracle (`FLASHBACK TABLE <tabla> TO BEFORE DROP`).
4. Se crearon las 28 tablas, con 60 FK, restricciones CHECK y UNIQUE, y 99 índices, en el tablespace `TS_COMUNIDADES`.
5. Se cargaron los catálogos (roles, 6 idiomas, 5 pueblos originarios) y los datos de prueba.
6. **Pruebas:** 16 inserciones inválidas fueron rechazadas por la BD. Se encontró y corrigió un error: los comentarios y mensajes formados solo por espacios se aceptaban; ahora la restricción es `TRIM(contenido) IS NOT NULL`.
7. Se escribieron 14 triggers, el paquete `PKG_COMUNIDADES` (6 subprogramas) y 3 vistas. El paquete y las vistas se probaron en la BD sin guardarlos. **Falta instalarlos:** el usuario `COMUNIDADES` solo tiene `CREATE TABLE`, `CREATE SEQUENCE` y `CREATE SESSION`.
8. Se escribieron 14 consultas de negocio (C01–C14), probadas con los datos de prueba.
9. Se generaron en **SQL Developer Data Modeler** (importación desde el diccionario de datos: 28 tablas, 0 errores) el **modelo físico/relacional** y, por ingeniería inversa, el **modelo lógico**. Están en `modelo_fisico_comunidades.png` y `modelo_logico_comunidades.png`.

## 5. Diagrama de módulos

```
USUARIO ─┬─ ROL
         ├─ ARCHIVO (foto, adjuntos, recursos, entregas)
         │
COMUNIDAD (CATEGORIA, IDIOMA, PUEBLO_ORIGINARIO)
 ├─ MIEMBRO_COMUNIDAD (usuario N:M comunidad)
 ├─ SOLICITUD_INGRESO
 ├─ PUBLICACION ─ PUBLICACION_ARCHIVO
 │      └─ COMENTARIO (también en RECURSO; respuestas con id_comentario_padre)
 ├─ EVENTO
 └─ CURSO
      └─ SECCION (curso + PERIODO_ACADEMICO + grupo + docente)
           ├─ UNIDAD ─ SESION ─ ASISTENCIA
           │     └─ RECURSO ─ TAREA (subtipo 1:1) ─ ENTREGA_TAREA
           ├─ EVALUACION ─ NOTA
           └─ MATRICULA (alumno en la sección) ─ NOTA / ASISTENCIA / ENTREGA_TAREA

NOTIFICACION (una fila por destinatario) · MENSAJE (directo emisor→receptor) · AUDITORIA
```

Idea clave: **CURSO** es la materia; **SECCION** es esa materia dictada en un periodo y grupo concretos. Matrícula, notas, asistencia, unidades y sesiones cuelgan de la **sección**, para no mezclar periodos.

## 6. Diccionario de datos (28 tablas)

Convenciones comunes:

- PK `id_<tabla>` = `NUMBER GENERATED ALWAYS AS IDENTITY` → en JPA `@GeneratedValue(strategy = GenerationType.IDENTITY)`. **No enviar el id en INSERT.**
- Booleanos = `NUMBER(1)` con 0/1 → en JPA usar `Boolean` con un `AttributeConverter<Boolean,Integer>` (o `@JdbcTypeCode(SqlTypes.INTEGER)`).
- Estados = `VARCHAR2` con CHECK → en Java usar `enum` con `@Enumerated(EnumType.STRING)`; los valores exactos están en cada tabla.
- `CLOB` → `@Lob String`.
- Borrado lógico con `eliminado = 1` en `RECURSO`, `PUBLICACION`, `COMENTARIO`, `MENSAJE`: **no hacer DELETE físico**; filtrar `eliminado = 0` en lecturas (Hibernate `@SQLRestriction("eliminado = 0")` / `@SQLDelete`).
- Fechas `TIMESTAMP` con default `SYSTIMESTAMP`: el backend puede omitirlas en el INSERT (usar `@Column(insertable = false, updatable = false)` + `@Generated`, o enviarlas).
- `correo` siempre en minúsculas.

### ROL

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_rol | NUMBER (IDENTITY) | NO | PK autogenerada |
| codigo | VARCHAR2(20) | NO |  |
| nombre | VARCHAR2(50) | NO |  |

- **PK:** id_rol
- **Únicos:** (codigo)
- **Checks:** `(codigo IN ('ESTUDIANTE','DOCENTE','ADMIN'))`

### CATEGORIA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_categoria | NUMBER (IDENTITY) | NO | PK autogenerada |
| nombre | VARCHAR2(100) | NO |  |
| descripcion | VARCHAR2(500) | sí |  |

- **PK:** id_categoria
- **Únicos:** (nombre)

### IDIOMA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_idioma | NUMBER (IDENTITY) | NO | PK autogenerada |
| codigo | VARCHAR2(10) | NO | ej. 'es', 'qu', 'cni' (asháninka) |
| nombre | VARCHAR2(60) | NO |  |

- **PK:** id_idioma
- **Únicos:** (codigo)

### PUEBLO_ORIGINARIO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_pueblo | NUMBER (IDENTITY) | NO | PK autogenerada |
| nombre | VARCHAR2(100) | NO |  |
| familia_linguistica | VARCHAR2(100) | sí |  |

- **PK:** id_pueblo
- **Únicos:** (nombre)

### USUARIO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_usuario | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_rol | NUMBER | NO |  |
| correo | VARCHAR2(150) | NO |  |
| nombres | VARCHAR2(100) | NO |  |
| apellidos | VARCHAR2(100) | NO |  |
| especialidad | VARCHAR2(100) | sí | solo docentes |
| keycloak_sub | VARCHAR2(64) | sí | id del usuario en el SSO (RES10) |
| password_hash | VARCHAR2(255) | sí | solo si no usa SSO (RF53, RES16) |
| intentos_fallidos | NUMBER(2) | NO | default 0; RF54 |
| bloqueado_hasta | TIMESTAMP | sí | RF55 |
| id_foto_perfil | NUMBER | sí | FK a archivo (se agrega al final) |
| id_idioma_pref | NUMBER | sí |  |
| estado | VARCHAR2(10) | NO | default 'ACTIVO' |
| fecha_registro | TIMESTAMP | NO | default SYSTIMESTAMP |
| fecha_actualizacion | TIMESTAMP | sí |  |

- **PK:** id_usuario
- **Únicos:** (correo); (keycloak_sub)
- **FK:** id_rol → ROL.id_rol; id_idioma_pref → IDIOMA.id_idioma
- **Checks:** `(REGEXP_LIKE(correo, '^[^@ ]+@(minedu\.gob\.pe|perueduca\.pe)$'))` · `(correo = LOWER(correo))` · `(keycloak_sub IS NOT NULL OR password_hash IS NOT NULL)` · `(intentos_fallidos >= 0)` · `(estado IN ('ACTIVO','INACTIVO'))`

### ARCHIVO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_archivo | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_subido_por | NUMBER | NO |  |
| nombre_original | VARCHAR2(255) | NO |  |
| extension | VARCHAR2(10) | NO |  |
| tipo_mime | VARCHAR2(100) | NO |  |
| tamano_bytes | NUMBER | NO |  |
| ruta_almacenamiento | VARCHAR2(500) | NO | ruta o key en el storage, no el binario |
| fecha_subida | TIMESTAMP | NO | default SYSTIMESTAMP |

- **PK:** id_archivo
- **FK:** id_subido_por → USUARIO.id_usuario
- **Checks:** `( (LOWER(extension) IN ('pdf','mp4','docx') AND tamano_bytes <= 52428800) OR (LOWER(extension) IN ('jpg','jpeg','png') AND tamano_bytes <= 2097152) )` · `(tamano_bytes > 0)`

### COMUNIDAD

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_comunidad | NUMBER (IDENTITY) | NO | PK autogenerada |
| nombre | VARCHAR2(150) | NO |  |
| descripcion | VARCHAR2(1000) | sí |  |
| id_categoria | NUMBER | NO | RN21 |
| id_idioma | NUMBER | sí |  |
| id_pueblo | NUMBER | sí |  |
| visibilidad | VARCHAR2(10) | NO | default 'PUBLICA'; RF09, RN04, RN22 |
| moderacion_previa | NUMBER(1) | NO | default 0; RN02 (comunidad "oficial") |
| estado | VARCHAR2(10) | NO | default 'ACTIVA'; RN20 |
| id_imagen | NUMBER | sí | RF18 |
| id_creador | NUMBER | NO |  |
| fecha_creacion | TIMESTAMP | NO | default SYSTIMESTAMP |
| fecha_actualizacion | TIMESTAMP | sí |  |

- **PK:** id_comunidad
- **Únicos:** (nombre)
- **FK:** id_categoria → CATEGORIA.id_categoria; id_idioma → IDIOMA.id_idioma; id_pueblo → PUEBLO_ORIGINARIO.id_pueblo; id_imagen → ARCHIVO.id_archivo; id_creador → USUARIO.id_usuario
- **Checks:** `(visibilidad IN ('PUBLICA','PRIVADA'))` · `(moderacion_previa IN (0,1))` · `(estado IN ('ACTIVA','INACTIVA'))`

### MIEMBRO_COMUNIDAD

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_miembro | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_comunidad | NUMBER | NO |  |
| id_usuario | NUMBER | NO |  |
| rol_comunidad | VARCHAR2(15) | NO | default 'MIEMBRO' |
| estado | VARCHAR2(10) | NO | default 'ACTIVO' |
| fecha_ingreso | TIMESTAMP | NO | default SYSTIMESTAMP |
| fecha_salida | TIMESTAMP | sí |  |

- **PK:** id_miembro
- **Únicos:** (id_comunidad, id_usuario)
- **FK:** id_comunidad → COMUNIDAD.id_comunidad; id_usuario → USUARIO.id_usuario
- **Checks:** `(rol_comunidad IN ('MIEMBRO','MODERADOR','ADMINISTRADOR'))` · `(estado IN ('ACTIVO','RETIRADO','EXPULSADO'))` · `((estado = 'ACTIVO' AND fecha_salida IS NULL) OR (estado <> 'ACTIVO' AND fecha_salida IS NOT NULL))`

### SOLICITUD_INGRESO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_solicitud | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_comunidad | NUMBER | NO |  |
| id_usuario | NUMBER | NO | alumno O docente |
| estado | VARCHAR2(10) | NO | default 'PENDIENTE' |
| mensaje | VARCHAR2(500) | sí |  |
| fecha_solicitud | TIMESTAMP | NO | default SYSTIMESTAMP |
| id_revisor | NUMBER | sí | vacío mientras está pendiente |
| fecha_respuesta | TIMESTAMP | sí |  |
| motivo_rechazo | VARCHAR2(500) | sí |  |

- **PK:** id_solicitud
- **FK:** id_comunidad → COMUNIDAD.id_comunidad; id_usuario → USUARIO.id_usuario; id_revisor → USUARIO.id_usuario
- **Checks:** `(estado IN ('PENDIENTE','APROBADA','RECHAZADA'))` · `( (estado = 'PENDIENTE' AND id_revisor IS NULL AND fecha_respuesta IS NULL) OR (estado <> 'PENDIENTE' AND id_revisor IS NOT NULL AND fecha_respuesta IS NOT NULL))`

### PERIODO_ACADEMICO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_periodo | NUMBER (IDENTITY) | NO | PK autogenerada |
| nombre | VARCHAR2(20) | NO | ej. '2026-I' |
| fecha_inicio | DATE | NO |  |
| fecha_fin | DATE | NO |  |
| estado | VARCHAR2(12) | NO | default 'PLANIFICADO' |

- **PK:** id_periodo
- **Únicos:** (nombre)
- **Checks:** `(fecha_fin > fecha_inicio)` · `(estado IN ('PLANIFICADO','ABIERTO','CERRADO'))`

### CURSO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_curso | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_comunidad | NUMBER | NO |  |
| codigo | VARCHAR2(20) | NO |  |
| nombre | VARCHAR2(150) | NO |  |
| descripcion | VARCHAR2(1000) | sí |  |
| estado | VARCHAR2(10) | NO | default 'ACTIVO' |
| fecha_creacion | TIMESTAMP | NO | default SYSTIMESTAMP |

- **PK:** id_curso
- **Únicos:** (id_comunidad, codigo)
- **FK:** id_comunidad → COMUNIDAD.id_comunidad
- **Checks:** `(estado IN ('ACTIVO','INACTIVO'))`

### SECCION

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_seccion | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_curso | NUMBER | NO |  |
| id_periodo | NUMBER | NO |  |
| id_docente | NUMBER | NO | docente titular |
| grupo | VARCHAR2(10) | NO | 'A', 'B', ... |
| cupo_maximo | NUMBER(4) | sí |  |
| sala_virtual | VARCHAR2(500) | sí | enlace fijo por defecto (RF36) |
| estado | VARCHAR2(10) | NO | default 'ABIERTA' |

- **PK:** id_seccion
- **Únicos:** (id_curso, id_periodo, grupo)
- **FK:** id_curso → CURSO.id_curso; id_periodo → PERIODO_ACADEMICO.id_periodo; id_docente → USUARIO.id_usuario
- **Checks:** `(cupo_maximo IS NULL OR cupo_maximo > 0)` · `(estado IN ('ABIERTA','EN_CURSO','CERRADA'))`

### UNIDAD

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_unidad | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_seccion | NUMBER | NO |  |
| nombre | VARCHAR2(150) | NO |  |
| descripcion | VARCHAR2(1000) | sí |  |
| orden | NUMBER(3) | NO |  |

- **PK:** id_unidad
- **Únicos:** (id_seccion, orden)
- **FK:** id_seccion → SECCION.id_seccion

### SESION

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_sesion | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_unidad | NUMBER | NO |  |
| titulo | VARCHAR2(200) | NO |  |
| orden | NUMBER(3) | NO |  |
| fecha_inicio | TIMESTAMP | sí |  |
| duracion_min | NUMBER(4) | sí |  |
| enlace_clase | VARCHAR2(500) | sí | si es NULL se usa seccion.sala_virtual |

- **PK:** id_sesion
- **Únicos:** (id_unidad, orden)
- **FK:** id_unidad → UNIDAD.id_unidad
- **Checks:** `(duracion_min IS NULL OR duracion_min > 0)`

### RECURSO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_recurso | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_unidad | NUMBER | NO |  |
| id_sesion | NUMBER | sí | opcional: recurso de una sesión concreta |
| id_autor | NUMBER | NO |  |
| titulo | VARCHAR2(200) | NO |  |
| descripcion | VARCHAR2(1000) | sí |  |
| tipo | VARCHAR2(10) | NO |  |
| id_idioma | NUMBER | sí | idioma del recurso |
| id_archivo | NUMBER | sí |  |
| url_externa | VARCHAR2(500) | sí |  |
| fecha_publicacion | TIMESTAMP | NO | default SYSTIMESTAMP |
| eliminado | NUMBER(1) | NO | default 0 |
| fecha_eliminacion | TIMESTAMP | sí |  |

- **PK:** id_recurso
- **FK:** id_unidad → UNIDAD.id_unidad; id_sesion → SESION.id_sesion; id_autor → USUARIO.id_usuario; id_idioma → IDIOMA.id_idioma; id_archivo → ARCHIVO.id_archivo
- **Checks:** `(tipo IN ('DOCUMENTO','VIDEO','ENLACE','TAREA'))` · `( (tipo IN ('DOCUMENTO','VIDEO') AND id_archivo IS NOT NULL) OR (tipo = 'ENLACE' AND url_externa IS NOT NULL) OR (tipo = 'TAREA'))` · `(eliminado IN (0,1))`

### EVENTO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_evento | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_comunidad | NUMBER | NO |  |
| id_seccion | NUMBER | sí |  |
| id_creador | NUMBER | NO |  |
| titulo | VARCHAR2(200) | NO |  |
| descripcion | VARCHAR2(1000) | sí |  |
| fecha_inicio | TIMESTAMP | NO |  |
| fecha_fin | TIMESTAMP | sí |  |
| enlace | VARCHAR2(500) | sí |  |

- **PK:** id_evento
- **FK:** id_comunidad → COMUNIDAD.id_comunidad; id_seccion → SECCION.id_seccion; id_creador → USUARIO.id_usuario
- **Checks:** `(fecha_fin IS NULL OR fecha_fin >= fecha_inicio)`

### MATRICULA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_matricula | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_seccion | NUMBER | NO |  |
| id_usuario | NUMBER | NO | estudiante |
| fecha_matricula | TIMESTAMP | NO | default SYSTIMESTAMP |
| estado | VARCHAR2(12) | NO | default 'MATRICULADO' |
| nota_final | NUMBER(4,2) | sí | se calcula al cerrar la sección |
| fecha_retiro | TIMESTAMP | sí |  |

- **PK:** id_matricula
- **Únicos:** (id_seccion, id_usuario)
- **FK:** id_seccion → SECCION.id_seccion; id_usuario → USUARIO.id_usuario
- **Checks:** `(estado IN ('MATRICULADO','RETIRADO','APROBADO','DESAPROBADO'))` · `(nota_final IS NULL OR nota_final BETWEEN 0 AND 20)` · `((estado = 'RETIRADO' AND fecha_retiro IS NOT NULL) OR (estado <> 'RETIRADO' AND fecha_retiro IS NULL))`

### EVALUACION

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_evaluacion | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_seccion | NUMBER | NO |  |
| id_unidad | NUMBER | sí |  |
| nombre | VARCHAR2(100) | NO |  |
| tipo | VARCHAR2(15) | NO |  |
| peso | NUMBER(5,2) | NO | % de la nota final |
| fecha | TIMESTAMP | sí |  |

- **PK:** id_evaluacion
- **Únicos:** (id_seccion, nombre)
- **FK:** id_seccion → SECCION.id_seccion; id_unidad → UNIDAD.id_unidad
- **Checks:** `(tipo IN ('PRACTICA','TAREA','EXAMEN','PROYECTO','PARTICIPACION'))` · `(peso > 0 AND peso <= 100)`

### TAREA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_recurso | NUMBER | NO |  |
| fecha_entrega | TIMESTAMP | NO |  |
| permite_tardias | NUMBER(1) | NO | default 0 |
| id_evaluacion | NUMBER | sí | si la tarea se califica |

- **PK:** id_recurso
- **Únicos:** (id_evaluacion)
- **FK:** id_recurso → RECURSO.id_recurso; id_evaluacion → EVALUACION.id_evaluacion
- **Checks:** `(permite_tardias IN (0,1))`

### ENTREGA_TAREA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_entrega | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_recurso | NUMBER | NO | la tarea |
| id_matricula | NUMBER | NO | el alumno en esa sección |
| id_archivo | NUMBER | sí |  |
| comentario | VARCHAR2(1000) | sí |  |
| fecha_entrega | TIMESTAMP | NO | default SYSTIMESTAMP |
| estado | VARCHAR2(10) | NO | default 'ENTREGADA' |

- **PK:** id_entrega
- **Únicos:** (id_recurso, id_matricula)
- **FK:** id_recurso → TAREA.id_recurso; id_matricula → MATRICULA.id_matricula; id_archivo → ARCHIVO.id_archivo
- **Checks:** `(id_archivo IS NOT NULL OR comentario IS NOT NULL)` · `(estado IN ('ENTREGADA','TARDIA','CALIFICADA'))`

### NOTA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_nota | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_matricula | NUMBER | NO |  |
| id_evaluacion | NUMBER | NO |  |
| calificacion | NUMBER(4,2) | NO |  |
| comentario_docente | VARCHAR2(1000) | sí |  |
| id_registrado_por | NUMBER | NO | docente |
| fecha_registro | TIMESTAMP | NO | default SYSTIMESTAMP |
| fecha_modificacion | TIMESTAMP | sí |  |

- **PK:** id_nota
- **Únicos:** (id_matricula, id_evaluacion)
- **FK:** id_matricula → MATRICULA.id_matricula; id_evaluacion → EVALUACION.id_evaluacion; id_registrado_por → USUARIO.id_usuario
- **Checks:** `(calificacion BETWEEN 0 AND 20)`

### ASISTENCIA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_asistencia | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_sesion | NUMBER | NO |  |
| id_matricula | NUMBER | NO |  |
| estado | VARCHAR2(12) | NO |  |
| observacion | VARCHAR2(300) | sí |  |
| id_registrado_por | NUMBER | NO |  |
| fecha_registro | TIMESTAMP | NO | default SYSTIMESTAMP |

- **PK:** id_asistencia
- **Únicos:** (id_sesion, id_matricula)
- **FK:** id_sesion → SESION.id_sesion; id_matricula → MATRICULA.id_matricula; id_registrado_por → USUARIO.id_usuario
- **Checks:** `(estado IN ('PRESENTE','TARDANZA','FALTA','JUSTIFICADA'))`

### PUBLICACION

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_publicacion | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_comunidad | NUMBER | NO |  |
| id_seccion | NUMBER | sí | opcional: publicación de una sección |
| id_autor | NUMBER | NO |  |
| titulo | VARCHAR2(200) | sí |  |
| contenido | CLOB | sí |  |
| estado | VARCHAR2(10) | NO | default 'APROBADA' |
| id_moderador | NUMBER | sí |  |
| fecha_moderacion | TIMESTAMP | sí |  |
| fecha_publicacion | TIMESTAMP | NO | default SYSTIMESTAMP; RN09 |
| fecha_edicion | TIMESTAMP | sí |  |
| eliminado | NUMBER(1) | NO | default 0; RN10 |

- **PK:** id_publicacion
- **FK:** id_comunidad → COMUNIDAD.id_comunidad; id_seccion → SECCION.id_seccion; id_autor → USUARIO.id_usuario; id_moderador → USUARIO.id_usuario
- **Checks:** `(titulo IS NOT NULL OR contenido IS NOT NULL)` · `(estado IN ('PENDIENTE','APROBADA','RECHAZADA'))` · `(eliminado IN (0,1))`

### PUBLICACION_ARCHIVO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_publicacion | NUMBER | NO |  |
| id_archivo | NUMBER | NO |  |
| orden | NUMBER(2) | NO | default 1 |

- **PK:** id_publicacion, id_archivo
- **FK:** id_publicacion → PUBLICACION.id_publicacion; id_archivo → ARCHIVO.id_archivo

### COMENTARIO

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_comentario | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_publicacion | NUMBER | sí |  |
| id_recurso | NUMBER | sí |  |
| id_comentario_padre | NUMBER | sí |  |
| id_autor | NUMBER | NO |  |
| contenido | VARCHAR2(2000) | NO |  |
| fecha_comentario | TIMESTAMP | NO | default SYSTIMESTAMP |
| fecha_edicion | TIMESTAMP | sí |  |
| eliminado | NUMBER(1) | NO | default 0 |
| id_eliminado_por | NUMBER | sí | RF29: el docente puede eliminar |

- **PK:** id_comentario
- **FK:** id_publicacion → PUBLICACION.id_publicacion; id_recurso → RECURSO.id_recurso; id_comentario_padre → COMENTARIO.id_comentario; id_autor → USUARIO.id_usuario; id_eliminado_por → USUARIO.id_usuario
- **Checks:** `( (id_publicacion IS NOT NULL AND id_recurso IS NULL) OR (id_publicacion IS NULL AND id_recurso IS NOT NULL))` · `(TRIM(contenido) IS NOT NULL)` · `(eliminado IN (0,1))`

### NOTIFICACION

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_notificacion | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_destinatario | NUMBER | NO | receptor |
| id_emisor | NUMBER | sí | NULL = generada por el sistema |
| id_comunidad | NUMBER | sí |  |
| tipo | VARCHAR2(30) | NO |  |
| titulo | VARCHAR2(200) | NO |  |
| contenido | VARCHAR2(500) | sí |  |
| entidad_tipo | VARCHAR2(30) | sí | 'PUBLICACION','COMENTARIO','SOLICITUD',... |
| entidad_id | NUMBER | sí | id del objeto para abrirlo desde la notificación |
| leida | NUMBER(1) | NO | default 0 |
| fecha_creacion | TIMESTAMP | NO | default SYSTIMESTAMP |
| fecha_lectura | TIMESTAMP | sí |  |

- **PK:** id_notificacion
- **FK:** id_destinatario → USUARIO.id_usuario; id_emisor → USUARIO.id_usuario; id_comunidad → COMUNIDAD.id_comunidad
- **Checks:** `(tipo IN ('SOLICITUD_INGRESO','SOLICITUD_RESPUESTA', 'NUEVA_PUBLICACION','NUEVO_COMENTARIO','NUEVO_RECURSO','EVENTO','MENSAJE','SISTEMA'))` · `((leida = 0 AND fecha_lectura IS NULL) OR (leida = 1 AND fecha_lectura IS NOT NULL))`

### MENSAJE

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_mensaje | NUMBER (IDENTITY) | NO | PK autogenerada |
| id_emisor | NUMBER | NO |  |
| id_receptor | NUMBER | NO |  |
| contenido | VARCHAR2(4000) | NO |  |
| leido | NUMBER(1) | NO | default 0 |
| fecha_envio | TIMESTAMP | NO | default SYSTIMESTAMP |
| eliminado | NUMBER(1) | NO | default 0 |

- **PK:** id_mensaje
- **FK:** id_emisor → USUARIO.id_usuario; id_receptor → USUARIO.id_usuario
- **Checks:** `(id_emisor <> id_receptor)` · `(TRIM(contenido) IS NOT NULL)` · `(leido IN (0,1))` · `(eliminado IN (0,1))`

### AUDITORIA

| Columna | Tipo | Nulo | Default / notas |
|---|---|---|---|
| id_auditoria | NUMBER (IDENTITY) | NO | PK autogenerada |
| tabla | VARCHAR2(30) | NO |  |
| id_registro | NUMBER | NO |  |
| accion | VARCHAR2(10) | NO |  |
| id_usuario | NUMBER | sí |  |
| fecha | TIMESTAMP | NO | default SYSTIMESTAMP |
| valores_antes | CLOB | sí |  |
| valores_despues | CLOB | sí |  |

- **PK:** id_auditoria
- **FK:** id_usuario → USUARIO.id_usuario
- **Checks:** `(accion IN ('INSERT','UPDATE','DELETE'))` · `(valores_antes IS JSON)` · `(valores_despues IS JSON)`

---

## 7. Reglas de negocio: dónde se validan

Leyenda: **BD** = restricción ya activa (CHECK/UNIQUE/FK) · **Trigger** = en el script pendiente · **Backend** = debe hacerlo la aplicación.

| Regla | Qué exige | Dónde |
|---|---|---|
| RN01 | Correo `@minedu.gob.pe` o `@perueduca.pe`, en minúsculas | BD (`ck_usuario_correo_dominio`, `ck_usuario_correo_min`). Backend: normalizar a minúsculas antes de guardar |
| RN02 | En comunidades con `moderacion_previa = 1`, la publicación nace `PENDIENTE` | Trigger `trg_publicacion_valida`. **Backend mientras no exista el trigger** |
| RN03 / RN08 / RES15 | PDF/MP4/DOCX ≤ 50 MB; JPG/JPEG/PNG ≤ 2 MB | BD (`ck_archivo_formato_peso`). Backend: validar **antes** de subir a S3 (RES18) |
| RN04 / RN22 | Comunidad `PRIVADA` → ingreso solo por solicitud aprobada | Backend (en `PUBLICA` se inserta directo en `miembro_comunidad`) |
| RN05 | Solo DOCENTE (o ADMIN) crea publicaciones | Trigger + Backend (`@PreAuthorize`) |
| RN06 | Solo el autor edita su publicación | Backend |
| RN07 | Publicación con título o contenido | BD (`ck_pub_titulo_o_contenido`) |
| RN09 | Fecha de publicación automática | BD (default `SYSTIMESTAMP`) |
| RN10 / RN17 | Lo eliminado deja de verse | Backend: borrado lógico `eliminado = 1` y filtrar |
| RN11 / RN12 / RN19 / RN24 | Solo usuarios autenticados | Backend (Spring Security) |
| RN13 | Comentario asociado a una publicación existente | BD (FK + `ck_com_destino`: publicación **o** recurso) |
| RN14 | Mostrar nombre y rol del autor | Backend (JOIN usuario + rol; ver consulta C11) |
| RN15 | Sin comentarios/mensajes vacíos | BD (`TRIM(contenido) IS NOT NULL`) + Backend (`@NotBlank`) |
| RN16 | Respuesta en la misma publicación que su padre | Trigger `trg_comentario_valida` + Backend |
| RN18 | Normas de convivencia / filtro de lenguaje | Backend (filtro de palabras y sanitización, RES20) |
| RN20 | Buscar solo comunidades `ACTIVA` | Backend (filtro; vista `v_comunidades_activas`) |
| RN21 | Toda comunidad con categoría | BD (`id_categoria NOT NULL`) |
| RN23 | Abandonar comunidad | Backend: `estado = 'RETIRADO'`, `fecha_salida = SYSTIMESTAMP` (CHECK exige la fecha) |
| RN25 / RN26 | Recursos de cursos activos; descarga solo si está matriculado | Backend (consulta C09) |
| Una sola solicitud pendiente por usuario y comunidad | | BD (índice único `ux_solicitud_pendiente`) |
| Solicitud resuelta debe tener revisor y fecha | | BD (`ck_solicitud_revision`) |
| Una nota por alumno y evaluación (0–20) | | BD (`uq_nota`, `ck_nota_rango`) |
| Docente de sección = rol DOCENTE | | Trigger `trg_seccion_docente` + Backend |
| Matrícula: solo ESTUDIANTE, miembro activo de la comunidad, sección no cerrada | | Trigger `trg_matricula_valida` + Backend |
| Cupo máximo de la sección | | `pkg_comunidades.matricular` (o Backend) |
| Asistencia/nota de la misma sección que la matrícula | | Triggers `trg_asistencia_valida`, `trg_nota_valida` |
| Pesos de evaluaciones suman 100 % | | `pkg_comunidades.cerrar_seccion` |
| RF54 / RF55 | Bloqueo 15 min tras 5 intentos fallidos | `pkg_comunidades.registrar_login` (o Backend con la misma lógica) |

## 8. Lógica que vive en la base de datos (script pendiente)

### 8.1 Triggers (14)

| Trigger | Tabla / evento | Qué hace |
|---|---|---|
| `trg_seccion_docente` | SECCION ins/upd | Valida que `id_docente` tenga rol DOCENTE (error -20010) |
| `trg_matricula_valida` | MATRICULA ins | Rol ESTUDIANTE (-20011), sección no cerrada (-20012), miembro activo de la comunidad (-20013) |
| `trg_asistencia_valida` | ASISTENCIA ins/upd | Sesión de la misma sección que la matrícula (-20014) |
| `trg_nota_valida` | NOTA ins/upd | Evaluación de la misma sección (-20015); actualiza `fecha_modificacion` |
| `trg_recurso_valida` | RECURSO ins/upd | Sesión de la misma unidad (-20016); pone `fecha_eliminacion` |
| `trg_publicacion_valida` | PUBLICACION ins | RN05 (-20017); RN02 → estado `PENDIENTE` |
| `trg_comentario_valida` | COMENTARIO ins | RN16 (-20018) |
| `trg_usuario_actualizacion` | USUARIO upd | Pone `fecha_actualizacion` |
| `trg_notif_solicitud` | SOLICITUD_INGRESO ins | **RF16:** notifica a todos los ADMIN |
| `trg_notif_publicacion` | PUBLICACION ins/upd estado | **RF31:** notifica a los miembros cuando queda `APROBADA` |
| `trg_notif_comentario` | COMENTARIO ins | **RF35:** notifica al autor de la publicación/recurso |
| `trg_auditoria_nota` | NOTA ins/upd/del | Registra el cambio en AUDITORIA (JSON antes/después) |
| `trg_auditoria_publicacion` | PUBLICACION upd/del | **RES19** |
| `trg_auditoria_comentario` | COMENTARIO upd/del | **RES19** |

> Nota: `trg_comentario_valida` lee la misma tabla; funciona con `INSERT ... VALUES` de una fila (lo normal desde JPA). No usar `INSERT ... SELECT` masivo en COMENTARIO.

### 8.2 Paquete `PKG_COMUNIDADES`

| Subprograma | Firma | Uso desde el backend |
|---|---|---|
| `registrar_login` | `FUNCTION (p_correo VARCHAR2, p_exito BOOLEAN) RETURN VARCHAR2` → `'OK'`, `'FALLIDO'`, `'BLOQUEADO'`, `'NO_EXISTE'` | Tras validar credenciales. **BOOLEAN no se puede pasar por JDBC en 19c**: envolver en un bloque `BEGIN ? := pkg_comunidades.registrar_login(?, ? = 1); END;` o replicar la lógica en Java |
| `resolver_solicitud` | `PROCEDURE (p_id_solicitud, p_id_revisor, p_aprobar BOOLEAN, p_motivo VARCHAR2)` | RF22. Aprueba/rechaza, crea o reactiva al miembro y notifica. Mismo truco del BOOLEAN |
| `matricular` | `PROCEDURE (p_id_seccion, p_id_usuario)` | Respeta el cupo (-20021) |
| `promedio` | `FUNCTION (p_id_matricula) RETURN NUMBER` | Promedio ponderado actual |
| `cerrar_seccion` | `PROCEDURE (p_id_seccion)` | Valida pesos = 100 % (-20022), calcula `nota_final`, pone APROBADO/DESAPROBADO (≥ 10.5) y cierra la sección |
| `marcar_notificaciones_leidas` | `PROCEDURE (p_id_usuario)` | RF52 |

Probado con los datos de prueba: bloqueo al 5.º intento, solicitud aprobada crea al miembro, cupo lleno rechazado, cierre → Ana 16 APROBADO, Luis 10 DESAPROBADO.

### 8.3 Vistas

| Vista | Para qué |
|---|---|
| `v_reporte_seccion` | Por alumno y sección: nota acumulada, peso evaluado, % asistencia |
| `v_muro_comunidad` | Publicaciones visibles (aprobadas, no eliminadas) con autor, rol, nº de comentarios y adjuntos |
| `v_comunidades_activas` | Buscador: comunidad, categoría, idioma, pueblo, nº de miembros |

## 9. API REST propuesta

Base `/api/v1`. Todas requieren JWT salvo login. Paginación `?page=&size=` (Oracle: `OFFSET … FETCH NEXT …`).

### Autenticación y perfil (CU01, CU02, CU13)
| Método | Ruta | Rol | Notas |
|---|---|---|---|
| POST | `/auth/login` | público | Solo si no se usa Keycloak. Llama a `registrar_login` |
| POST | `/auth/logout` | todos | RF03 |
| GET | `/me` | todos | RF05 (usuario + rol + idioma) |
| PUT | `/me` | todos | RF06 (nombres, apellidos, idioma, especialidad) |
| PUT | `/me/foto` | todos | RF07 (multipart; JPG/PNG ≤ 2 MB → `archivo` + `id_foto_perfil`) |

### Catálogos
`GET /categorias`, `GET /idiomas`, `GET /pueblos`, `GET /periodos`.

### Comunidades (CU03–CU06)
| Método | Ruta | Rol | Notas |
|---|---|---|---|
| GET | `/comunidades?q=&categoria=&idioma=` | todos | RF08–RF11 (C01) |
| GET | `/comunidades/recomendadas` | todos | RF12 (C02) |
| GET | `/comunidades/{id}` | todos | |
| POST | `/comunidades` | ADMIN | RF17–RF18 |
| PUT | `/comunidades/{id}` | ADMIN | RF19 |
| DELETE | `/comunidades/{id}` | ADMIN | RF20 → `estado = 'INACTIVA'` (no borrar físico) |
| POST | `/comunidades/{id}/unirse` | EST, DOC | RF14 si es PUBLICA (inserta miembro) |
| POST | `/comunidades/{id}/solicitudes` | EST, DOC | RF13 si es PRIVADA |
| DELETE | `/comunidades/{id}/miembros/me` | miembro | RF15 / RN23 |
| GET | `/comunidades/{id}/miembros` | DOC, ADMIN | RF21, RF23 (C03) |
| DELETE | `/comunidades/{id}/miembros/{idUsuario}` | ADMIN, moderador | RF24 → `estado = 'EXPULSADO'` |
| GET | `/solicitudes?estado=PENDIENTE` | ADMIN | C04 |
| PUT | `/solicitudes/{id}` | ADMIN | RF22 `{aprobar, motivo}` → `resolver_solicitud` |

### Cursos y contenido académico (CU08–CU11)
| Método | Ruta | Rol | Notas |
|---|---|---|---|
| GET/POST | `/comunidades/{id}/cursos` | todos / ADMIN | RF38–RF41 |
| PUT/DELETE | `/cursos/{id}` | ADMIN | RF40 (DELETE → `estado = 'INACTIVO'`) |
| GET/POST | `/cursos/{id}/secciones` | todos / ADMIN | curso + periodo + grupo + docente |
| GET | `/secciones/{id}` | miembros | |
| GET/POST | `/secciones/{id}/unidades` | todos / DOC titular | RF45 |
| PUT/DELETE | `/unidades/{id}` | DOC titular | RF48–RF49 |
| GET/POST | `/unidades/{id}/sesiones` | todos / DOC | RF36 (`enlace_clase`) |
| GET | `/me/clases` | EST | RF37 (C08) |
| GET/POST | `/unidades/{id}/recursos` | matriculados / DOC | RF42–RF47; POST multipart para DOCUMENTO/VIDEO |
| PUT/DELETE | `/recursos/{id}` | DOC autor | DELETE → `eliminado = 1` |
| GET | `/recursos/{id}/descarga` | matriculados | RN26 → URL prefirmada de S3 |
| GET | `/me/recursos` | EST | RF43–RF44 (C09) |
| GET/POST | `/comunidades/{id}/eventos` | todos / DOC | CU08 eventos |

### Matrícula, evaluaciones, notas, asistencia, tareas
| Método | Ruta | Rol | Notas |
|---|---|---|---|
| POST | `/secciones/{id}/matriculas` | ADMIN, EST | `pkg_comunidades.matricular` |
| PUT | `/matriculas/{id}/retiro` | ADMIN, EST | `estado = 'RETIRADO'`, `fecha_retiro` |
| GET/POST | `/secciones/{id}/evaluaciones` | DOC | pesos en % |
| GET | `/secciones/{id}/notas` | DOC | libreta (C05) |
| PUT | `/notas` | DOC | upsert `{idMatricula, idEvaluacion, calificacion}` |
| GET | `/me/notas` | EST | sus notas + `promedio` |
| POST | `/secciones/{id}/cerrar` | DOC, ADMIN | `cerrar_seccion` |
| GET | `/secciones/{id}/ranking` | DOC | C07 |
| GET | `/secciones/{id}/riesgo` | DOC | C06 |
| GET/POST | `/sesiones/{id}/asistencia` | DOC | lista y registro masivo |
| POST | `/tareas/{idRecurso}/entregas` | EST | archivo o comentario; `TARDIA` si pasó `fecha_entrega` |
| GET | `/tareas/{idRecurso}/entregas` | DOC | C10 |

### Publicaciones y comentarios (CU07)
| Método | Ruta | Rol | Notas |
|---|---|---|---|
| GET | `/comunidades/{id}/publicaciones` | miembros | muro (`v_muro_comunidad`) |
| POST | `/comunidades/{id}/publicaciones` | DOC | RF25, RF28, RF30 (adjuntos) |
| PUT/DELETE | `/publicaciones/{id}` | DOC autor | RF26–RF27, RN06 |
| PUT | `/publicaciones/{id}/moderacion` | moderador, ADMIN | RN02 `{estado}` |
| GET | `/publicaciones/{id}/comentarios` | miembros | árbol (C11) |
| POST | `/publicaciones/{id}/comentarios` | miembros | RF33–RF34 (`idComentarioPadre` opcional) |
| POST | `/recursos/{id}/comentarios` | matriculados | |
| DELETE | `/comentarios/{id}` | autor, DOC | RF29 → `eliminado = 1`, `id_eliminado_por` |

### Notificaciones y mensajes (CU12, CUS10)
| Método | Ruta | Rol | Notas |
|---|---|---|---|
| GET | `/me/notificaciones` | todos | RF51 (C12) |
| PUT | `/notificaciones/{id}/leida` | destinatario | RF52 |
| PUT | `/me/notificaciones/leidas` | todos | `marcar_notificaciones_leidas` |
| GET | `/me/mensajes?con={idUsuario}` | todos | conversación entre dos usuarios |
| POST | `/mensajes` | todos | `{idReceptor, contenido}` |

### Reportes
`GET /reportes/docentes?periodo=` (C14), `GET /reportes/idiomas` (C13).

## 10. Errores de Oracle → respuesta HTTP

| Error Oracle | Significa | HTTP sugerido |
|---|---|---|
| ORA-00001 (unique) | Duplicado: correo, matrícula, nota, solicitud pendiente… | 409 Conflict |
| ORA-02290 (check) | Valor no permitido (nombre de la restricción en el mensaje) | 400 / 422 |
| ORA-02291 (FK padre) | Referencia a algo que no existe | 404 / 422 |
| ORA-02292 (FK hijo) | No se puede borrar: tiene datos relacionados | 409 (usar borrado lógico) |
| ORA-01400 | Falta un campo obligatorio | 400 |
| ORA-20010…20018 | Reglas de los triggers | 422 con el mensaje del error |
| ORA-20020 | Solicitud ya resuelta | 409 |
| ORA-20021 | Sin cupo | 409 |
| ORA-20022 | Pesos ≠ 100 % | 422 |

Recomendación: un `@ControllerAdvice` que lea `SQLException.getErrorCode()` y, para los CHECK, traduzca el nombre de la restricción (`CK_…`) a un mensaje legible.

## 11. Valores permitidos (enums)

| Campo | Valores |
|---|---|
| `rol.codigo` | ESTUDIANTE, DOCENTE, ADMIN |
| `usuario.estado` | ACTIVO, INACTIVO |
| `comunidad.visibilidad` | PUBLICA, PRIVADA |
| `comunidad.estado` | ACTIVA, INACTIVA |
| `miembro_comunidad.rol_comunidad` | MIEMBRO, MODERADOR, ADMINISTRADOR |
| `miembro_comunidad.estado` | ACTIVO, RETIRADO, EXPULSADO |
| `solicitud_ingreso.estado` | PENDIENTE, APROBADA, RECHAZADA |
| `periodo_academico.estado` | PLANIFICADO, ABIERTO, CERRADO |
| `curso.estado` | ACTIVO, INACTIVO |
| `seccion.estado` | ABIERTA, EN_CURSO, CERRADA |
| `recurso.tipo` | DOCUMENTO, VIDEO, ENLACE, TAREA |
| `evaluacion.tipo` | PRACTICA, TAREA, EXAMEN, PROYECTO, PARTICIPACION |
| `matricula.estado` | MATRICULADO, RETIRADO, APROBADO, DESAPROBADO |
| `entrega_tarea.estado` | ENTREGADA, TARDIA, CALIFICADA |
| `asistencia.estado` | PRESENTE, TARDANZA, FALTA, JUSTIFICADA |
| `publicacion.estado` | PENDIENTE, APROBADA, RECHAZADA |
| `notificacion.tipo` | SOLICITUD_INGRESO, SOLICITUD_RESPUESTA, NUEVA_PUBLICACION, NUEVO_COMENTARIO, NUEVO_RECURSO, EVENTO, MENSAJE, SISTEMA |
| `auditoria.accion` | INSERT, UPDATE, DELETE |

## 12. Datos de prueba cargados

| id | Usuario | Rol | Notas |
|---|---|---|---|
| 1 | admin@minedu.gob.pe (Rosa Quispe) | ADMIN | |
| 2 | jcampos@perueduca.pe (Julio Campos) | DOCENTE | Titular de Matemática 1 – A |
| 3 | mhuaman@perueduca.pe (María Huamán) | DOCENTE | Moderadora de «Saberes Andinos» |
| 4 | achumpate@perueduca.pe (Ana Chumpate) | ESTUDIANTE | Notas 17/15/16 → 16 |
| 5 | lsantos@perueduca.pe (Luis Santos) | ESTUDIANTE | Notas 9/11/10 → 10, asistencia 33 % (en riesgo) |
| 6 | kmayta@perueduca.pe (Karen Mayta) | ESTUDIANTE | Solicitud PENDIENTE a «Saberes Andinos» |
| 7 | pnaquiri@perueduca.pe (Pedro Naquiri) | ESTUDIANTE | Solo nota de Práctica 1 (14) |

Comunidades: 1 «Comunidad Asháninka – Río Tambo» (PÚBLICA), 2 «Saberes Andinos» (PRIVADA, con moderación previa). Periodo 2026-II, curso MAT-01, sección 1 (grupo A), 2 unidades, 3 sesiones, 2 recursos (una guía PDF y una tarea), 3 evaluaciones (30 + 30 + 40 = 100 %).

> Los `password_hash` son ficticios (`$2a$10$demo…`). Para probar login propio, regenerarlos con BCrypt desde el backend; con Keycloak, llenar `keycloak_sub` con el `sub` del token.

## 13. Pendientes y decisiones abiertas

1. **El DBA debe ejecutar la sección 1** del archivo SQL (`GRANT CREATE TRIGGER, CREATE PROCEDURE, CREATE VIEW TO comunidades`); luego, como COMUNIDADES, las secciones 5–7 (triggers, paquete, vistas). Hasta entonces el backend debe validar esas reglas por su cuenta (sección 7).
2. **Completar la contraseña** en la sección 3.
3. **SSO (Keycloak) o login propio:** la tabla admite ambos (`keycloak_sub` o `password_hash`). Decidir uno.
4. **Comentarios en recursos:** RN13 dice solo publicaciones; el modelo también permite recursos. Confirmar.
5. El Excel no tiene RF para matrícula, notas, asistencia ni tareas (se agregaron al modelo); conviene agregarlos al documento.
6. Mensajería simple (uno a uno). Sin chats grupales en v1.
7. RES19 (auditoría) queda cubierta en BD para notas, publicaciones y comentarios; el resto, si se pide, desde el backend.

## 14. Instrucción sugerida para la IA que construya el backend

> Construye un backend **Spring Boot 3 (Java 17)** para la base Oracle descrita en este informe (conexión en la sección 3). Reglas: no modifiques el esquema (`ddl-auto=validate`); mapea las 28 tablas como entidades JPA con `GenerationType.IDENTITY`, enums para los estados (sección 11) y un converter Boolean↔NUMBER(1); usa borrado lógico donde existe la columna `eliminado`; implementa los endpoints de la sección 9 con DTOs, validación (`@Valid`) y seguridad por rol (JWT de Keycloak, roles ESTUDIANTE/DOCENTE/ADMIN); traduce los errores Oracle según la sección 10; llama a `PKG_COMUNIDADES` con `SimpleJdbcCall`/`CallableStatement` cuando exista y, mientras tanto, replica su lógica en servicios; guarda los archivos en S3 y en la BD solo la ruta. Empieza por: entidades + repositorios, luego auth/perfil, comunidades, cursos/secciones, notas/asistencia, publicaciones/comentarios, notificaciones/mensajes. Usa los datos de prueba de la sección 12 para las pruebas.

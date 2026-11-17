/* =====================================================================================
   PerúEduca – Comunidades Virtuales
   MODELO DE IMPLEMENTACIÓN DE LA BASE DE DATOS (Oracle 19c) – Iteración 1
   -------------------------------------------------------------------------------------
   Contenido
     1. Usuarios y roles de base de datos ........ (ejecutar como DBA / SYSTEM)
     2. Tablas (29) con PK, UNIQUE y CHECK ....... (como COMUNIDADES)
     3. Claves foráneas (61) e índices ........... (como COMUNIDADES)
     4. Paquete PKG_COMUNIDADES: 2 funciones y 4 procedimientos
     5. Triggers (14) y vistas (3)
     6. Permisos del usuario de ejecución APP_COMUNIDADES
     7. Catálogos iniciales (roles, idiomas, categoría)
     8. Ambiente de desarrollo COMU_VIR_DESA (clon con Data Pump)
     9. Consultas de verificación
   Datos de prueba: ver 08_datos_prueba_por_rol.sql
   Las contraseñas NUNCA se escriben en el script: SQL*Plus/SQLcl las pide (ACCEPT ... HIDE).
   ===================================================================================== */
SET DEFINE ON
SET VERIFY OFF

-- =====================================================================================
-- 1. USUARIOS Y ROLES DE BASE DE DATOS  (conectado como SYSTEM / DBA)
-- =====================================================================================
-- 1.1 Propietario del esquema: contiene las tablas, el paquete, los triggers y las vistas
ACCEPT clave_owner CHAR PROMPT 'Contraseña para COMUNIDADES: ' HIDE
CREATE USER COMUNIDADES IDENTIFIED BY "&clave_owner"
  DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
UNDEFINE clave_owner
-- ROL_DESARROLLADOR ya existe en la BD del curso y está asignado a COMUNIDADES.
-- Si se instala en una BD nueva, se crea así (privilegios mínimos para desarrollar):
CREATE ROLE ROL_DESARROLLADOR;
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE,
      CREATE PROCEDURE, CREATE TRIGGER, CREATE TYPE TO ROL_DESARROLLADOR;
GRANT ROL_DESARROLLADOR TO COMUNIDADES;
GRANT CREATE TRIGGER, CREATE PROCEDURE, CREATE VIEW TO COMUNIDADES;  -- privilegios directos (necesarios en PL/SQL)

-- 1.2 Usuario de ejecución de la aplicación (mínimo privilegio, sin dueño de objetos)
ACCEPT clave_app CHAR PROMPT 'Contraseña para APP_COMUNIDADES: ' HIDE
CREATE USER APP_COMUNIDADES IDENTIFIED BY "&clave_app";
UNDEFINE clave_app
GRANT CREATE SESSION TO APP_COMUNIDADES;
CREATE ROLE R_COMUNIDADES_APP;                     -- permisos sobre objetos (ver sección 6)
GRANT R_COMUNIDADES_APP TO APP_COMUNIDADES;

-- 1.3 Roles funcionales de la aplicación: NO son roles Oracle, son filas de la tabla ROL
--     ESTUDIANTE (@perueduca.pe) · DOCENTE (@minedu.gob.pe) · ADMIN (@minedu.gob.pe)
--     Se cargan en la sección 7 y se validan en Spring Security (@PreAuthorize).

-- A partir de aquí, conectarse como COMUNIDADES
-- CONNECT COMUNIDADES@//<host>:1521/orcl
SET DEFINE OFF

-- =====================================================================================
-- 2. TABLAS (29)
-- =====================================================================================

CREATE TABLE ROL (
  id_rol NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo VARCHAR2(20) NOT NULL,
  nombre VARCHAR2(50) NOT NULL,
  CONSTRAINT pk_rol PRIMARY KEY (id_rol),
  CONSTRAINT ck_rol_1 CHECK (codigo IN ('ESTUDIANTE','DOCENTE','ADMIN')),
  CONSTRAINT uq_rol_2 UNIQUE (codigo)
);

CREATE TABLE CATEGORIA (
  id_categoria NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre VARCHAR2(100) NOT NULL,
  descripcion VARCHAR2(500),
  CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
  CONSTRAINT uq_categoria_1 UNIQUE (nombre)
);

CREATE TABLE IDIOMA (
  id_idioma NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo VARCHAR2(10) NOT NULL,
  nombre VARCHAR2(60) NOT NULL,
  CONSTRAINT pk_idioma PRIMARY KEY (id_idioma),
  CONSTRAINT uq_idioma_1 UNIQUE (codigo)
);

CREATE TABLE PUEBLO_ORIGINARIO (
  id_pueblo NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre VARCHAR2(100) NOT NULL,
  familia_linguistica VARCHAR2(100),
  CONSTRAINT pk_pueblo_originario PRIMARY KEY (id_pueblo),
  CONSTRAINT uq_pueblo_originario_1 UNIQUE (nombre)
);

CREATE TABLE USUARIO (
  id_usuario NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_rol NUMBER NOT NULL,
  correo VARCHAR2(150) NOT NULL,
  nombres VARCHAR2(100) NOT NULL,
  apellidos VARCHAR2(100) NOT NULL,
  especialidad VARCHAR2(100),
  telefono VARCHAR2(15),
  keycloak_sub VARCHAR2(64),
  password_hash VARCHAR2(255),
  intentos_fallidos NUMBER(2) DEFAULT 0 NOT NULL,
  bloqueado_hasta TIMESTAMP,
  id_foto_perfil NUMBER,
  id_idioma_pref NUMBER,
  estado VARCHAR2(10) DEFAULT 'ACTIVO' NOT NULL,
  fecha_registro TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_actualizacion TIMESTAMP,
  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
  -- Regla de correo por rol (id_rol 1 = ESTUDIANTE; 2 = DOCENTE; 3 = ADMIN)
  CONSTRAINT ck_usuario_correo_rol CHECK (
    (id_rol = 1 AND REGEXP_LIKE(correo, '^[a-z0-9._%+-]+@perueduca\.pe$')) OR
    (id_rol IN (2,3) AND REGEXP_LIKE(correo, '^[a-z0-9._%+-]+@minedu\.gob\.pe$'))),
  CONSTRAINT ck_usuario_2 CHECK (correo = LOWER(correo)),
  CONSTRAINT ck_usuario_3 CHECK (keycloak_sub IS NOT NULL OR password_hash IS NOT NULL),
  CONSTRAINT ck_usuario_4 CHECK (intentos_fallidos >= 0),
  CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO','INACTIVO','PENDIENTE')),
  CONSTRAINT ck_usuario_telefono CHECK (telefono IS NULL OR REGEXP_LIKE(telefono, '^[0-9]{9}$')),
  CONSTRAINT uq_usuario_6 UNIQUE (correo),
  CONSTRAINT uq_usuario_7 UNIQUE (keycloak_sub)
);

CREATE TABLE ARCHIVO (
  id_archivo NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_subido_por NUMBER NOT NULL,
  nombre_original VARCHAR2(255) NOT NULL,
  extension VARCHAR2(10) NOT NULL,
  tipo_mime VARCHAR2(100) NOT NULL,
  tamano_bytes NUMBER NOT NULL,
  ruta_almacenamiento VARCHAR2(500) NOT NULL,
  fecha_subida TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_archivo PRIMARY KEY (id_archivo),
  CONSTRAINT ck_archivo_1 CHECK ( (LOWER(extension) IN ('pdf','mp4','docx') AND tamano_bytes <= 52428800) OR (LOWER(extension) IN ('jpg','jpeg','png') AND tamano_bytes <= 2097152) ),
  CONSTRAINT ck_archivo_2 CHECK (tamano_bytes > 0)
);

CREATE TABLE COMUNIDAD (
  id_comunidad NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre VARCHAR2(150) NOT NULL,
  descripcion VARCHAR2(1000),
  id_categoria NUMBER NOT NULL,
  id_idioma NUMBER,
  id_pueblo NUMBER,
  visibilidad VARCHAR2(10) DEFAULT 'PUBLICA' NOT NULL,
  moderacion_previa NUMBER(1) DEFAULT 0 NOT NULL,
  estado VARCHAR2(10) DEFAULT 'ACTIVA' NOT NULL,
  id_imagen NUMBER,
  id_creador NUMBER NOT NULL,
  fecha_creacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_actualizacion TIMESTAMP,
  CONSTRAINT pk_comunidad PRIMARY KEY (id_comunidad),
  CONSTRAINT ck_comunidad_1 CHECK (visibilidad IN ('PUBLICA','PRIVADA')),
  CONSTRAINT ck_comunidad_2 CHECK (moderacion_previa IN (0,1)),
  CONSTRAINT ck_comunidad_3 CHECK (estado IN ('ACTIVA','INACTIVA')),
  CONSTRAINT uq_comunidad_4 UNIQUE (nombre)
);

CREATE TABLE MIEMBRO_COMUNIDAD (
  id_miembro NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_comunidad NUMBER NOT NULL,
  id_usuario NUMBER NOT NULL,
  rol_comunidad VARCHAR2(15) DEFAULT 'MIEMBRO' NOT NULL,
  estado VARCHAR2(10) DEFAULT 'ACTIVO' NOT NULL,
  fecha_ingreso TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_salida TIMESTAMP,
  CONSTRAINT pk_miembro_comunidad PRIMARY KEY (id_miembro),
  CONSTRAINT ck_miembro_comunidad_1 CHECK (rol_comunidad IN ('MIEMBRO','MODERADOR','ADMINISTRADOR')),
  CONSTRAINT ck_miembro_comunidad_2 CHECK (estado IN ('ACTIVO','RETIRADO','EXPULSADO')),
  CONSTRAINT ck_miembro_comunidad_3 CHECK ((estado = 'ACTIVO' AND fecha_salida IS NULL) OR (estado <> 'ACTIVO' AND fecha_salida IS NOT NULL)),
  CONSTRAINT uq_miembro_comunidad_4 UNIQUE (id_comunidad, id_usuario)
);

CREATE TABLE SOLICITUD_INGRESO (
  id_solicitud NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_comunidad NUMBER NOT NULL,
  id_usuario NUMBER NOT NULL,
  estado VARCHAR2(10) DEFAULT 'PENDIENTE' NOT NULL,
  mensaje VARCHAR2(500),
  fecha_solicitud TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  id_revisor NUMBER,
  fecha_respuesta TIMESTAMP,
  motivo_rechazo VARCHAR2(500),
  CONSTRAINT pk_solicitud_ingreso PRIMARY KEY (id_solicitud),
  CONSTRAINT ck_solicitud_ingreso_1 CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
  CONSTRAINT ck_solicitud_ingreso_2 CHECK ( (estado = 'PENDIENTE' AND id_revisor IS NULL AND fecha_respuesta IS NULL) OR (estado <> 'PENDIENTE' AND id_revisor IS NOT NULL AND fecha_respuesta IS NOT NULL))
);

CREATE TABLE PERIODO_ACADEMICO (
  id_periodo NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre VARCHAR2(20) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  estado VARCHAR2(12) DEFAULT 'PLANIFICADO' NOT NULL,
  CONSTRAINT pk_periodo_academico PRIMARY KEY (id_periodo),
  CONSTRAINT ck_periodo_academico_1 CHECK (fecha_fin > fecha_inicio),
  CONSTRAINT ck_periodo_academico_2 CHECK (estado IN ('PLANIFICADO','ABIERTO','CERRADO')),
  CONSTRAINT uq_periodo_academico_3 UNIQUE (nombre)
);

CREATE TABLE CURSO (
  id_curso NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_comunidad NUMBER NOT NULL,
  codigo VARCHAR2(20) NOT NULL,
  nombre VARCHAR2(150) NOT NULL,
  descripcion VARCHAR2(1000),
  estado VARCHAR2(10) DEFAULT 'ACTIVO' NOT NULL,
  fecha_creacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_curso PRIMARY KEY (id_curso),
  CONSTRAINT ck_curso_1 CHECK (estado IN ('ACTIVO','INACTIVO')),
  CONSTRAINT uq_curso_2 UNIQUE (id_comunidad, codigo)
);

CREATE TABLE SECCION (
  id_seccion NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_curso NUMBER NOT NULL,
  id_periodo NUMBER NOT NULL,
  id_docente NUMBER NOT NULL,
  grupo VARCHAR2(10) NOT NULL,
  cupo_maximo NUMBER(4),
  sala_virtual VARCHAR2(500),
  estado VARCHAR2(10) DEFAULT 'ABIERTA' NOT NULL,
  CONSTRAINT pk_seccion PRIMARY KEY (id_seccion),
  CONSTRAINT ck_seccion_1 CHECK (cupo_maximo IS NULL OR cupo_maximo > 0),
  CONSTRAINT ck_seccion_2 CHECK (estado IN ('ABIERTA','EN_CURSO','CERRADA')),
  CONSTRAINT uq_seccion_3 UNIQUE (id_curso, id_periodo, grupo)
);

CREATE TABLE UNIDAD (
  id_unidad NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_seccion NUMBER NOT NULL,
  nombre VARCHAR2(150) NOT NULL,
  descripcion VARCHAR2(1000),
  orden NUMBER(3) NOT NULL,
  CONSTRAINT pk_unidad PRIMARY KEY (id_unidad),
  CONSTRAINT uq_unidad_1 UNIQUE (id_seccion, orden)
);

CREATE TABLE SESION (
  id_sesion NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_unidad NUMBER NOT NULL,
  titulo VARCHAR2(200) NOT NULL,
  orden NUMBER(3) NOT NULL,
  fecha_inicio TIMESTAMP,
  duracion_min NUMBER(4),
  enlace_clase VARCHAR2(500),
  CONSTRAINT pk_sesion PRIMARY KEY (id_sesion),
  CONSTRAINT ck_sesion_1 CHECK (duracion_min IS NULL OR duracion_min > 0),
  CONSTRAINT uq_sesion_2 UNIQUE (id_unidad, orden)
);

CREATE TABLE RECURSO (
  id_recurso NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_unidad NUMBER NOT NULL,
  id_sesion NUMBER,
  id_autor NUMBER NOT NULL,
  titulo VARCHAR2(200) NOT NULL,
  descripcion VARCHAR2(1000),
  tipo VARCHAR2(10) NOT NULL,
  id_idioma NUMBER,
  id_archivo NUMBER,
  url_externa VARCHAR2(500),
  fecha_publicacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  eliminado NUMBER(1) DEFAULT 0 NOT NULL,
  fecha_eliminacion TIMESTAMP,
  CONSTRAINT pk_recurso PRIMARY KEY (id_recurso),
  CONSTRAINT ck_recurso_1 CHECK (tipo IN ('DOCUMENTO','VIDEO','ENLACE','TAREA')),
  CONSTRAINT ck_recurso_2 CHECK ( (tipo IN ('DOCUMENTO','VIDEO') AND id_archivo IS NOT NULL) OR (tipo = 'ENLACE' AND url_externa IS NOT NULL) OR (tipo = 'TAREA')),
  CONSTRAINT ck_recurso_3 CHECK (eliminado IN (0,1))
);

CREATE TABLE EVENTO (
  id_evento NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_comunidad NUMBER NOT NULL,
  id_seccion NUMBER,
  id_creador NUMBER NOT NULL,
  titulo VARCHAR2(200) NOT NULL,
  descripcion VARCHAR2(1000),
  fecha_inicio TIMESTAMP NOT NULL,
  fecha_fin TIMESTAMP,
  enlace VARCHAR2(500),
  CONSTRAINT pk_evento PRIMARY KEY (id_evento),
  CONSTRAINT ck_evento_1 CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE TABLE MATRICULA (
  id_matricula NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_seccion NUMBER NOT NULL,
  id_usuario NUMBER NOT NULL,
  fecha_matricula TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  estado VARCHAR2(12) DEFAULT 'MATRICULADO' NOT NULL,
  nota_final NUMBER(4,2),
  fecha_retiro TIMESTAMP,
  CONSTRAINT pk_matricula PRIMARY KEY (id_matricula),
  CONSTRAINT ck_matricula_1 CHECK (estado IN ('MATRICULADO','RETIRADO','APROBADO','DESAPROBADO')),
  CONSTRAINT ck_matricula_2 CHECK (nota_final IS NULL OR nota_final BETWEEN 0 AND 20),
  CONSTRAINT ck_matricula_3 CHECK ((estado = 'RETIRADO' AND fecha_retiro IS NOT NULL) OR (estado <> 'RETIRADO' AND fecha_retiro IS NULL)),
  CONSTRAINT uq_matricula_4 UNIQUE (id_seccion, id_usuario)
);

CREATE TABLE EVALUACION (
  id_evaluacion NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_seccion NUMBER NOT NULL,
  id_unidad NUMBER,
  nombre VARCHAR2(100) NOT NULL,
  tipo VARCHAR2(15) NOT NULL,
  peso NUMBER(5,2) NOT NULL,
  fecha TIMESTAMP,
  CONSTRAINT pk_evaluacion PRIMARY KEY (id_evaluacion),
  CONSTRAINT ck_evaluacion_1 CHECK (tipo IN ('PRACTICA','TAREA','EXAMEN','PROYECTO','PARTICIPACION')),
  CONSTRAINT ck_evaluacion_2 CHECK (peso > 0 AND peso <= 100),
  CONSTRAINT uq_evaluacion_3 UNIQUE (id_seccion, nombre)
);

CREATE TABLE TAREA (
  id_recurso NUMBER NOT NULL,
  fecha_entrega TIMESTAMP NOT NULL,
  permite_tardias NUMBER(1) DEFAULT 0 NOT NULL,
  id_evaluacion NUMBER,
  CONSTRAINT pk_tarea PRIMARY KEY (id_recurso),
  CONSTRAINT ck_tarea_1 CHECK (permite_tardias IN (0,1)),
  CONSTRAINT uq_tarea_2 UNIQUE (id_evaluacion)
);

CREATE TABLE ENTREGA_TAREA (
  id_entrega NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_recurso NUMBER NOT NULL,
  id_matricula NUMBER NOT NULL,
  id_archivo NUMBER,
  comentario VARCHAR2(1000),
  fecha_entrega TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  estado VARCHAR2(10) DEFAULT 'ENTREGADA' NOT NULL,
  CONSTRAINT pk_entrega_tarea PRIMARY KEY (id_entrega),
  CONSTRAINT ck_entrega_tarea_1 CHECK (id_archivo IS NOT NULL OR comentario IS NOT NULL),
  CONSTRAINT ck_entrega_tarea_2 CHECK (estado IN ('ENTREGADA','TARDIA','CALIFICADA')),
  CONSTRAINT uq_entrega_tarea_3 UNIQUE (id_recurso, id_matricula)
);

CREATE TABLE NOTA (
  id_nota NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_matricula NUMBER NOT NULL,
  id_evaluacion NUMBER NOT NULL,
  calificacion NUMBER(4,2) NOT NULL,
  comentario_docente VARCHAR2(1000),
  id_registrado_por NUMBER NOT NULL,
  fecha_registro TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_modificacion TIMESTAMP,
  CONSTRAINT pk_nota PRIMARY KEY (id_nota),
  CONSTRAINT ck_nota_1 CHECK (calificacion BETWEEN 0 AND 20),
  CONSTRAINT uq_nota_2 UNIQUE (id_matricula, id_evaluacion)
);

CREATE TABLE ASISTENCIA (
  id_asistencia NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_sesion NUMBER NOT NULL,
  id_matricula NUMBER NOT NULL,
  estado VARCHAR2(12) NOT NULL,
  observacion VARCHAR2(300),
  id_registrado_por NUMBER NOT NULL,
  fecha_registro TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_asistencia PRIMARY KEY (id_asistencia),
  CONSTRAINT ck_asistencia_1 CHECK (estado IN ('PRESENTE','TARDANZA','FALTA','JUSTIFICADA')),
  CONSTRAINT uq_asistencia_2 UNIQUE (id_sesion, id_matricula)
);

CREATE TABLE PUBLICACION (
  id_publicacion NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_comunidad NUMBER NOT NULL,
  id_seccion NUMBER,
  id_autor NUMBER NOT NULL,
  titulo VARCHAR2(200),
  contenido CLOB,
  estado VARCHAR2(10) DEFAULT 'APROBADA' NOT NULL,
  id_moderador NUMBER,
  fecha_moderacion TIMESTAMP,
  fecha_publicacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_edicion TIMESTAMP,
  eliminado NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT pk_publicacion PRIMARY KEY (id_publicacion),
  CONSTRAINT ck_publicacion_1 CHECK (titulo IS NOT NULL OR contenido IS NOT NULL),
  CONSTRAINT ck_publicacion_2 CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA')),
  CONSTRAINT ck_publicacion_3 CHECK (eliminado IN (0,1))
);

CREATE TABLE PUBLICACION_ARCHIVO (
  id_publicacion NUMBER NOT NULL,
  id_archivo NUMBER NOT NULL,
  orden NUMBER(2) DEFAULT 1 NOT NULL,
  CONSTRAINT pk_publicacion_archivo PRIMARY KEY (id_publicacion, id_archivo)
);

CREATE TABLE COMENTARIO (
  id_comentario NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_publicacion NUMBER,
  id_recurso NUMBER,
  id_comentario_padre NUMBER,
  id_autor NUMBER NOT NULL,
  contenido VARCHAR2(2000) NOT NULL,
  fecha_comentario TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_edicion TIMESTAMP,
  eliminado NUMBER(1) DEFAULT 0 NOT NULL,
  id_eliminado_por NUMBER,
  CONSTRAINT pk_comentario PRIMARY KEY (id_comentario),
  CONSTRAINT ck_comentario_1 CHECK ( (id_publicacion IS NOT NULL AND id_recurso IS NULL) OR (id_publicacion IS NULL AND id_recurso IS NOT NULL)),
  CONSTRAINT ck_comentario_2 CHECK (TRIM(contenido) IS NOT NULL),
  CONSTRAINT ck_comentario_3 CHECK (eliminado IN (0,1))
);

CREATE TABLE NOTIFICACION (
  id_notificacion NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_destinatario NUMBER NOT NULL,
  id_emisor NUMBER,
  id_comunidad NUMBER,
  tipo VARCHAR2(30) NOT NULL,
  titulo VARCHAR2(200) NOT NULL,
  contenido VARCHAR2(500),
  entidad_tipo VARCHAR2(30),
  entidad_id NUMBER,
  leida NUMBER(1) DEFAULT 0 NOT NULL,
  fecha_creacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_lectura TIMESTAMP,
  CONSTRAINT pk_notificacion PRIMARY KEY (id_notificacion),
  CONSTRAINT ck_notif_tipo CHECK (tipo IN ('SOLICITUD_INGRESO','SOLICITUD_RESPUESTA','NUEVA_PUBLICACION','NUEVO_COMENTARIO',
                                            'NUEVO_RECURSO','EVENTO','MENSAJE','SISTEMA','AVISO')),
  CONSTRAINT ck_notificacion_2 CHECK ((leida = 0 AND fecha_lectura IS NULL) OR (leida = 1 AND fecha_lectura IS NOT NULL))
);

CREATE TABLE MENSAJE (
  id_mensaje NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_emisor NUMBER NOT NULL,
  id_receptor NUMBER NOT NULL,
  contenido VARCHAR2(4000) NOT NULL,
  leido NUMBER(1) DEFAULT 0 NOT NULL,
  fecha_envio TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  eliminado NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT pk_mensaje PRIMARY KEY (id_mensaje),
  CONSTRAINT ck_mensaje_1 CHECK (id_emisor <> id_receptor),
  CONSTRAINT ck_mensaje_2 CHECK (TRIM(contenido) IS NOT NULL),
  CONSTRAINT ck_mensaje_3 CHECK (leido IN (0,1)),
  CONSTRAINT ck_mensaje_4 CHECK (eliminado IN (0,1))
);

CREATE TABLE AUDITORIA (
  id_auditoria NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  tabla VARCHAR2(30) NOT NULL,
  id_registro NUMBER NOT NULL,
  accion VARCHAR2(10) NOT NULL,
  id_usuario NUMBER,
  fecha TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  valores_antes CLOB,
  valores_despues CLOB,
  CONSTRAINT pk_auditoria PRIMARY KEY (id_auditoria),
  CONSTRAINT ck_auditoria_1 CHECK (accion IN ('INSERT','UPDATE','DELETE')),
  CONSTRAINT ck_auditoria_2 CHECK (valores_antes IS JSON),
  CONSTRAINT ck_auditoria_3 CHECK (valores_despues IS JSON)
);

CREATE TABLE CODIGO_RECUPERACION (
  id_codigo        NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_usuario       NUMBER        NOT NULL,
  codigo_hash      VARCHAR2(255) NOT NULL,   -- solo el hash BCrypt del código de 6 dígitos
  intentos         NUMBER(2)     DEFAULT 0 NOT NULL,
  usado            NUMBER(1)     DEFAULT 0 NOT NULL,
  fecha_creacion   TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_expiracion TIMESTAMP     NOT NULL,
  CONSTRAINT pk_codigo_recuperacion PRIMARY KEY (id_codigo),
  CONSTRAINT ck_codrec_usado CHECK (usado IN (0,1)),
  CONSTRAINT ck_codrec_intentos CHECK (intentos BETWEEN 0 AND 5),
  CONSTRAINT ck_codrec_fechas CHECK (fecha_expiracion > fecha_creacion)
);

-- =====================================================================================
-- 3. CLAVES FORÁNEAS E ÍNDICES
-- =====================================================================================
ALTER TABLE USUARIO ADD CONSTRAINT fk_cv_1 FOREIGN KEY (id_rol) REFERENCES ROL(id_rol);
ALTER TABLE USUARIO ADD CONSTRAINT fk_cv_2 FOREIGN KEY (id_idioma_pref) REFERENCES IDIOMA(id_idioma);
ALTER TABLE ARCHIVO ADD CONSTRAINT fk_cv_3 FOREIGN KEY (id_subido_por) REFERENCES USUARIO(id_usuario);
ALTER TABLE COMUNIDAD ADD CONSTRAINT fk_cv_4 FOREIGN KEY (id_categoria) REFERENCES CATEGORIA(id_categoria);
ALTER TABLE COMUNIDAD ADD CONSTRAINT fk_cv_5 FOREIGN KEY (id_idioma) REFERENCES IDIOMA(id_idioma);
ALTER TABLE COMUNIDAD ADD CONSTRAINT fk_cv_6 FOREIGN KEY (id_pueblo) REFERENCES PUEBLO_ORIGINARIO(id_pueblo);
ALTER TABLE COMUNIDAD ADD CONSTRAINT fk_cv_7 FOREIGN KEY (id_imagen) REFERENCES ARCHIVO(id_archivo);
ALTER TABLE COMUNIDAD ADD CONSTRAINT fk_cv_8 FOREIGN KEY (id_creador) REFERENCES USUARIO(id_usuario);
ALTER TABLE MIEMBRO_COMUNIDAD ADD CONSTRAINT fk_cv_9 FOREIGN KEY (id_comunidad) REFERENCES COMUNIDAD(id_comunidad);
ALTER TABLE MIEMBRO_COMUNIDAD ADD CONSTRAINT fk_cv_10 FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario);
ALTER TABLE SOLICITUD_INGRESO ADD CONSTRAINT fk_cv_11 FOREIGN KEY (id_comunidad) REFERENCES COMUNIDAD(id_comunidad);
ALTER TABLE SOLICITUD_INGRESO ADD CONSTRAINT fk_cv_12 FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario);
ALTER TABLE SOLICITUD_INGRESO ADD CONSTRAINT fk_cv_13 FOREIGN KEY (id_revisor) REFERENCES USUARIO(id_usuario);
ALTER TABLE CURSO ADD CONSTRAINT fk_cv_14 FOREIGN KEY (id_comunidad) REFERENCES COMUNIDAD(id_comunidad);
ALTER TABLE SECCION ADD CONSTRAINT fk_cv_15 FOREIGN KEY (id_curso) REFERENCES CURSO(id_curso);
ALTER TABLE SECCION ADD CONSTRAINT fk_cv_16 FOREIGN KEY (id_periodo) REFERENCES PERIODO_ACADEMICO(id_periodo);
ALTER TABLE SECCION ADD CONSTRAINT fk_cv_17 FOREIGN KEY (id_docente) REFERENCES USUARIO(id_usuario);
ALTER TABLE UNIDAD ADD CONSTRAINT fk_cv_18 FOREIGN KEY (id_seccion) REFERENCES SECCION(id_seccion);
ALTER TABLE SESION ADD CONSTRAINT fk_cv_19 FOREIGN KEY (id_unidad) REFERENCES UNIDAD(id_unidad);
ALTER TABLE RECURSO ADD CONSTRAINT fk_cv_20 FOREIGN KEY (id_unidad) REFERENCES UNIDAD(id_unidad);
ALTER TABLE RECURSO ADD CONSTRAINT fk_cv_21 FOREIGN KEY (id_sesion) REFERENCES SESION(id_sesion);
ALTER TABLE RECURSO ADD CONSTRAINT fk_cv_22 FOREIGN KEY (id_autor) REFERENCES USUARIO(id_usuario);
ALTER TABLE RECURSO ADD CONSTRAINT fk_cv_23 FOREIGN KEY (id_idioma) REFERENCES IDIOMA(id_idioma);
ALTER TABLE RECURSO ADD CONSTRAINT fk_cv_24 FOREIGN KEY (id_archivo) REFERENCES ARCHIVO(id_archivo);
ALTER TABLE EVENTO ADD CONSTRAINT fk_cv_25 FOREIGN KEY (id_comunidad) REFERENCES COMUNIDAD(id_comunidad);
ALTER TABLE EVENTO ADD CONSTRAINT fk_cv_26 FOREIGN KEY (id_seccion) REFERENCES SECCION(id_seccion);
ALTER TABLE EVENTO ADD CONSTRAINT fk_cv_27 FOREIGN KEY (id_creador) REFERENCES USUARIO(id_usuario);
ALTER TABLE MATRICULA ADD CONSTRAINT fk_cv_28 FOREIGN KEY (id_seccion) REFERENCES SECCION(id_seccion);
ALTER TABLE MATRICULA ADD CONSTRAINT fk_cv_29 FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario);
ALTER TABLE EVALUACION ADD CONSTRAINT fk_cv_30 FOREIGN KEY (id_seccion) REFERENCES SECCION(id_seccion);
ALTER TABLE EVALUACION ADD CONSTRAINT fk_cv_31 FOREIGN KEY (id_unidad) REFERENCES UNIDAD(id_unidad);
ALTER TABLE TAREA ADD CONSTRAINT fk_cv_32 FOREIGN KEY (id_recurso) REFERENCES RECURSO(id_recurso);
ALTER TABLE TAREA ADD CONSTRAINT fk_cv_33 FOREIGN KEY (id_evaluacion) REFERENCES EVALUACION(id_evaluacion);
ALTER TABLE ENTREGA_TAREA ADD CONSTRAINT fk_cv_34 FOREIGN KEY (id_recurso) REFERENCES TAREA(id_recurso);
ALTER TABLE ENTREGA_TAREA ADD CONSTRAINT fk_cv_35 FOREIGN KEY (id_matricula) REFERENCES MATRICULA(id_matricula);
ALTER TABLE ENTREGA_TAREA ADD CONSTRAINT fk_cv_36 FOREIGN KEY (id_archivo) REFERENCES ARCHIVO(id_archivo);
ALTER TABLE NOTA ADD CONSTRAINT fk_cv_37 FOREIGN KEY (id_matricula) REFERENCES MATRICULA(id_matricula);
ALTER TABLE NOTA ADD CONSTRAINT fk_cv_38 FOREIGN KEY (id_evaluacion) REFERENCES EVALUACION(id_evaluacion);
ALTER TABLE NOTA ADD CONSTRAINT fk_cv_39 FOREIGN KEY (id_registrado_por) REFERENCES USUARIO(id_usuario);
ALTER TABLE ASISTENCIA ADD CONSTRAINT fk_cv_40 FOREIGN KEY (id_sesion) REFERENCES SESION(id_sesion);
ALTER TABLE ASISTENCIA ADD CONSTRAINT fk_cv_41 FOREIGN KEY (id_matricula) REFERENCES MATRICULA(id_matricula);
ALTER TABLE ASISTENCIA ADD CONSTRAINT fk_cv_42 FOREIGN KEY (id_registrado_por) REFERENCES USUARIO(id_usuario);
ALTER TABLE PUBLICACION ADD CONSTRAINT fk_cv_43 FOREIGN KEY (id_comunidad) REFERENCES COMUNIDAD(id_comunidad);
ALTER TABLE PUBLICACION ADD CONSTRAINT fk_cv_44 FOREIGN KEY (id_seccion) REFERENCES SECCION(id_seccion);
ALTER TABLE PUBLICACION ADD CONSTRAINT fk_cv_45 FOREIGN KEY (id_autor) REFERENCES USUARIO(id_usuario);
ALTER TABLE PUBLICACION ADD CONSTRAINT fk_cv_46 FOREIGN KEY (id_moderador) REFERENCES USUARIO(id_usuario);
ALTER TABLE PUBLICACION_ARCHIVO ADD CONSTRAINT fk_cv_47 FOREIGN KEY (id_publicacion) REFERENCES PUBLICACION(id_publicacion);
ALTER TABLE PUBLICACION_ARCHIVO ADD CONSTRAINT fk_cv_48 FOREIGN KEY (id_archivo) REFERENCES ARCHIVO(id_archivo);
ALTER TABLE COMENTARIO ADD CONSTRAINT fk_cv_49 FOREIGN KEY (id_publicacion) REFERENCES PUBLICACION(id_publicacion);
ALTER TABLE COMENTARIO ADD CONSTRAINT fk_cv_50 FOREIGN KEY (id_recurso) REFERENCES RECURSO(id_recurso);
ALTER TABLE COMENTARIO ADD CONSTRAINT fk_cv_51 FOREIGN KEY (id_comentario_padre) REFERENCES COMENTARIO(id_comentario);
ALTER TABLE COMENTARIO ADD CONSTRAINT fk_cv_52 FOREIGN KEY (id_autor) REFERENCES USUARIO(id_usuario);
ALTER TABLE COMENTARIO ADD CONSTRAINT fk_cv_53 FOREIGN KEY (id_eliminado_por) REFERENCES USUARIO(id_usuario);
ALTER TABLE NOTIFICACION ADD CONSTRAINT fk_cv_54 FOREIGN KEY (id_destinatario) REFERENCES USUARIO(id_usuario);
ALTER TABLE NOTIFICACION ADD CONSTRAINT fk_cv_55 FOREIGN KEY (id_emisor) REFERENCES USUARIO(id_usuario);
ALTER TABLE NOTIFICACION ADD CONSTRAINT fk_cv_56 FOREIGN KEY (id_comunidad) REFERENCES COMUNIDAD(id_comunidad);
ALTER TABLE MENSAJE ADD CONSTRAINT fk_cv_57 FOREIGN KEY (id_emisor) REFERENCES USUARIO(id_usuario);
ALTER TABLE MENSAJE ADD CONSTRAINT fk_cv_58 FOREIGN KEY (id_receptor) REFERENCES USUARIO(id_usuario);
ALTER TABLE AUDITORIA ADD CONSTRAINT fk_cv_59 FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario);
ALTER TABLE USUARIO ADD CONSTRAINT fk_usuario_foto FOREIGN KEY (id_foto_perfil) REFERENCES ARCHIVO(id_archivo);
ALTER TABLE CODIGO_RECUPERACION ADD CONSTRAINT fk_codrec_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario);

CREATE INDEX ix_cv_60 ON USUARIO(id_rol);
CREATE INDEX ix_cv_61 ON USUARIO(id_idioma_pref);
CREATE INDEX ix_cv_62 ON ARCHIVO(id_subido_por);
CREATE INDEX ix_cv_63 ON COMUNIDAD(id_categoria);
CREATE INDEX ix_cv_64 ON COMUNIDAD(id_idioma);
CREATE INDEX ix_cv_65 ON COMUNIDAD(id_pueblo);
CREATE INDEX ix_cv_66 ON COMUNIDAD(id_imagen);
CREATE INDEX ix_cv_67 ON COMUNIDAD(id_creador);
CREATE INDEX ix_cv_68 ON MIEMBRO_COMUNIDAD(id_comunidad);
CREATE INDEX ix_cv_69 ON MIEMBRO_COMUNIDAD(id_usuario);
CREATE INDEX ix_cv_70 ON SOLICITUD_INGRESO(id_comunidad);
CREATE INDEX ix_cv_71 ON SOLICITUD_INGRESO(id_usuario);
CREATE INDEX ix_cv_72 ON SOLICITUD_INGRESO(id_revisor);
CREATE INDEX ix_cv_73 ON CURSO(id_comunidad);
CREATE INDEX ix_cv_74 ON SECCION(id_curso);
CREATE INDEX ix_cv_75 ON SECCION(id_periodo);
CREATE INDEX ix_cv_76 ON SECCION(id_docente);
CREATE INDEX ix_cv_77 ON UNIDAD(id_seccion);
CREATE INDEX ix_cv_78 ON SESION(id_unidad);
CREATE INDEX ix_cv_79 ON RECURSO(id_unidad);
CREATE INDEX ix_cv_80 ON RECURSO(id_sesion);
CREATE INDEX ix_cv_81 ON RECURSO(id_autor);
CREATE INDEX ix_cv_82 ON RECURSO(id_idioma);
CREATE INDEX ix_cv_83 ON RECURSO(id_archivo);
CREATE INDEX ix_cv_84 ON EVENTO(id_comunidad);
CREATE INDEX ix_cv_85 ON EVENTO(id_seccion);
CREATE INDEX ix_cv_86 ON EVENTO(id_creador);
CREATE INDEX ix_cv_87 ON MATRICULA(id_seccion);
CREATE INDEX ix_cv_88 ON MATRICULA(id_usuario);
CREATE INDEX ix_cv_89 ON EVALUACION(id_seccion);
CREATE INDEX ix_cv_90 ON EVALUACION(id_unidad);
CREATE INDEX ix_cv_91 ON TAREA(id_recurso);
CREATE INDEX ix_cv_92 ON TAREA(id_evaluacion);
CREATE INDEX ix_cv_93 ON ENTREGA_TAREA(id_recurso);
CREATE INDEX ix_cv_94 ON ENTREGA_TAREA(id_matricula);
CREATE INDEX ix_cv_95 ON ENTREGA_TAREA(id_archivo);
CREATE INDEX ix_cv_96 ON NOTA(id_matricula);
CREATE INDEX ix_cv_97 ON NOTA(id_evaluacion);
CREATE INDEX ix_cv_98 ON NOTA(id_registrado_por);
CREATE INDEX ix_cv_99 ON ASISTENCIA(id_sesion);
CREATE INDEX ix_cv_100 ON ASISTENCIA(id_matricula);
CREATE INDEX ix_cv_101 ON ASISTENCIA(id_registrado_por);
CREATE INDEX ix_cv_102 ON PUBLICACION(id_comunidad);
CREATE INDEX ix_cv_103 ON PUBLICACION(id_seccion);
CREATE INDEX ix_cv_104 ON PUBLICACION(id_autor);
CREATE INDEX ix_cv_105 ON PUBLICACION(id_moderador);
CREATE INDEX ix_cv_106 ON PUBLICACION_ARCHIVO(id_publicacion);
CREATE INDEX ix_cv_107 ON PUBLICACION_ARCHIVO(id_archivo);
CREATE INDEX ix_cv_108 ON COMENTARIO(id_publicacion);
CREATE INDEX ix_cv_109 ON COMENTARIO(id_recurso);
CREATE INDEX ix_cv_110 ON COMENTARIO(id_comentario_padre);
CREATE INDEX ix_cv_111 ON COMENTARIO(id_autor);
CREATE INDEX ix_cv_112 ON COMENTARIO(id_eliminado_por);
CREATE INDEX ix_cv_113 ON NOTIFICACION(id_destinatario);
CREATE INDEX ix_cv_114 ON NOTIFICACION(id_emisor);
CREATE INDEX ix_cv_115 ON NOTIFICACION(id_comunidad);
CREATE INDEX ix_cv_116 ON MENSAJE(id_emisor);
CREATE INDEX ix_cv_117 ON MENSAJE(id_receptor);
CREATE INDEX ix_cv_118 ON AUDITORIA(id_usuario);
CREATE UNIQUE INDEX ux_solicitud_pendiente ON SOLICITUD_INGRESO(CASE WHEN estado='PENDIENTE' THEN id_comunidad END, CASE WHEN estado='PENDIENTE' THEN id_usuario END);
CREATE INDEX ix_codrec_usuario ON CODIGO_RECUPERACION(id_usuario);

-- =====================================================================================
-- 4. PAQUETE PKG_COMUNIDADES
--    Funciones:      registrar_login, promedio
--    Procedimientos: resolver_solicitud, matricular, cerrar_seccion, marcar_notificaciones_leidas
-- =====================================================================================
-- Implementación reconstruida del contrato del informe; revisar y compilar como COMUNIDADES.
-- No ejecuta COMMIT: la transacción pertenece al cliente.
CREATE OR REPLACE PACKAGE pkg_comunidades AS
 FUNCTION registrar_login(p_correo VARCHAR2,p_exito BOOLEAN) RETURN VARCHAR2;
 PROCEDURE resolver_solicitud(p_id_solicitud NUMBER,p_id_revisor NUMBER,p_aprobar BOOLEAN,p_motivo VARCHAR2);
 PROCEDURE matricular(p_id_seccion NUMBER,p_id_usuario NUMBER);
 FUNCTION promedio(p_id_matricula NUMBER) RETURN NUMBER;
 PROCEDURE cerrar_seccion(p_id_seccion NUMBER);
 PROCEDURE marcar_notificaciones_leidas(p_id_usuario NUMBER);
END pkg_comunidades;
/
CREATE OR REPLACE PACKAGE BODY pkg_comunidades AS
 FUNCTION registrar_login(p_correo VARCHAR2,p_exito BOOLEAN) RETURN VARCHAR2 IS
  u usuario%ROWTYPE;
 BEGIN
  SELECT * INTO u FROM usuario WHERE correo=LOWER(TRIM(p_correo)) FOR UPDATE;
  IF u.estado<>'ACTIVO' OR u.bloqueado_hasta>SYSTIMESTAMP THEN RETURN 'BLOQUEADO'; END IF;
  IF p_exito THEN UPDATE usuario SET intentos_fallidos=0,bloqueado_hasta=NULL WHERE id_usuario=u.id_usuario; RETURN 'OK'; END IF;
  IF u.bloqueado_hasta IS NOT NULL THEN u.intentos_fallidos:=0; END IF;
  u.intentos_fallidos:=u.intentos_fallidos+1;
  UPDATE usuario SET intentos_fallidos=u.intentos_fallidos,bloqueado_hasta=CASE WHEN u.intentos_fallidos>=5 THEN SYSTIMESTAMP+INTERVAL '15' MINUTE ELSE NULL END WHERE id_usuario=u.id_usuario;
  IF u.intentos_fallidos>=5 THEN RETURN 'BLOQUEADO'; END IF;
  RETURN 'FALLIDO';
 EXCEPTION WHEN NO_DATA_FOUND THEN RETURN 'NO_EXISTE';
 END;
 PROCEDURE resolver_solicitud(p_id_solicitud NUMBER,p_id_revisor NUMBER,p_aprobar BOOLEAN,p_motivo VARCHAR2) IS
  s solicitud_ingreso%ROWTYPE; n NUMBER; v_estado VARCHAR2(10); cid NUMBER;
 BEGIN
  SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=p_id_revisor AND u.estado='ACTIVO' AND r.codigo='ADMIN';
  IF n<>1 THEN RAISE_APPLICATION_ERROR(-20023,'Revisor no autorizado'); END IF;
  SELECT * INTO s FROM solicitud_ingreso WHERE id_solicitud=p_id_solicitud FOR UPDATE;
  SELECT id_comunidad INTO cid FROM comunidad WHERE id_comunidad=s.id_comunidad AND estado='ACTIVA' FOR UPDATE;
  IF s.estado<>'PENDIENTE' THEN RAISE_APPLICATION_ERROR(-20020,'Solicitud ya resuelta'); END IF;
  IF p_aprobar THEN
   SELECT COUNT(*) INTO n FROM miembro_comunidad WHERE id_comunidad=s.id_comunidad AND id_usuario=s.id_usuario AND estado='EXPULSADO';
   IF n>0 THEN RAISE_APPLICATION_ERROR(-20023,'Usuario expulsado'); END IF;
   MERGE INTO miembro_comunidad m USING (SELECT s.id_comunidad cid,s.id_usuario uid FROM dual) x ON(m.id_comunidad=x.cid AND m.id_usuario=x.uid)
   WHEN MATCHED THEN UPDATE SET estado='ACTIVO',fecha_ingreso=SYSTIMESTAMP,fecha_salida=NULL
   WHEN NOT MATCHED THEN INSERT(id_comunidad,id_usuario,rol_comunidad,estado,fecha_ingreso) VALUES(x.cid,x.uid,'MIEMBRO','ACTIVO',SYSTIMESTAMP);
   v_estado:='APROBADA';
  ELSE
   IF TRIM(p_motivo) IS NULL THEN RAISE_APPLICATION_ERROR(-20024,'Falta motivo de rechazo'); END IF;
   v_estado:='RECHAZADA';
  END IF;
  UPDATE solicitud_ingreso SET estado=v_estado,id_revisor=p_id_revisor,fecha_respuesta=SYSTIMESTAMP,motivo_rechazo=CASE WHEN v_estado='RECHAZADA' THEN p_motivo ELSE NULL END WHERE id_solicitud=p_id_solicitud;
  INSERT INTO notificacion(id_destinatario,id_emisor,id_comunidad,tipo,titulo,entidad_tipo,entidad_id,leida,fecha_creacion)
  VALUES(s.id_usuario,p_id_revisor,s.id_comunidad,'SOLICITUD_RESPUESTA','Solicitud '||v_estado,'SOLICITUD',p_id_solicitud,0,SYSTIMESTAMP);
 END;
 PROCEDURE matricular(p_id_seccion NUMBER,p_id_usuario NUMBER) IS
  s seccion%ROWTYPE;n NUMBER;cid NUMBER;
 BEGIN
  SELECT * INTO s FROM seccion WHERE id_seccion=p_id_seccion FOR UPDATE;
  IF s.estado='CERRADA' THEN RAISE_APPLICATION_ERROR(-20012,'Sección cerrada'); END IF;
  SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=p_id_usuario AND u.estado='ACTIVO' AND r.codigo='ESTUDIANTE';
  IF n<>1 THEN RAISE_APPLICATION_ERROR(-20011,'Solo estudiante activo'); END IF;
  SELECT c.id_comunidad INTO cid FROM curso c JOIN comunidad co ON co.id_comunidad=c.id_comunidad WHERE c.id_curso=s.id_curso AND c.estado='ACTIVO' AND co.estado='ACTIVA';
  SELECT COUNT(*) INTO n FROM miembro_comunidad WHERE id_comunidad=cid AND id_usuario=p_id_usuario AND estado='ACTIVO';
  IF n<>1 THEN RAISE_APPLICATION_ERROR(-20013,'No pertenece a la comunidad'); END IF;
  SELECT COUNT(*) INTO n FROM matricula WHERE id_seccion=p_id_seccion AND estado='MATRICULADO';
  IF s.cupo_maximo IS NOT NULL AND n>=s.cupo_maximo THEN RAISE_APPLICATION_ERROR(-20021,'Sin cupo'); END IF;
  INSERT INTO matricula(id_seccion,id_usuario,fecha_matricula,estado) VALUES(p_id_seccion,p_id_usuario,SYSTIMESTAMP,'MATRICULADO');
 END;
 FUNCTION promedio(p_id_matricula NUMBER) RETURN NUMBER IS n NUMBER;
 BEGIN
  SELECT NVL(SUM(n.calificacion*e.peso/100),0) INTO n FROM nota n JOIN evaluacion e ON e.id_evaluacion=n.id_evaluacion WHERE n.id_matricula=p_id_matricula;
  RETURN ROUND(n,2);
 END;
 PROCEDURE cerrar_seccion(p_id_seccion NUMBER) IS n NUMBER;v_estado VARCHAR2(10);p NUMBER;
 BEGIN
  SELECT estado INTO v_estado FROM seccion WHERE id_seccion=p_id_seccion FOR UPDATE;
  IF v_estado='CERRADA' THEN RAISE_APPLICATION_ERROR(-20012,'Sección ya cerrada'); END IF;
  SELECT NVL(SUM(peso),0) INTO n FROM evaluacion WHERE id_seccion=p_id_seccion;
  IF n<>100 THEN RAISE_APPLICATION_ERROR(-20022,'Pesos deben sumar 100'); END IF;
  FOR m IN(SELECT id_matricula FROM matricula WHERE id_seccion=p_id_seccion AND estado='MATRICULADO' FOR UPDATE) LOOP
   p:=promedio(m.id_matricula);
   UPDATE matricula SET nota_final=p,estado=CASE WHEN p>=10.5 THEN 'APROBADO' ELSE 'DESAPROBADO' END WHERE id_matricula=m.id_matricula;
  END LOOP;
  UPDATE seccion SET estado='CERRADA' WHERE id_seccion=p_id_seccion;
 END;
 PROCEDURE marcar_notificaciones_leidas(p_id_usuario NUMBER) IS
 BEGIN UPDATE notificacion SET leida=1,fecha_lectura=SYSTIMESTAMP WHERE id_destinatario=p_id_usuario AND leida=0; END;
END pkg_comunidades;
/
SHOW ERRORS

-- =====================================================================================
-- 5. TRIGGERS (14) Y VISTAS (3)
--    Validación: trg_seccion_docente, trg_matricula_valida, trg_asistencia_valida, trg_nota_valida,
--                trg_recurso_valida, trg_publicacion_valida, trg_comentario_valida (compound)
--    Mantenimiento: trg_usuario_actualizacion
--    Notificación:  trg_notif_solicitud, trg_notif_publicacion, trg_notif_comentario
--    Auditoría:     trg_auditoria_nota, trg_auditoria_publicacion, trg_auditoria_comentario
--    Nota: el backend también genera notificaciones y auditoría; al instalar los triggers
--          configurar DB_TRIGGERS_ACTIVOS=true para no duplicarlas.
-- =====================================================================================
-- Revisar con DBA; instalar únicamente tras validar el diccionario de datos.
CREATE OR REPLACE TRIGGER trg_seccion_docente BEFORE INSERT OR UPDATE OF id_docente ON seccion FOR EACH ROW
DECLARE n NUMBER;BEGIN SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=:NEW.id_docente AND r.codigo='DOCENTE' AND u.estado='ACTIVO';IF n<>1 THEN RAISE_APPLICATION_ERROR(-20010,'El titular debe ser docente activo');END IF;END;
/
CREATE OR REPLACE TRIGGER trg_matricula_valida BEFORE INSERT ON matricula FOR EACH ROW
DECLARE n NUMBER;cid NUMBER;st VARCHAR2(10);BEGIN
 SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=:NEW.id_usuario AND u.estado='ACTIVO' AND r.codigo='ESTUDIANTE';
 IF n<>1 THEN RAISE_APPLICATION_ERROR(-20011,'Solo estudiante activo');END IF;
 SELECT s.estado,c.id_comunidad INTO st,cid FROM seccion s JOIN curso c ON c.id_curso=s.id_curso WHERE s.id_seccion=:NEW.id_seccion;
 IF st='CERRADA' THEN RAISE_APPLICATION_ERROR(-20012,'Sección cerrada');END IF;
 SELECT COUNT(*) INTO n FROM miembro_comunidad WHERE id_usuario=:NEW.id_usuario AND id_comunidad=cid AND estado='ACTIVO';
 IF n<>1 THEN RAISE_APPLICATION_ERROR(-20013,'No pertenece a comunidad');END IF;
END;
/
CREATE OR REPLACE TRIGGER trg_asistencia_valida BEFORE INSERT OR UPDATE ON asistencia FOR EACH ROW
DECLARE a NUMBER;b NUMBER;BEGIN
 SELECT u.id_seccion INTO a FROM sesion s JOIN unidad u ON u.id_unidad=s.id_unidad WHERE s.id_sesion=:NEW.id_sesion;
 SELECT id_seccion INTO b FROM matricula WHERE id_matricula=:NEW.id_matricula;
 IF a<>b THEN RAISE_APPLICATION_ERROR(-20014,'Sesión y matrícula de distintas secciones');END IF;
END;
/
CREATE OR REPLACE TRIGGER trg_nota_valida BEFORE INSERT OR UPDATE ON nota FOR EACH ROW
DECLARE a NUMBER;b NUMBER;BEGIN
 SELECT id_seccion INTO a FROM evaluacion WHERE id_evaluacion=:NEW.id_evaluacion;
 SELECT id_seccion INTO b FROM matricula WHERE id_matricula=:NEW.id_matricula;
 IF a<>b THEN RAISE_APPLICATION_ERROR(-20015,'Evaluación y matrícula de distintas secciones');END IF;
 IF UPDATING THEN :NEW.fecha_modificacion:=SYSTIMESTAMP;END IF;
END;
/
CREATE OR REPLACE TRIGGER trg_recurso_valida BEFORE INSERT OR UPDATE ON recurso FOR EACH ROW
DECLARE a NUMBER;BEGIN
 IF :NEW.id_sesion IS NOT NULL THEN SELECT id_unidad INTO a FROM sesion WHERE id_sesion=:NEW.id_sesion;IF a<>:NEW.id_unidad THEN RAISE_APPLICATION_ERROR(-20016,'Sesión de otra unidad');END IF;END IF;
 IF :NEW.eliminado=1 AND :NEW.fecha_eliminacion IS NULL THEN :NEW.fecha_eliminacion:=SYSTIMESTAMP;END IF;
END;
/
CREATE OR REPLACE TRIGGER trg_publicacion_valida BEFORE INSERT ON publicacion FOR EACH ROW
DECLARE n NUMBER;moderada NUMBER;BEGIN
 SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=:NEW.id_autor AND u.estado='ACTIVO' AND r.codigo IN('ESTUDIANTE','DOCENTE','ADMIN');
 IF n<>1 THEN RAISE_APPLICATION_ERROR(-20017,'Solo usuarios activos publican');END IF;  -- iteración 1: el estudiante también publica
 SELECT moderacion_previa INTO moderada FROM comunidad WHERE id_comunidad=:NEW.id_comunidad;
 IF moderada=1 THEN :NEW.estado:='PENDIENTE';END IF;
END;
/
-- Compound trigger: consulta COMENTARIO después de terminar la sentencia para evitar ORA-04091.
CREATE OR REPLACE TRIGGER trg_comentario_valida FOR INSERT OR UPDATE OF id_comentario_padre,id_publicacion,id_recurso ON comentario COMPOUND TRIGGER
 TYPE fila IS RECORD(id NUMBER,padre NUMBER,publicacion NUMBER,recurso NUMBER);
 TYPE filas IS TABLE OF fila INDEX BY PLS_INTEGER;
 datos filas;contador PLS_INTEGER:=0;
 AFTER EACH ROW IS BEGIN
  IF :NEW.id_comentario_padre IS NOT NULL THEN contador:=contador+1;datos(contador).id:=:NEW.id_comentario;datos(contador).padre:=:NEW.id_comentario_padre;datos(contador).publicacion:=:NEW.id_publicacion;datos(contador).recurso:=:NEW.id_recurso;END IF;
 END AFTER EACH ROW;
 AFTER STATEMENT IS p NUMBER;r NUMBER;e NUMBER;BEGIN
  FOR i IN 1..contador LOOP
   SELECT id_publicacion,id_recurso,eliminado INTO p,r,e FROM comentario WHERE id_comentario=datos(i).padre;
   IF datos(i).id=datos(i).padre OR e=1 OR NVL(p,-1)<>NVL(datos(i).publicacion,-1) OR NVL(r,-1)<>NVL(datos(i).recurso,-1) THEN RAISE_APPLICATION_ERROR(-20018,'Respuesta fuera del destino o padre inválido');END IF;
  END LOOP;
 END AFTER STATEMENT;
END;
/
CREATE OR REPLACE TRIGGER trg_usuario_actualizacion BEFORE UPDATE ON usuario FOR EACH ROW
BEGIN :NEW.fecha_actualizacion:=SYSTIMESTAMP;END;
/
CREATE OR REPLACE TRIGGER trg_notif_solicitud AFTER INSERT ON solicitud_ingreso FOR EACH ROW
BEGIN
 INSERT INTO notificacion(id_destinatario,id_emisor,id_comunidad,tipo,titulo,entidad_tipo,entidad_id,leida,fecha_creacion)
 SELECT u.id_usuario,:NEW.id_usuario,:NEW.id_comunidad,'SOLICITUD_INGRESO','Nueva solicitud de ingreso','SOLICITUD',:NEW.id_solicitud,0,SYSTIMESTAMP FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE r.codigo='ADMIN' AND u.estado='ACTIVO' AND u.id_usuario<>:NEW.id_usuario;
END;
/
CREATE OR REPLACE TRIGGER trg_notif_publicacion AFTER INSERT OR UPDATE OF estado ON publicacion FOR EACH ROW
BEGIN
 IF :NEW.estado='APROBADA' AND :NEW.eliminado=0 AND (INSERTING OR :OLD.estado<>'APROBADA') THEN
 INSERT INTO notificacion(id_destinatario,id_emisor,id_comunidad,tipo,titulo,entidad_tipo,entidad_id,leida,fecha_creacion)
 SELECT id_usuario,:NEW.id_autor,:NEW.id_comunidad,'NUEVA_PUBLICACION','Nueva publicación educativa','PUBLICACION',:NEW.id_publicacion,0,SYSTIMESTAMP FROM miembro_comunidad WHERE id_comunidad=:NEW.id_comunidad AND estado='ACTIVO' AND id_usuario<>:NEW.id_autor;
 END IF;
END;
/
CREATE OR REPLACE TRIGGER trg_notif_comentario AFTER INSERT ON comentario FOR EACH ROW
DECLARE autor NUMBER;cid NUMBER;BEGIN
 IF :NEW.id_publicacion IS NOT NULL THEN SELECT id_autor,id_comunidad INTO autor,cid FROM publicacion WHERE id_publicacion=:NEW.id_publicacion;
 ELSE SELECT r.id_autor,c.id_comunidad INTO autor,cid FROM recurso r JOIN unidad u ON u.id_unidad=r.id_unidad JOIN seccion s ON s.id_seccion=u.id_seccion JOIN curso c ON c.id_curso=s.id_curso WHERE r.id_recurso=:NEW.id_recurso;END IF;
 IF autor<>:NEW.id_autor THEN INSERT INTO notificacion(id_destinatario,id_emisor,id_comunidad,tipo,titulo,entidad_tipo,entidad_id,leida,fecha_creacion) VALUES(autor,:NEW.id_autor,cid,'NUEVO_COMENTARIO','Nuevo comentario','COMENTARIO',:NEW.id_comentario,0,SYSTIMESTAMP);END IF;
END;
/
CREATE OR REPLACE TRIGGER trg_auditoria_nota AFTER INSERT OR UPDATE OR DELETE ON nota FOR EACH ROW
DECLARE accion VARCHAR2(10);antes CLOB;despues CLOB;BEGIN
 IF INSERTING THEN accion:='INSERT';ELSIF UPDATING THEN accion:='UPDATE';ELSE accion:='DELETE';END IF;
 IF NOT INSERTING THEN SELECT JSON_OBJECT('calificacion' VALUE :OLD.calificacion RETURNING CLOB) INTO antes FROM dual;END IF;
 IF NOT DELETING THEN SELECT JSON_OBJECT('calificacion' VALUE :NEW.calificacion RETURNING CLOB) INTO despues FROM dual;END IF;
 INSERT INTO auditoria(tabla,id_registro,accion,id_usuario,fecha,valores_antes,valores_despues) VALUES('NOTA',NVL(:NEW.id_nota,:OLD.id_nota),accion,NVL(:NEW.id_registrado_por,:OLD.id_registrado_por),SYSTIMESTAMP,antes,despues);
END;
/
CREATE OR REPLACE TRIGGER trg_auditoria_publicacion AFTER UPDATE OR DELETE ON publicacion FOR EACH ROW
DECLARE accion VARCHAR2(10);antes CLOB;despues CLOB;BEGIN
 IF UPDATING THEN accion:='UPDATE';ELSE accion:='DELETE';END IF;
 SELECT JSON_OBJECT('estado' VALUE :OLD.estado,'eliminado' VALUE :OLD.eliminado RETURNING CLOB) INTO antes FROM dual;
 IF UPDATING THEN SELECT JSON_OBJECT('estado' VALUE :NEW.estado,'eliminado' VALUE :NEW.eliminado RETURNING CLOB) INTO despues FROM dual;END IF;
 INSERT INTO auditoria(tabla,id_registro,accion,id_usuario,fecha,valores_antes,valores_despues) VALUES('PUBLICACION',:OLD.id_publicacion,accion,NVL(:NEW.id_moderador,:OLD.id_autor),SYSTIMESTAMP,antes,despues);
END;
/
CREATE OR REPLACE TRIGGER trg_auditoria_comentario AFTER UPDATE OR DELETE ON comentario FOR EACH ROW
DECLARE accion VARCHAR2(10);antes CLOB;despues CLOB;BEGIN
 IF UPDATING THEN accion:='UPDATE';ELSE accion:='DELETE';END IF;
 SELECT JSON_OBJECT('eliminado' VALUE :OLD.eliminado RETURNING CLOB) INTO antes FROM dual;
 IF UPDATING THEN SELECT JSON_OBJECT('eliminado' VALUE :NEW.eliminado RETURNING CLOB) INTO despues FROM dual;END IF;
 INSERT INTO auditoria(tabla,id_registro,accion,id_usuario,fecha,valores_antes,valores_despues) VALUES('COMENTARIO',:OLD.id_comentario,accion,NVL(:NEW.id_eliminado_por,:OLD.id_autor),SYSTIMESTAMP,antes,despues);
END;
/
CREATE OR REPLACE VIEW v_comunidades_activas AS
SELECT c.id_comunidad,c.nombre,c.descripcion,c.visibilidad,c.id_categoria,ca.nombre categoria,c.id_idioma,i.nombre idioma,c.id_pueblo,p.nombre pueblo,
 (SELECT COUNT(*) FROM miembro_comunidad m WHERE m.id_comunidad=c.id_comunidad AND m.estado='ACTIVO') miembros
FROM comunidad c JOIN categoria ca ON ca.id_categoria=c.id_categoria LEFT JOIN idioma i ON i.id_idioma=c.id_idioma LEFT JOIN pueblo_originario p ON p.id_pueblo=c.id_pueblo WHERE c.estado='ACTIVA';
CREATE OR REPLACE VIEW v_muro_comunidad AS
SELECT p.id_publicacion,p.id_comunidad,p.id_autor,p.titulo,p.contenido,p.fecha_publicacion,u.nombres,u.apellidos,r.codigo rol,
 (SELECT COUNT(*) FROM comentario c WHERE c.id_publicacion=p.id_publicacion AND c.eliminado=0) comentarios,
 (SELECT COUNT(*) FROM publicacion_archivo a WHERE a.id_publicacion=p.id_publicacion) adjuntos
FROM publicacion p JOIN usuario u ON u.id_usuario=p.id_autor JOIN rol r ON r.id_rol=u.id_rol WHERE p.estado='APROBADA' AND p.eliminado=0;
CREATE OR REPLACE VIEW v_reporte_seccion AS
SELECT m.id_matricula,m.id_seccion,m.id_usuario,u.nombres,u.apellidos,m.estado,pkg_comunidades.promedio(m.id_matricula) nota_acumulada,
 (SELECT NVL(SUM(e.peso),0) FROM nota n JOIN evaluacion e ON e.id_evaluacion=n.id_evaluacion WHERE n.id_matricula=m.id_matricula) peso_evaluado,
 (SELECT ROUND(100*SUM(CASE WHEN a.estado IN('PRESENTE','TARDANZA') THEN 1 ELSE 0 END)/NULLIF(COUNT(*),0),2) FROM asistencia a WHERE a.id_matricula=m.id_matricula) porcentaje_asistencia
FROM matricula m JOIN usuario u ON u.id_usuario=m.id_usuario;
SHOW ERRORS

-- =====================================================================================
-- 6. PERMISOS DEL USUARIO DE EJECUCIÓN (como COMUNIDADES)
-- =====================================================================================
-- Ejecutar como COMUNIDADES después de crear APP_COMUNIDADES.
-- JPA debe usar current_schema=COMUNIDADES (ver README).
-- El DBA crea R_COMUNIDADES_APP en 00b_usuario_runtime_dba.sql.
GRANT SELECT ON ROL TO R_COMUNIDADES_APP;
GRANT SELECT ON CATEGORIA TO R_COMUNIDADES_APP;
GRANT SELECT ON IDIOMA TO R_COMUNIDADES_APP;
GRANT SELECT ON PUEBLO_ORIGINARIO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON USUARIO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON ARCHIVO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON COMUNIDAD TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON MIEMBRO_COMUNIDAD TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON SOLICITUD_INGRESO TO R_COMUNIDADES_APP;
GRANT SELECT ON PERIODO_ACADEMICO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON CURSO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON SECCION TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON UNIDAD TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON SESION TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON RECURSO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON EVENTO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON MATRICULA TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON EVALUACION TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON TAREA TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON ENTREGA_TAREA TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON NOTA TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON ASISTENCIA TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON PUBLICACION TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE, DELETE ON PUBLICACION_ARCHIVO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON COMENTARIO TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON NOTIFICACION TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON MENSAJE TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON AUDITORIA TO R_COMUNIDADES_APP;
GRANT SELECT, INSERT, UPDATE ON CODIGO_RECUPERACION TO R_COMUNIDADES_APP;
GRANT EXECUTE ON PKG_COMUNIDADES TO R_COMUNIDADES_APP;
GRANT R_COMUNIDADES_APP TO APP_COMUNIDADES;
-- Si COMUNIDADES no tiene CREATE ROLE, el DBA crea el rol antes de ejecutar los GRANT.

-- =====================================================================================
-- 7. CATÁLOGOS INICIALES
-- =====================================================================================
-- Opcional en esquema nuevo. Catálogos mínimos; no modifica usuarios existentes.
MERGE INTO rol r USING (SELECT 'ADMIN' codigo,'Administrador' nombre FROM dual UNION ALL SELECT 'DOCENTE','Docente' FROM dual UNION ALL SELECT 'ESTUDIANTE','Estudiante' FROM dual) x
ON(r.codigo=x.codigo) WHEN NOT MATCHED THEN INSERT(codigo,nombre) VALUES(x.codigo,x.nombre);
MERGE INTO idioma i USING (SELECT 'es' codigo,'Castellano' nombre FROM dual UNION ALL SELECT 'qu','Quechua' FROM dual UNION ALL SELECT 'cni','Asháninka' FROM dual) x
ON(i.codigo=x.codigo) WHEN NOT MATCHED THEN INSERT(codigo,nombre) VALUES(x.codigo,x.nombre);
MERGE INTO categoria c USING (SELECT 'Educación intercultural' nombre FROM dual) x ON(c.nombre=x.nombre)
WHEN NOT MATCHED THEN INSERT(nombre,descripcion) VALUES(x.nombre,'Recursos y saberes de comunidades originarias');
-- Revisar resultados antes de COMMIT.

COMMIT;

-- =====================================================================================
-- 8. AMBIENTE DE DESARROLLO COMU_VIR_DESA (clon del esquema con Data Pump)
--    En el CMD del servidor (Oracle crea COMU_VIR_DESA al importar):
--    expdp system/******** dumpfile=EXP_COMU_VIR.dmp schemas=comunidades directory=dp_comu_vir logfile=exp_comu_vir.log
--    impdp system/******** dumpfile=EXP_COMU_VIR.dmp remap_schema=comunidades:comu_vir_desa directory=dp_comu_vir logfile=imp_comu_vir_desa.log
--    Directorio (como SYSTEM):  CREATE OR REPLACE DIRECTORY dp_comu_vir AS 'C:\dp_comu_vir';
--    Ver 09_clonar_comu_vir_desa.sql para la versión con DBMS_DATAPUMP desde SQL Developer.
-- =====================================================================================

-- =====================================================================================
-- 9. VERIFICACIÓN (como COMUNIDADES)
-- =====================================================================================
SELECT COUNT(*) AS tablas FROM user_tables WHERE table_name NOT LIKE 'BK\_%' ESCAPE '\' AND table_name <> 'DBTOOLS$MCP_LOG';
SELECT object_type, COUNT(*) total, SUM(CASE WHEN status='VALID' THEN 1 ELSE 0 END) validos
FROM user_objects WHERE object_type IN ('TABLE','INDEX','PACKAGE','PACKAGE BODY','TRIGGER','VIEW') GROUP BY object_type ORDER BY 1;
SELECT name, type, line, text FROM user_errors ORDER BY name, sequence;
-- DDL exacto desde el diccionario (para comparar con este script):
-- SELECT DBMS_METADATA.GET_DDL('TABLE', table_name) FROM user_tables ORDER BY table_name;

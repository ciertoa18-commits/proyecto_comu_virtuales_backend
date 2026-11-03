-- RECONSTRUCCIÓN desde informe-oracle.md; solo esquema VACÍO.
-- No ejecutar sobre COMUNIDADES existente. Comparar con DBMS_METADATA antes de usar como respaldo.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

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
  CONSTRAINT ck_usuario_1 CHECK (REGEXP_LIKE(correo, '^[^@ ]+@(minedu\.gob\.pe|perueduca\.pe)$')),
  CONSTRAINT ck_usuario_2 CHECK (correo = LOWER(correo)),
  CONSTRAINT ck_usuario_3 CHECK (keycloak_sub IS NOT NULL OR password_hash IS NOT NULL),
  CONSTRAINT ck_usuario_4 CHECK (intentos_fallidos >= 0),
  CONSTRAINT ck_usuario_5 CHECK (estado IN ('ACTIVO','INACTIVO')),
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
  CONSTRAINT ck_notificacion_1 CHECK (tipo IN ('SOLICITUD_INGRESO','SOLICITUD_RESPUESTA', 'NUEVA_PUBLICACION','NUEVO_COMENTARIO','NUEVO_RECURSO','EVENTO','MENSAJE','SISTEMA')),
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

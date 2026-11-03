-- =====================================================================
-- Comunidades Virtuales - Cambios de la Iteración 1 (ya aplicados en comunidades_aws2)
-- Registro con aprobación de docentes, avisos y recuperación de contraseña
-- =====================================================================

-- 1) Docente que se registra queda PENDIENTE hasta que un admin lo apruebe
ALTER TABLE usuario DROP CONSTRAINT ck_usuario_estado;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_estado
  CHECK (estado IN ('ACTIVO','INACTIVO','PENDIENTE'));

-- 2) Teléfono opcional (9 dígitos, Perú). Solo dato de perfil, no se envían SMS
ALTER TABLE usuario ADD (telefono VARCHAR2(15));
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_telefono
  CHECK (telefono IS NULL OR REGEXP_LIKE(telefono, '^[0-9]{9}$'));

-- 3) Nuevo tipo de notificación para los avisos enviados por admin/docente
ALTER TABLE notificacion DROP CONSTRAINT ck_notif_tipo;
ALTER TABLE notificacion ADD CONSTRAINT ck_notif_tipo CHECK (tipo IN (
  'SOLICITUD_INGRESO','SOLICITUD_RESPUESTA','NUEVA_PUBLICACION','NUEVO_COMENTARIO',
  'NUEVO_RECURSO','EVENTO','MENSAJE','SISTEMA','AVISO'));

-- 4) Códigos de recuperación de contraseña (se guarda solo el hash BCrypt del código)
CREATE TABLE codigo_recuperacion (
  id_codigo        NUMBER GENERATED ALWAYS AS IDENTITY,
  id_usuario       NUMBER        NOT NULL,
  codigo_hash      VARCHAR2(255) NOT NULL,
  intentos         NUMBER(2)     DEFAULT 0 NOT NULL,
  usado            NUMBER(1)     DEFAULT 0 NOT NULL,
  fecha_creacion   TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  fecha_expiracion TIMESTAMP     NOT NULL,
  CONSTRAINT pk_codigo_recuperacion PRIMARY KEY (id_codigo),
  CONSTRAINT fk_codrec_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  CONSTRAINT ck_codrec_usado CHECK (usado IN (0,1)),
  CONSTRAINT ck_codrec_intentos CHECK (intentos BETWEEN 0 AND 5),
  CONSTRAINT ck_codrec_fechas CHECK (fecha_expiracion > fecha_creacion)
);
CREATE INDEX ix_codrec_usuario ON codigo_recuperacion (id_usuario);

-- 5) Correo institucional según el rol (id_rol: 1 ESTUDIANTE, 2 DOCENTE, 3 ADMIN)
--    ESTUDIANTE -> @perueduca.pe ; DOCENTE y ADMIN -> @minedu.gob.pe
--    (el 2026-11-02 se vaciaron los usuarios y se recargaron con 08_datos_prueba_por_rol.sql)
ALTER TABLE usuario DROP CONSTRAINT ck_usuario_correo_dominio;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_correo_rol CHECK (
  (id_rol = 1 AND REGEXP_LIKE(correo, '^[a-z0-9._%+-]+@perueduca\.pe$')) OR
  (id_rol IN (2,3) AND REGEXP_LIKE(correo, '^[a-z0-9._%+-]+@minedu\.gob\.pe$')));

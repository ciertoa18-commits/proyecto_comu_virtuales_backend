-- Iteración 2 · CU13 Proteger cuenta
-- Correo personal (por ejemplo, Gmail) donde llega el código de recuperación,
-- porque los correos institucionales @perueduca.pe / @minedu.gob.pe son solo de identificación.
ALTER TABLE usuario ADD correo_recuperacion VARCHAR2(150);

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_correo_recup
  CHECK (correo_recuperacion IS NULL
         OR REGEXP_LIKE(correo_recuperacion, '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'));

COMMENT ON COLUMN usuario.correo_recuperacion IS 'Correo personal (Gmail) para recibir el código de recuperación de contraseña';

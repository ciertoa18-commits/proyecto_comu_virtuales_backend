CREATE TABLE PERMISO_NOTA (
  id_permiso NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_seccion NUMBER NOT NULL,
  id_docente NUMBER NOT NULL,
  motivo VARCHAR2(500) NOT NULL,
  estado VARCHAR2(10) DEFAULT 'PENDIENTE' NOT NULL,
  fecha_solicitud TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  id_revisor NUMBER,
  respuesta VARCHAR2(500),
  fecha_respuesta TIMESTAMP,
  vence TIMESTAMP,
  CONSTRAINT pk_permiso_nota PRIMARY KEY (id_permiso),
  CONSTRAINT ck_permiso_nota_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
  CONSTRAINT fk_permiso_nota_seccion FOREIGN KEY (id_seccion) REFERENCES SECCION (id_seccion) ON DELETE CASCADE,
  CONSTRAINT fk_permiso_nota_docente FOREIGN KEY (id_docente) REFERENCES USUARIO (id_usuario),
  CONSTRAINT fk_permiso_nota_revisor FOREIGN KEY (id_revisor) REFERENCES USUARIO (id_usuario)
);

CREATE INDEX ix_permiso_nota_seccion ON PERMISO_NOTA (id_seccion, id_docente);

COMMIT;

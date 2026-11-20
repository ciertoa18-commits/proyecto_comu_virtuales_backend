CREATE TABLE USUARIO_IDIOMA (
  id_usuario NUMBER NOT NULL,
  id_idioma NUMBER NOT NULL,
  CONSTRAINT pk_usuario_idioma PRIMARY KEY (id_usuario, id_idioma),
  CONSTRAINT fk_usuario_idioma_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id_usuario) ON DELETE CASCADE,
  CONSTRAINT fk_usuario_idioma_idioma FOREIGN KEY (id_idioma) REFERENCES IDIOMA (id_idioma)
);

CREATE INDEX ix_usuario_idioma_idioma ON USUARIO_IDIOMA (id_idioma);

INSERT INTO USUARIO_IDIOMA (id_usuario, id_idioma)
SELECT id_usuario, id_idioma_pref FROM USUARIO WHERE id_idioma_pref IS NOT NULL;

COMMIT;

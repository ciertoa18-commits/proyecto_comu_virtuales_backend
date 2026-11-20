CREATE TABLE SILABO (
  id_silabo NUMBER GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_seccion NUMBER NOT NULL,
  sumilla VARCHAR2(4000),
  competencias VARCHAR2(4000),
  contenidos VARCHAR2(4000),
  metodologia VARCHAR2(4000),
  evaluacion VARCHAR2(4000),
  bibliografia VARCHAR2(4000),
  id_archivo NUMBER,
  id_autor NUMBER NOT NULL,
  fecha_actualizacion TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_silabo PRIMARY KEY (id_silabo),
  CONSTRAINT uq_silabo_seccion UNIQUE (id_seccion),
  CONSTRAINT fk_silabo_seccion FOREIGN KEY (id_seccion) REFERENCES SECCION (id_seccion) ON DELETE CASCADE,
  CONSTRAINT fk_silabo_archivo FOREIGN KEY (id_archivo) REFERENCES ARCHIVO (id_archivo),
  CONSTRAINT fk_silabo_autor FOREIGN KEY (id_autor) REFERENCES USUARIO (id_usuario)
);

COMMIT;

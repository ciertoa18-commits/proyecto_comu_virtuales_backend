CREATE TABLE COMUNIDAD_IDIOMA (
  id_comunidad NUMBER NOT NULL,
  id_idioma NUMBER NOT NULL,
  CONSTRAINT pk_comunidad_idioma PRIMARY KEY (id_comunidad, id_idioma),
  CONSTRAINT fk_comunidad_idioma_comunidad FOREIGN KEY (id_comunidad) REFERENCES COMUNIDAD (id_comunidad) ON DELETE CASCADE,
  CONSTRAINT fk_comunidad_idioma_idioma FOREIGN KEY (id_idioma) REFERENCES IDIOMA (id_idioma)
);

CREATE INDEX ix_comunidad_idioma_idioma ON COMUNIDAD_IDIOMA (id_idioma);

MERGE INTO PUEBLO_ORIGINARIO p
USING (
  SELECT 'Achuar' nombre, 'Jíbaro' familia FROM dual UNION ALL
  SELECT 'Aimara', 'Aru' FROM dual UNION ALL
  SELECT 'Amahuaca', 'Pano' FROM dual UNION ALL
  SELECT 'Arabela', 'Záparo' FROM dual UNION ALL
  SELECT 'Asháninka', 'Arawak' FROM dual UNION ALL
  SELECT 'Asheninka', 'Arawak' FROM dual UNION ALL
  SELECT 'Awajún', 'Jíbaro' FROM dual UNION ALL
  SELECT 'Bora', 'Bora' FROM dual UNION ALL
  SELECT 'Chamicuro', 'Arawak' FROM dual UNION ALL
  SELECT 'Chapra', 'Cahuapana' FROM dual UNION ALL
  SELECT 'Chitonahua', 'Pano' FROM dual UNION ALL
  SELECT 'Ese Eja', 'Tacana' FROM dual UNION ALL
  SELECT 'Harakbut', 'Harakbut' FROM dual UNION ALL
  SELECT 'Ikitu', 'Záparo' FROM dual UNION ALL
  SELECT 'Iñapari', 'Arawak' FROM dual UNION ALL
  SELECT 'Iskonawa', 'Pano' FROM dual UNION ALL
  SELECT 'Jaqaru', 'Aru' FROM dual UNION ALL
  SELECT 'Kakataibo', 'Pano' FROM dual UNION ALL
  SELECT 'Kakinte', 'Arawak' FROM dual UNION ALL
  SELECT 'Kandozi', 'Kandozi' FROM dual UNION ALL
  SELECT 'Kapanawa', 'Pano' FROM dual UNION ALL
  SELECT 'Kichwa', 'Quechua' FROM dual UNION ALL
  SELECT 'Kukama Kukamiria', 'Tupí-Guaraní' FROM dual UNION ALL
  SELECT 'Madija', 'Arawa' FROM dual UNION ALL
  SELECT 'Maijuna', 'Tucano' FROM dual UNION ALL
  SELECT 'Marinahua', 'Pano' FROM dual UNION ALL
  SELECT 'Mashco Piro', 'Arawak' FROM dual UNION ALL
  SELECT 'Mastanahua', 'Pano' FROM dual UNION ALL
  SELECT 'Matsigenka', 'Arawak' FROM dual UNION ALL
  SELECT 'Matsés', 'Pano' FROM dual UNION ALL
  SELECT 'Muniche', 'Muniche' FROM dual UNION ALL
  SELECT 'Murui-Muinani', 'Huitoto' FROM dual UNION ALL
  SELECT 'Nahua', 'Pano' FROM dual UNION ALL
  SELECT 'Nanti', 'Arawak' FROM dual UNION ALL
  SELECT 'Nomatsigenga', 'Arawak' FROM dual UNION ALL
  SELECT 'Ocaina', 'Huitoto' FROM dual UNION ALL
  SELECT 'Omagua', 'Tupí-Guaraní' FROM dual UNION ALL
  SELECT 'Quechuas', 'Quechua' FROM dual UNION ALL
  SELECT 'Resígaro', 'Arawak' FROM dual UNION ALL
  SELECT 'Secoya', 'Tucano' FROM dual UNION ALL
  SELECT 'Sharanahua', 'Pano' FROM dual UNION ALL
  SELECT 'Shawi', 'Cahuapana' FROM dual UNION ALL
  SELECT 'Shipibo-Konibo', 'Pano' FROM dual UNION ALL
  SELECT 'Shiwilu', 'Cahuapana' FROM dual UNION ALL
  SELECT 'Ticuna', 'Ticuna' FROM dual UNION ALL
  SELECT 'Urarina', 'Urarina' FROM dual UNION ALL
  SELECT 'Uro', 'Uru-Chipaya' FROM dual UNION ALL
  SELECT 'Vacacocha', 'Záparo' FROM dual UNION ALL
  SELECT 'Wampis', 'Jíbaro' FROM dual UNION ALL
  SELECT 'Yagua', 'Peba-Yagua' FROM dual UNION ALL
  SELECT 'Yaminahua', 'Pano' FROM dual UNION ALL
  SELECT 'Yanesha', 'Arawak' FROM dual UNION ALL
  SELECT 'Yine', 'Arawak' FROM dual UNION ALL
  SELECT 'Yora', 'Pano' FROM dual
) x ON (UPPER(p.nombre) = UPPER(x.nombre))
WHEN MATCHED THEN UPDATE SET p.familia_linguistica = x.familia
WHEN NOT MATCHED THEN INSERT (nombre, familia_linguistica) VALUES (x.nombre, x.familia);

COMMIT;

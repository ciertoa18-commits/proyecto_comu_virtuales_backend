-- Opcional en esquema nuevo. Catálogos mínimos; no modifica usuarios existentes.
MERGE INTO rol r USING (SELECT 'ADMIN' codigo,'Administrador' nombre FROM dual UNION ALL SELECT 'DOCENTE','Docente' FROM dual UNION ALL SELECT 'ESTUDIANTE','Estudiante' FROM dual) x
ON(r.codigo=x.codigo) WHEN NOT MATCHED THEN INSERT(codigo,nombre) VALUES(x.codigo,x.nombre);
MERGE INTO idioma i USING (SELECT 'es' codigo,'Castellano' nombre FROM dual UNION ALL SELECT 'qu','Quechua' FROM dual UNION ALL SELECT 'cni','Asháninka' FROM dual) x
ON(i.codigo=x.codigo) WHEN NOT MATCHED THEN INSERT(codigo,nombre) VALUES(x.codigo,x.nombre);
MERGE INTO categoria c USING (SELECT 'Educación intercultural' nombre FROM dual) x ON(c.nombre=x.nombre)
WHEN NOT MATCHED THEN INSERT(nombre,descripcion) VALUES(x.nombre,'Recursos y saberes de comunidades originarias');
-- Revisar resultados antes de COMMIT.

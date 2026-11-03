-- Solo lectura. Ejecutar como propietario tras instalación.
SELECT table_name FROM user_tables ORDER BY table_name;
SELECT object_name, object_type, status FROM user_objects WHERE object_type IN ('TRIGGER','PACKAGE','PACKAGE BODY','VIEW') ORDER BY object_type,object_name;
SELECT name,type,line,position,text FROM user_errors ORDER BY name,sequence;
SELECT COUNT(*) AS tablas_dominio FROM user_tables WHERE table_name IN ('ROL','CATEGORIA','IDIOMA','PUEBLO_ORIGINARIO','USUARIO','ARCHIVO','COMUNIDAD','MIEMBRO_COMUNIDAD','SOLICITUD_INGRESO','PERIODO_ACADEMICO','CURSO','SECCION','UNIDAD','SESION','RECURSO','EVENTO','MATRICULA','EVALUACION','TAREA','ENTREGA_TAREA','NOTA','ASISTENCIA','PUBLICACION','PUBLICACION_ARCHIVO','COMENTARIO','NOTIFICACION','MENSAJE','AUDITORIA');
SELECT trigger_name,status FROM user_triggers ORDER BY trigger_name;
SELECT constraint_name,table_name,status FROM user_constraints WHERE status <> 'ENABLED';
SELECT codigo,nombre FROM rol;
-- Comparación del modelo real (exportar CLOB con SQL Developer):
SELECT DBMS_METADATA.GET_DDL('TABLE',table_name) FROM user_tables ORDER BY table_name;

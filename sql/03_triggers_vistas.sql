-- Revisar con DBA; instalar únicamente tras validar el diccionario de datos.
SET DEFINE OFF
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
 SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=:NEW.id_autor AND u.estado='ACTIVO' AND r.codigo IN('DOCENTE','ADMIN');
 IF n<>1 THEN RAISE_APPLICATION_ERROR(-20017,'Solo docente o administrador publica');END IF;
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

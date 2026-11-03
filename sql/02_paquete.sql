-- Implementación reconstruida del contrato del informe; revisar y compilar como COMUNIDADES.
-- No ejecuta COMMIT: la transacción pertenece al cliente.
SET DEFINE OFF
CREATE OR REPLACE PACKAGE pkg_comunidades AS
 FUNCTION registrar_login(p_correo VARCHAR2,p_exito BOOLEAN) RETURN VARCHAR2;
 PROCEDURE resolver_solicitud(p_id_solicitud NUMBER,p_id_revisor NUMBER,p_aprobar BOOLEAN,p_motivo VARCHAR2);
 PROCEDURE matricular(p_id_seccion NUMBER,p_id_usuario NUMBER);
 FUNCTION promedio(p_id_matricula NUMBER) RETURN NUMBER;
 PROCEDURE cerrar_seccion(p_id_seccion NUMBER);
 PROCEDURE marcar_notificaciones_leidas(p_id_usuario NUMBER);
END pkg_comunidades;
/
CREATE OR REPLACE PACKAGE BODY pkg_comunidades AS
 FUNCTION registrar_login(p_correo VARCHAR2,p_exito BOOLEAN) RETURN VARCHAR2 IS
  u usuario%ROWTYPE;
 BEGIN
  SELECT * INTO u FROM usuario WHERE correo=LOWER(TRIM(p_correo)) FOR UPDATE;
  IF u.estado<>'ACTIVO' OR u.bloqueado_hasta>SYSTIMESTAMP THEN RETURN 'BLOQUEADO'; END IF;
  IF p_exito THEN UPDATE usuario SET intentos_fallidos=0,bloqueado_hasta=NULL WHERE id_usuario=u.id_usuario; RETURN 'OK'; END IF;
  IF u.bloqueado_hasta IS NOT NULL THEN u.intentos_fallidos:=0; END IF;
  u.intentos_fallidos:=u.intentos_fallidos+1;
  UPDATE usuario SET intentos_fallidos=u.intentos_fallidos,bloqueado_hasta=CASE WHEN u.intentos_fallidos>=5 THEN SYSTIMESTAMP+INTERVAL '15' MINUTE ELSE NULL END WHERE id_usuario=u.id_usuario;
  IF u.intentos_fallidos>=5 THEN RETURN 'BLOQUEADO'; END IF;
  RETURN 'FALLIDO';
 EXCEPTION WHEN NO_DATA_FOUND THEN RETURN 'NO_EXISTE';
 END;
 PROCEDURE resolver_solicitud(p_id_solicitud NUMBER,p_id_revisor NUMBER,p_aprobar BOOLEAN,p_motivo VARCHAR2) IS
  s solicitud_ingreso%ROWTYPE; n NUMBER; v_estado VARCHAR2(10); cid NUMBER;
 BEGIN
  SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=p_id_revisor AND u.estado='ACTIVO' AND r.codigo='ADMIN';
  IF n<>1 THEN RAISE_APPLICATION_ERROR(-20023,'Revisor no autorizado'); END IF;
  SELECT * INTO s FROM solicitud_ingreso WHERE id_solicitud=p_id_solicitud FOR UPDATE;
  SELECT id_comunidad INTO cid FROM comunidad WHERE id_comunidad=s.id_comunidad AND estado='ACTIVA' FOR UPDATE;
  IF s.estado<>'PENDIENTE' THEN RAISE_APPLICATION_ERROR(-20020,'Solicitud ya resuelta'); END IF;
  IF p_aprobar THEN
   SELECT COUNT(*) INTO n FROM miembro_comunidad WHERE id_comunidad=s.id_comunidad AND id_usuario=s.id_usuario AND estado='EXPULSADO';
   IF n>0 THEN RAISE_APPLICATION_ERROR(-20023,'Usuario expulsado'); END IF;
   MERGE INTO miembro_comunidad m USING (SELECT s.id_comunidad cid,s.id_usuario uid FROM dual) x ON(m.id_comunidad=x.cid AND m.id_usuario=x.uid)
   WHEN MATCHED THEN UPDATE SET estado='ACTIVO',fecha_ingreso=SYSTIMESTAMP,fecha_salida=NULL
   WHEN NOT MATCHED THEN INSERT(id_comunidad,id_usuario,rol_comunidad,estado,fecha_ingreso) VALUES(x.cid,x.uid,'MIEMBRO','ACTIVO',SYSTIMESTAMP);
   v_estado:='APROBADA';
  ELSE
   IF TRIM(p_motivo) IS NULL THEN RAISE_APPLICATION_ERROR(-20024,'Falta motivo de rechazo'); END IF;
   v_estado:='RECHAZADA';
  END IF;
  UPDATE solicitud_ingreso SET estado=v_estado,id_revisor=p_id_revisor,fecha_respuesta=SYSTIMESTAMP,motivo_rechazo=CASE WHEN v_estado='RECHAZADA' THEN p_motivo ELSE NULL END WHERE id_solicitud=p_id_solicitud;
  INSERT INTO notificacion(id_destinatario,id_emisor,id_comunidad,tipo,titulo,entidad_tipo,entidad_id,leida,fecha_creacion)
  VALUES(s.id_usuario,p_id_revisor,s.id_comunidad,'SOLICITUD_RESPUESTA','Solicitud '||v_estado,'SOLICITUD',p_id_solicitud,0,SYSTIMESTAMP);
 END;
 PROCEDURE matricular(p_id_seccion NUMBER,p_id_usuario NUMBER) IS
  s seccion%ROWTYPE;n NUMBER;cid NUMBER;
 BEGIN
  SELECT * INTO s FROM seccion WHERE id_seccion=p_id_seccion FOR UPDATE;
  IF s.estado='CERRADA' THEN RAISE_APPLICATION_ERROR(-20012,'Sección cerrada'); END IF;
  SELECT COUNT(*) INTO n FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=p_id_usuario AND u.estado='ACTIVO' AND r.codigo='ESTUDIANTE';
  IF n<>1 THEN RAISE_APPLICATION_ERROR(-20011,'Solo estudiante activo'); END IF;
  SELECT c.id_comunidad INTO cid FROM curso c JOIN comunidad co ON co.id_comunidad=c.id_comunidad WHERE c.id_curso=s.id_curso AND c.estado='ACTIVO' AND co.estado='ACTIVA';
  SELECT COUNT(*) INTO n FROM miembro_comunidad WHERE id_comunidad=cid AND id_usuario=p_id_usuario AND estado='ACTIVO';
  IF n<>1 THEN RAISE_APPLICATION_ERROR(-20013,'No pertenece a la comunidad'); END IF;
  SELECT COUNT(*) INTO n FROM matricula WHERE id_seccion=p_id_seccion AND estado='MATRICULADO';
  IF s.cupo_maximo IS NOT NULL AND n>=s.cupo_maximo THEN RAISE_APPLICATION_ERROR(-20021,'Sin cupo'); END IF;
  INSERT INTO matricula(id_seccion,id_usuario,fecha_matricula,estado) VALUES(p_id_seccion,p_id_usuario,SYSTIMESTAMP,'MATRICULADO');
 END;
 FUNCTION promedio(p_id_matricula NUMBER) RETURN NUMBER IS n NUMBER;
 BEGIN
  SELECT NVL(SUM(n.calificacion*e.peso/100),0) INTO n FROM nota n JOIN evaluacion e ON e.id_evaluacion=n.id_evaluacion WHERE n.id_matricula=p_id_matricula;
  RETURN ROUND(n,2);
 END;
 PROCEDURE cerrar_seccion(p_id_seccion NUMBER) IS n NUMBER;v_estado VARCHAR2(10);p NUMBER;
 BEGIN
  SELECT estado INTO v_estado FROM seccion WHERE id_seccion=p_id_seccion FOR UPDATE;
  IF v_estado='CERRADA' THEN RAISE_APPLICATION_ERROR(-20012,'Sección ya cerrada'); END IF;
  SELECT NVL(SUM(peso),0) INTO n FROM evaluacion WHERE id_seccion=p_id_seccion;
  IF n<>100 THEN RAISE_APPLICATION_ERROR(-20022,'Pesos deben sumar 100'); END IF;
  FOR m IN(SELECT id_matricula FROM matricula WHERE id_seccion=p_id_seccion AND estado='MATRICULADO' FOR UPDATE) LOOP
   p:=promedio(m.id_matricula);
   UPDATE matricula SET nota_final=p,estado=CASE WHEN p>=10.5 THEN 'APROBADO' ELSE 'DESAPROBADO' END WHERE id_matricula=m.id_matricula;
  END LOOP;
  UPDATE seccion SET estado='CERRADA' WHERE id_seccion=p_id_seccion;
 END;
 PROCEDURE marcar_notificaciones_leidas(p_id_usuario NUMBER) IS
 BEGIN UPDATE notificacion SET leida=1,fecha_lectura=SYSTIMESTAMP WHERE id_destinatario=p_id_usuario AND leida=0; END;
END pkg_comunidades;
/
SHOW ERRORS

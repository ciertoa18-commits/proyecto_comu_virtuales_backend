/* =====================================================================
   PerúEduca – Comunidades Virtuales
   04 – Datos de prueba (recargados el 2026-11-02 con la regla de correo por rol)
   Requiere los catálogos de 01 (roles, idiomas, pueblos).
   Si ya existen los triggers de 02, se generan además notificaciones
   y registros de auditoría automáticos.
   ===================================================================== */
DECLARE
  r_est NUMBER; r_doc NUMBER; r_adm NUMBER; i_es NUMBER; i_cni NUMBER; i_qu NUMBER; p_ash NUMBER;
  c_edu NUMBER; c_cult NUMBER;
  u_adm NUMBER; u_d1 NUMBER; u_d2 NUMBER; u_e1 NUMBER; u_e2 NUMBER; u_e3 NUMBER; u_e4 NUMBER;
  u_tadm NUMBER; u_tdoc NUMBER; u_test NUMBER; sec2 NUMBER;
  com1 NUMBER; com2 NUMBER; per NUMBER; cur1 NUMBER; sec1 NUMBER; un1 NUMBER; un2 NUMBER;
  s1 NUMBER; s2 NUMBER; s3 NUMBER; arch1 NUMBER; rec1 NUMBER; rec_t NUMBER;
  ev1 NUMBER; ev2 NUMBER; ev3 NUMBER; m1 NUMBER; m2 NUMBER; m3 NUMBER; pub1 NUMBER; cm1 NUMBER;
BEGIN
  SELECT id_rol INTO r_est FROM rol WHERE codigo = 'ESTUDIANTE';
  SELECT id_rol INTO r_doc FROM rol WHERE codigo = 'DOCENTE';
  SELECT id_rol INTO r_adm FROM rol WHERE codigo = 'ADMIN';
  SELECT id_idioma INTO i_es  FROM idioma WHERE codigo = 'es';
  SELECT id_idioma INTO i_qu  FROM idioma WHERE codigo = 'qu';
  SELECT id_idioma INTO i_cni FROM idioma WHERE codigo = 'cni';
  SELECT id_pueblo INTO p_ash FROM pueblo_originario WHERE nombre = 'Asháninka';

  -- Categorías: se reutilizan si ya existen
  BEGIN SELECT id_categoria INTO c_edu FROM categoria WHERE nombre = 'Educación Intercultural Bilingüe';
  EXCEPTION WHEN NO_DATA_FOUND THEN INSERT INTO categoria (nombre,descripcion) VALUES ('Educación Intercultural Bilingüe','Comunidades EIB por lengua originaria') RETURNING id_categoria INTO c_edu; END;
  BEGIN SELECT id_categoria INTO c_cult FROM categoria WHERE nombre = 'Cultura y Tradiciones';
  EXCEPTION WHEN NO_DATA_FOUND THEN INSERT INTO categoria (nombre,descripcion) VALUES ('Cultura y Tradiciones','Saberes y expresión cultural') RETURNING id_categoria INTO c_cult; END;
  MERGE INTO categoria c USING (SELECT 'Ciencia y Tecnología' n FROM dual) x ON (c.nombre = x.n)
  WHEN NOT MATCHED THEN INSERT (nombre,descripcion) VALUES (x.n,'Áreas STEM');

  -- Usuarios. Contraseña de todos (solo desarrollo): Comunidades2026!  (hash BCrypt $2a$10$)
  -- Regla de correo: ESTUDIANTE -> @perueduca.pe ; DOCENTE y ADMIN -> @minedu.gob.pe
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,password_hash,id_idioma_pref) VALUES (r_adm,'admin@minedu.gob.pe','Rosa','Quispe Mamani','$2a$10$5rhfD3r4833w2/S6vlyHbO6C2472iygrl7TfmMalmf9LNWQzGoQL6',i_es) RETURNING id_usuario INTO u_adm;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,especialidad,password_hash,id_idioma_pref) VALUES (r_doc,'jcampos@minedu.gob.pe','Julio','Campos Shimpe','Matemática','$2a$10$qRTBX4WQvdPm6QkboSPq6eKby4rwu4ZIFcmn6WPuhjuUgowjf0RXa',i_cni) RETURNING id_usuario INTO u_d1;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,especialidad,password_hash,id_idioma_pref) VALUES (r_doc,'mhuaman@minedu.gob.pe','María','Huamán Ccori','Comunicación','$2a$10$8ZFqSvHxRDyhc8gRkFpeHem5ufJH/oAIES38Atvz/hrJ4hU0veUhm',i_qu) RETURNING id_usuario INTO u_d2;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,password_hash,id_idioma_pref) VALUES (r_est,'achumpate@perueduca.pe','Ana','Chumpate Ríos','$2a$10$tGlaArNj.7n8Y9fa7hjXPuKlvN46DkkBhHWJfWKizeG1Lv9VYuKka',i_cni) RETURNING id_usuario INTO u_e1;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,password_hash,id_idioma_pref) VALUES (r_est,'lsantos@perueduca.pe','Luis','Santos Antonio','$2a$10$fE59N8CRgAg85do2O3fu8eKQGo7tb5RD7sWWtIuhDhHAGp5zj0bba',i_cni) RETURNING id_usuario INTO u_e2;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,password_hash,id_idioma_pref) VALUES (r_est,'kmayta@perueduca.pe','Karen','Mayta Condori','$2a$10$boxj7TagR88ZDTNnsqDhCuYhK4gXY8qcCjTGrHz0Puopc5Z6vSER.',i_es) RETURNING id_usuario INTO u_e3;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,password_hash,id_idioma_pref) VALUES (r_est,'pnaquiri@perueduca.pe','Pedro','Naquiri Vásquez','$2a$10$hok/hJdtgkBbyxt2ZFcd1OFgizZU.aFmb74Z/1DczieCS/hTVWbTy',i_cni) RETURNING id_usuario INTO u_e4;

  -- Cuentas de prueba para el equipo
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,password_hash,id_idioma_pref) VALUES (r_adm,'admin.prueba@minedu.gob.pe','Admin','Prueba','$2a$10$5rhfD3r4833w2/S6vlyHbO6C2472iygrl7TfmMalmf9LNWQzGoQL6',i_es) RETURNING id_usuario INTO u_tadm;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,especialidad,password_hash,id_idioma_pref) VALUES (r_doc,'docente.prueba@minedu.gob.pe','Docente','Prueba','Matemática','$2a$10$5rhfD3r4833w2/S6vlyHbO6C2472iygrl7TfmMalmf9LNWQzGoQL6',i_es) RETURNING id_usuario INTO u_tdoc;
  INSERT INTO usuario (id_rol,correo,nombres,apellidos,password_hash,id_idioma_pref) VALUES (r_est,'alumno.prueba@perueduca.pe','Alumno','Prueba','$2a$10$5rhfD3r4833w2/S6vlyHbO6C2472iygrl7TfmMalmf9LNWQzGoQL6',i_es) RETURNING id_usuario INTO u_test;

  INSERT INTO archivo (id_subido_por,nombre_original,extension,tipo_mime,tamano_bytes,ruta_almacenamiento) VALUES (u_d1,'fracciones_ashaninka.pdf','pdf','application/pdf',1048576,'recursos/2026/fracciones_ashaninka.pdf') RETURNING id_archivo INTO arch1;

  -- Comunidades, miembros y solicitudes
  INSERT INTO comunidad (nombre,descripcion,id_categoria,id_idioma,id_pueblo,visibilidad,id_creador) VALUES ('Comunidad Asháninka – Río Tambo','Espacio de aprendizaje para estudiantes asháninkas',c_edu,i_cni,p_ash,'PUBLICA',u_adm) RETURNING id_comunidad INTO com1;
  INSERT INTO comunidad (nombre,descripcion,id_categoria,id_idioma,visibilidad,moderacion_previa,id_creador) VALUES ('Saberes Andinos','Comunidad oficial de cultura quechua',c_cult,i_qu,'PRIVADA',1,u_adm) RETURNING id_comunidad INTO com2;
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario,rol_comunidad) VALUES (com1,u_d1,'MODERADOR');
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario) VALUES (com1,u_e1);
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario) VALUES (com1,u_e2);
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario) VALUES (com1,u_e4);
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario) VALUES (com1,u_tdoc);
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario) VALUES (com1,u_test);
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario,rol_comunidad) VALUES (com2,u_d2,'MODERADOR');
  INSERT INTO solicitud_ingreso (id_comunidad,id_usuario,mensaje) VALUES (com2,u_e3,'Quiero aprender quechua');
  INSERT INTO solicitud_ingreso (id_comunidad,id_usuario,estado,id_revisor,fecha_respuesta) VALUES (com2,u_e1,'APROBADA',u_adm,SYSTIMESTAMP);
  INSERT INTO miembro_comunidad (id_comunidad,id_usuario) VALUES (com2,u_e1);

  -- Estructura académica
  BEGIN SELECT id_periodo INTO per FROM periodo_academico WHERE nombre = '2026-II';
  EXCEPTION WHEN NO_DATA_FOUND THEN INSERT INTO periodo_academico (nombre,fecha_inicio,fecha_fin,estado) VALUES ('2026-II',DATE '2026-08-17',DATE '2026-12-18','ABIERTO') RETURNING id_periodo INTO per; END;
  INSERT INTO curso (id_comunidad,codigo,nombre,descripcion) VALUES (com1,'MAT-01','Matemática 1','Números y fracciones con ejemplos del entorno') RETURNING id_curso INTO cur1;
  INSERT INTO seccion (id_curso,id_periodo,id_docente,grupo,cupo_maximo,sala_virtual,estado) VALUES (cur1,per,u_d1,'A',30,'https://meet.perueduca.pe/mat01-a','EN_CURSO') RETURNING id_seccion INTO sec1;
  INSERT INTO seccion (id_curso,id_periodo,id_docente,grupo,cupo_maximo,estado) VALUES (cur1,per,u_tdoc,'B',30,'EN_CURSO') RETURNING id_seccion INTO sec2;
  INSERT INTO matricula (id_seccion,id_usuario) VALUES (sec2,u_test);
  INSERT INTO unidad (id_seccion,nombre,orden) VALUES (sec1,'Unidad 1: Números naturales',1) RETURNING id_unidad INTO un1;
  INSERT INTO unidad (id_seccion,nombre,orden) VALUES (sec1,'Unidad 2: Fracciones',2) RETURNING id_unidad INTO un2;
  INSERT INTO sesion (id_unidad,titulo,orden,fecha_inicio,duracion_min) VALUES (un1,'Contamos en asháninka y en español',1,TIMESTAMP '2026-09-01 09:00:00',60) RETURNING id_sesion INTO s1;
  INSERT INTO sesion (id_unidad,titulo,orden,fecha_inicio,duracion_min) VALUES (un1,'Sumas y restas',2,TIMESTAMP '2026-09-08 09:00:00',60) RETURNING id_sesion INTO s2;
  INSERT INTO sesion (id_unidad,titulo,orden,fecha_inicio,duracion_min) VALUES (un2,'¿Qué es una fracción?',1,TIMESTAMP '2026-10-06 09:00:00',60) RETURNING id_sesion INTO s3;
  INSERT INTO recurso (id_unidad,id_sesion,id_autor,titulo,tipo,id_idioma,id_archivo) VALUES (un2,s3,u_d1,'Guía de fracciones (bilingüe)','DOCUMENTO',i_cni,arch1) RETURNING id_recurso INTO rec1;
  INSERT INTO recurso (id_unidad,id_sesion,id_autor,titulo,descripcion,tipo,id_idioma) VALUES (un2,s3,u_d1,'Tarea 1: fracciones en la chacra','Dibuja y reparte una cosecha en partes iguales','TAREA',i_es) RETURNING id_recurso INTO rec_t;

  -- Evaluaciones (suman 100 %), matrícula, notas, entregas y asistencia
  INSERT INTO evaluacion (id_seccion,id_unidad,nombre,tipo,peso) VALUES (sec1,un1,'Práctica 1','PRACTICA',30) RETURNING id_evaluacion INTO ev1;
  INSERT INTO evaluacion (id_seccion,id_unidad,nombre,tipo,peso) VALUES (sec1,un2,'Tarea 1','TAREA',30) RETURNING id_evaluacion INTO ev2;
  INSERT INTO evaluacion (id_seccion,nombre,tipo,peso) VALUES (sec1,'Examen final','EXAMEN',40) RETURNING id_evaluacion INTO ev3;
  INSERT INTO tarea (id_recurso,fecha_entrega,id_evaluacion) VALUES (rec_t,TIMESTAMP '2026-10-13 23:59:00',ev2);
  INSERT INTO matricula (id_seccion,id_usuario) VALUES (sec1,u_e1) RETURNING id_matricula INTO m1;
  INSERT INTO matricula (id_seccion,id_usuario) VALUES (sec1,u_e2) RETURNING id_matricula INTO m2;
  INSERT INTO matricula (id_seccion,id_usuario) VALUES (sec1,u_e4) RETURNING id_matricula INTO m3;
  INSERT INTO matricula (id_seccion,id_usuario) VALUES (sec1,u_test);
  INSERT INTO nota (id_matricula,id_evaluacion,calificacion,id_registrado_por) VALUES (m1,ev1,17,u_d1);
  INSERT INTO nota (id_matricula,id_evaluacion,calificacion,id_registrado_por) VALUES (m1,ev2,15,u_d1);
  INSERT INTO nota (id_matricula,id_evaluacion,calificacion,id_registrado_por) VALUES (m1,ev3,16,u_d1);
  INSERT INTO nota (id_matricula,id_evaluacion,calificacion,id_registrado_por) VALUES (m2,ev1,9,u_d1);
  INSERT INTO nota (id_matricula,id_evaluacion,calificacion,id_registrado_por) VALUES (m2,ev2,11,u_d1);
  INSERT INTO nota (id_matricula,id_evaluacion,calificacion,id_registrado_por) VALUES (m2,ev3,10,u_d1);
  INSERT INTO nota (id_matricula,id_evaluacion,calificacion,id_registrado_por) VALUES (m3,ev1,14,u_d1);
  INSERT INTO entrega_tarea (id_recurso,id_matricula,comentario,fecha_entrega) VALUES (rec_t,m1,'Repartí 12 yucas en 4 partes: 3/12 = 1/4',TIMESTAMP '2026-10-12 20:15:00');
  INSERT INTO asistencia (id_sesion,id_matricula,estado,id_registrado_por) VALUES (s1,m1,'PRESENTE',u_d1);
  INSERT INTO asistencia (id_sesion,id_matricula,estado,id_registrado_por) VALUES (s2,m1,'PRESENTE',u_d1);
  INSERT INTO asistencia (id_sesion,id_matricula,estado,id_registrado_por) VALUES (s3,m1,'TARDANZA',u_d1);
  INSERT INTO asistencia (id_sesion,id_matricula,estado,id_registrado_por) VALUES (s1,m2,'FALTA',u_d1);
  INSERT INTO asistencia (id_sesion,id_matricula,estado,id_registrado_por) VALUES (s2,m2,'FALTA',u_d1);
  INSERT INTO asistencia (id_sesion,id_matricula,estado,id_registrado_por) VALUES (s3,m2,'PRESENTE',u_d1);
  INSERT INTO asistencia (id_sesion,id_matricula,estado,observacion,id_registrado_por) VALUES (s1,m3,'JUSTIFICADA','Crecida del río',u_d1);

  -- Contenido y comunicación
  INSERT INTO evento (id_comunidad,id_seccion,id_creador,titulo,fecha_inicio,fecha_fin) VALUES (com1,sec1,u_d1,'Feria de matemática en la comunidad',TIMESTAMP '2026-11-20 10:00:00',TIMESTAMP '2026-11-20 13:00:00');
  INSERT INTO publicacion (id_comunidad,id_seccion,id_autor,titulo,contenido) VALUES (com1,sec1,u_d1,'Bienvenidos a la Unidad 2','Esta semana empezamos fracciones. Revisen la guía bilingüe.') RETURNING id_publicacion INTO pub1;
  INSERT INTO publicacion_archivo (id_publicacion,id_archivo) VALUES (pub1,arch1);
  INSERT INTO comentario (id_publicacion,id_autor,contenido) VALUES (pub1,u_e1,'Profesor, ¿la tarea se entrega en papel o aquí?') RETURNING id_comentario INTO cm1;
  INSERT INTO comentario (id_publicacion,id_comentario_padre,id_autor,contenido) VALUES (pub1,cm1,u_d1,'Aquí en la plataforma, Ana.');
  INSERT INTO comentario (id_recurso,id_autor,contenido) VALUES (rec1,u_e2,'La guía está muy clara, gracias.');
  INSERT INTO notificacion (id_destinatario,id_emisor,id_comunidad,tipo,titulo,entidad_tipo,entidad_id) VALUES (u_e1,u_d1,com1,'NUEVA_PUBLICACION','Nueva publicación: Bienvenidos a la Unidad 2','PUBLICACION',pub1);
  INSERT INTO notificacion (id_destinatario,id_emisor,id_comunidad,tipo,titulo,entidad_tipo,entidad_id) VALUES (u_e2,u_d1,com1,'NUEVA_PUBLICACION','Nueva publicación: Bienvenidos a la Unidad 2','PUBLICACION',pub1);
  INSERT INTO notificacion (id_destinatario,id_comunidad,tipo,titulo,entidad_tipo) VALUES (u_adm,com2,'SOLICITUD_INGRESO','Nueva solicitud de ingreso a Saberes Andinos','SOLICITUD');
  INSERT INTO mensaje (id_emisor,id_receptor,contenido) VALUES (u_e2,u_d1,'Profesor, falté por enfermedad, ¿puedo recuperar la práctica?');
  COMMIT;
END;
/

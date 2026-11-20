DECLARE
  v_com NUMBER;
  v_per NUMBER;
  v_doc NUMBER;
  v_idi NUMBER;
  TYPE t_ids IS TABLE OF NUMBER;
  v_alumnos t_ids := t_ids();

  FUNCTION id_de(p_correo VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id_usuario INTO v FROM usuario WHERE correo = p_correo;
    RETURN v;
  EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
  END;

  PROCEDURE agregar_miembro(p_usuario NUMBER) IS
  BEGIN
    MERGE INTO miembro_comunidad m
    USING (SELECT v_com c, p_usuario u FROM dual) x ON (m.id_comunidad = x.c AND m.id_usuario = x.u)
    WHEN NOT MATCHED THEN INSERT (id_comunidad, id_usuario) VALUES (x.c, x.u);
  END;

  PROCEDURE crear_curso(
    p_codigo VARCHAR2, p_nombre VARCHAR2, p_desc VARCHAR2,
    p_u1 VARCHAR2, p_u2 VARCHAR2, p_u3 VARCHAR2,
    p_sumilla VARCHAR2, p_competencias VARCHAR2, p_contenidos VARCHAR2,
    p_metodologia VARCHAR2, p_evaluacion VARCHAR2, p_bibliografia VARCHAR2,
    p_base NUMBER) IS
    v_cur NUMBER; v_sec NUMBER; v_u NUMBER; v_s NUMBER; v_r NUMBER; v_mat NUMBER;
    TYPE t_txt IS TABLE OF VARCHAR2(150);
    v_unidades t_txt := t_txt(p_u1, p_u2, p_u3);
    v_eval t_ids := t_ids();
    v_ses t_ids := t_ids();
    v_nota NUMBER;
    v_est VARCHAR2(12);
  BEGIN
    SELECT COUNT(*) INTO v_cur FROM curso WHERE id_comunidad = v_com AND codigo = p_codigo;
    IF v_cur > 0 THEN RETURN; END IF;

    INSERT INTO curso (id_comunidad, codigo, nombre, descripcion)
    VALUES (v_com, p_codigo, p_nombre, p_desc) RETURNING id_curso INTO v_cur;
    INSERT INTO seccion (id_curso, id_periodo, id_docente, grupo, cupo_maximo, sala_virtual, estado)
    VALUES (v_cur, v_per, v_doc, 'A', 30, 'https://meet.google.com/abc-defg-hij', 'EN_CURSO') RETURNING id_seccion INTO v_sec;

    FOR i IN 1 .. 3 LOOP
      INSERT INTO unidad (id_seccion, nombre, descripcion, orden)
      VALUES (v_sec, v_unidades(i), 'Unidad ' || i || ' de ' || p_nombre, i) RETURNING id_unidad INTO v_u;
      FOR j IN 1 .. 2 LOOP
        INSERT INTO sesion (id_unidad, titulo, orden, fecha_inicio, duracion_min, enlace_clase)
        VALUES (v_u, 'Semana ' || ((i - 1) * 2 + j) || ': ' || v_unidades(i), j,
                TIMESTAMP '2026-09-01 08:00:00' + NUMTODSINTERVAL(((i - 1) * 2 + j - 1) * 7 + p_base, 'DAY'), 90,
                'https://meet.google.com/abc-defg-hij')
        RETURNING id_sesion INTO v_s;
        v_ses.EXTEND; v_ses(v_ses.COUNT) := v_s;
      END LOOP;
      INSERT INTO recurso (id_unidad, id_autor, titulo, descripcion, tipo, id_idioma, url_externa)
      VALUES (v_u, v_doc, 'Lectura: ' || v_unidades(i), 'Material de lectura de la unidad ' || i, 'ENLACE', v_idi,
              'https://www.perueduca.pe/recursos-educativos');
      INSERT INTO recurso (id_unidad, id_autor, titulo, descripcion, tipo, id_idioma, url_externa)
      VALUES (v_u, v_doc, 'Video de apoyo: ' || v_unidades(i), 'Repaso en video de la unidad ' || i, 'ENLACE', v_idi,
              'https://www.youtube.com/@perueduca');
      INSERT INTO evaluacion (id_seccion, id_unidad, nombre, tipo, peso, fecha)
      VALUES (v_sec, v_u, CASE i WHEN 1 THEN 'Práctica 1' WHEN 2 THEN 'Tarea 1' ELSE 'Examen final' END,
              CASE i WHEN 1 THEN 'PRACTICA' WHEN 2 THEN 'TAREA' ELSE 'EXAMEN' END,
              CASE i WHEN 1 THEN 30 WHEN 2 THEN 30 ELSE 40 END,
              TIMESTAMP '2026-09-14 08:00:00' + NUMTODSINTERVAL((i - 1) * 14 + p_base, 'DAY'))
      RETURNING id_evaluacion INTO v_s;
      v_eval.EXTEND; v_eval(v_eval.COUNT) := v_s;
      IF i = 2 THEN
        INSERT INTO recurso (id_unidad, id_autor, titulo, descripcion, tipo, id_idioma)
        VALUES (v_u, v_doc, 'Tarea 1: ' || v_unidades(i), 'Entrega un informe de una página sobre lo aprendido.', 'TAREA', v_idi)
        RETURNING id_recurso INTO v_r;
        INSERT INTO tarea (id_recurso, fecha_entrega, permite_tardias, id_evaluacion)
        VALUES (v_r, TIMESTAMP '2026-12-10 23:59:00', 1, v_s);
      END IF;
    END LOOP;

    INSERT INTO silabo (id_seccion, sumilla, competencias, contenidos, metodologia, evaluacion, bibliografia, id_autor)
    VALUES (v_sec, p_sumilla, p_competencias, p_contenidos, p_metodologia, p_evaluacion, p_bibliografia, v_doc);

    FOR k IN 1 .. v_alumnos.COUNT LOOP
      INSERT INTO matricula (id_seccion, id_usuario) VALUES (v_sec, v_alumnos(k)) RETURNING id_matricula INTO v_mat;
      FOR e IN 1 .. v_eval.COUNT - 1 LOOP
        v_nota := LEAST(20, GREATEST(8, 11 + MOD(k * 3 + e * 2 + p_base, 9)));
        INSERT INTO nota (id_matricula, id_evaluacion, calificacion, comentario_docente, id_registrado_por)
        VALUES (v_mat, v_eval(e), v_nota, CASE WHEN v_nota >= 15 THEN 'Muy buen trabajo' WHEN v_nota < 11 THEN 'Debe reforzar' END, v_doc);
      END LOOP;
      FOR s IN 1 .. 4 LOOP
        v_est := CASE MOD(k + s + p_base, 7) WHEN 0 THEN 'FALTA' WHEN 3 THEN 'TARDANZA' WHEN 5 THEN 'JUSTIFICADA' ELSE 'PRESENTE' END;
        INSERT INTO asistencia (id_sesion, id_matricula, estado, observacion, id_registrado_por)
        VALUES (v_ses(s), v_mat, v_est, CASE v_est WHEN 'JUSTIFICADA' THEN 'Cita médica' END, v_doc);
      END LOOP;
    END LOOP;
  END;
BEGIN
  SELECT id_comunidad INTO v_com FROM comunidad WHERE nombre LIKE 'Comunidad Ash%' AND ROWNUM = 1;
  SELECT id_periodo INTO v_per FROM periodo_academico WHERE estado = 'ABIERTO' AND ROWNUM = 1;
  v_doc := id_de('docente.prueba@minedu.gob.pe');
  SELECT MIN(id_idioma) INTO v_idi FROM idioma WHERE codigo = 'es';
  agregar_miembro(v_doc);
  FOR r IN (SELECT column_value c FROM TABLE(sys.odcivarchar2list(
              'alumno.prueba@perueduca.pe', 'ciertoa18@perueduca.pe', 'achumpate@perueduca.pe',
              'lsantos@perueduca.pe', 'kmayta@perueduca.pe'))) LOOP
    IF id_de(r.c) IS NOT NULL THEN
      v_alumnos.EXTEND; v_alumnos(v_alumnos.COUNT) := id_de(r.c);
      agregar_miembro(id_de(r.c));
    END IF;
  END LOOP;

  crear_curso('COM101', 'Comunicación Intercultural',
        'Desarrolla la comprensión y producción de textos en castellano y asháninka.',
        'Lectura y comprensión de textos', 'Producción de textos', 'Expresión oral intercultural',
        'Curso que fortalece la comunicación oral y escrita valorando la lengua y cultura asháninka.',
        'Comprende textos escritos. Produce textos con coherencia. Se comunica oralmente en contextos interculturales.',
        'Semana 1-2: estrategias de lectura' || CHR(10) || 'Semana 3-4: el párrafo y la redacción' || CHR(10) || 'Semana 5-6: narración oral y relatos tradicionales',
        'Clases virtuales con lecturas guiadas, trabajo en parejas y presentaciones orales.',
        'Práctica 1 (30%) + Tarea 1 (30%) + Examen final (40%). Nota mínima aprobatoria: 11.',
        'Ministerio de Educación (2023). Guía de lectura intercultural. Lima: MINEDU.',
        0);

  crear_curso('CYA101', 'Ciencia y Ambiente',
        'Explora los ecosistemas de la Amazonía y el cuidado del ambiente.',
        'Los seres vivos y su entorno', 'El agua y los recursos naturales', 'Cuidado del ambiente en la comunidad',
        'Curso que estudia los ecosistemas amazónicos y promueve prácticas de cuidado ambiental.',
        'Explica el mundo natural. Indaga con métodos científicos. Propone acciones de cuidado ambiental.',
        'Semana 1-2: ecosistemas del bosque' || CHR(10) || 'Semana 3-4: ciclo del agua' || CHR(10) || 'Semana 5-6: proyecto ambiental comunitario',
        'Indagación guiada, observación del entorno y proyecto grupal.',
        'Práctica 1 (30%) + Tarea 1 (30%) + Examen final (40%). Nota mínima aprobatoria: 11.',
        'MINEDU (2022). Ciencia y tecnología para la Amazonía. Lima: MINEDU.',
        2);

  FOR s IN (SELECT id_seccion, id_docente FROM seccion se
            WHERE NOT EXISTS (SELECT 1 FROM silabo x WHERE x.id_seccion = se.id_seccion)) LOOP
    INSERT INTO silabo (id_seccion, sumilla, competencias, contenidos, metodologia, evaluacion, bibliografia, id_autor)
    VALUES (s.id_seccion,
            'Curso de matemática básica orientado a resolver problemas de la vida diaria.',
            'Resuelve problemas de cantidad. Resuelve problemas de regularidad y cambio.',
            'Unidad 1: números naturales y fracciones' || CHR(10) || 'Unidad 2: proporcionalidad' || CHR(10) || 'Unidad 3: estadística básica',
            'Resolución de problemas en contexto y trabajo colaborativo.',
            'Prácticas (40%) + Tareas (20%) + Examen (40%).',
            'MINEDU (2023). Matemática 1. Cuaderno de trabajo. Lima: MINEDU.',
            s.id_docente);
  END LOOP;
  COMMIT;
END;

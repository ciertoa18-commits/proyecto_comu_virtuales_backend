/* =====================================================================
   PerúEduca – Comunidades Virtuales
   09 – Clonar el proyecto en un esquema de DESARROLLO: COMU_VIR_DESA
   ---------------------------------------------------------------------
   Origen : COMUNIDADES      (esquema del proyecto)
   Destino: COMU_VIR_DESA    (lo crea Oracle en la importación)
   Nota   : Oracle no acepta guiones (-) en nombres de usuario sin
            comillas, por eso "comu_vir-desa" se escribe COMU_VIR_DESA.

   Ejecutar como SYSTEM (u otro usuario con rol DBA).
   La contraseña NO se escribe en el comando: Oracle la pide al ejecutar.
   ===================================================================== */


-- ---------------------------------------------------------------------
-- 0. (Opcional) Revisar el directorio de Data Pump que se va a usar
-- ---------------------------------------------------------------------
SELECT directory_name, directory_path
FROM   dba_directories
WHERE  directory_name = 'DATA_PUMP_DIR';


-- =====================================================================
-- OPCIÓN A – Igual que el ejemplo de clase (línea de comandos)
-- Se ejecuta en la terminal del servidor (EC2), no en SQL Developer.
-- =====================================================================

-- 1. Exportar el esquema del proyecto
-- expdp system schemas=comunidades directory=DATA_PUMP_DIR dumpfile=EXP_COMU_VIR.dmp logfile=exp_comu_vir.log

-- 2. Importar en un usuario que NO existe (Oracle crea COMU_VIR_DESA)
-- impdp system dumpfile=EXP_COMU_VIR.dmp remap_schema=comunidades:comu_vir_desa directory=DATA_PUMP_DIR logfile=imp_comu_vir_desa.log

--    Desde otra PC con cliente Oracle, se agrega la conexión al usuario:
-- expdp system@//<IP_AWS>:1521/orcl schemas=comunidades directory=DATA_PUMP_DIR dumpfile=EXP_COMU_VIR.dmp logfile=exp_comu_vir.log
-- impdp system@//<IP_AWS>:1521/orcl dumpfile=EXP_COMU_VIR.dmp remap_schema=comunidades:comu_vir_desa directory=DATA_PUMP_DIR logfile=imp_comu_vir_desa.log


-- =====================================================================
-- OPCIÓN B – Lo mismo desde SQL Developer (conectado como SYSTEM)
-- Usa el paquete DBMS_DATAPUMP, que es lo que expdp/impdp hacen por dentro.
-- Activar la salida: Ver > Salida de DBMS (o SET SERVEROUTPUT ON).
-- =====================================================================
SET SERVEROUTPUT ON

-- 1. Exportar COMUNIDADES -> EXP_COMU_VIR.dmp
DECLARE
  h      NUMBER;
  estado VARCHAR2(30);
BEGIN
  h := DBMS_DATAPUMP.OPEN(operation => 'EXPORT', job_mode => 'SCHEMA', job_name => 'EXP_COMU_VIR');
  DBMS_DATAPUMP.ADD_FILE(h, 'EXP_COMU_VIR.dmp', 'DATA_PUMP_DIR', reusefile => 1);
  DBMS_DATAPUMP.ADD_FILE(h, 'exp_comu_vir.log', 'DATA_PUMP_DIR',
                         filetype => DBMS_DATAPUMP.KU$_FILE_TYPE_LOG_FILE, reusefile => 1);
  DBMS_DATAPUMP.METADATA_FILTER(h, 'SCHEMA_EXPR', 'IN (''COMUNIDADES'')');
  DBMS_DATAPUMP.START_JOB(h);
  DBMS_DATAPUMP.WAIT_FOR_JOB(h, estado);
  DBMS_OUTPUT.PUT_LINE('Exportación: ' || estado);
END;
/

-- 2. Importar en COMU_VIR_DESA (remap_schema=comunidades:comu_vir_desa)
DECLARE
  h      NUMBER;
  estado VARCHAR2(30);
BEGIN
  h := DBMS_DATAPUMP.OPEN(operation => 'IMPORT', job_mode => 'SCHEMA', job_name => 'IMP_COMU_VIR_DESA');
  DBMS_DATAPUMP.ADD_FILE(h, 'EXP_COMU_VIR.dmp', 'DATA_PUMP_DIR');
  DBMS_DATAPUMP.ADD_FILE(h, 'imp_comu_vir_desa.log', 'DATA_PUMP_DIR',
                         filetype => DBMS_DATAPUMP.KU$_FILE_TYPE_LOG_FILE, reusefile => 1);
  DBMS_DATAPUMP.METADATA_REMAP(h, 'REMAP_SCHEMA', 'COMUNIDADES', 'COMU_VIR_DESA');
  DBMS_DATAPUMP.START_JOB(h);
  DBMS_DATAPUMP.WAIT_FOR_JOB(h, estado);
  DBMS_OUTPUT.PUT_LINE('Importación: ' || estado);
END;
/


-- ---------------------------------------------------------------------
-- 3. Dar al nuevo usuario su propia contraseña
--    (al importar copia la de COMUNIDADES). Escribe tú la contraseña:
-- ---------------------------------------------------------------------
-- ALTER USER comu_vir_desa IDENTIFIED BY "<nueva_contraseña>";


-- ---------------------------------------------------------------------
-- 4. Verificar que el clon quedó igual al original
-- ---------------------------------------------------------------------
-- 4.1 Objetos por tipo (deben coincidir las dos columnas)
SELECT object_type,
       SUM(CASE owner WHEN 'COMUNIDADES'   THEN 1 ELSE 0 END) AS original,
       SUM(CASE owner WHEN 'COMU_VIR_DESA' THEN 1 ELSE 0 END) AS clon_desa
FROM   dba_objects
WHERE  owner IN ('COMUNIDADES', 'COMU_VIR_DESA')
GROUP  BY object_type
ORDER  BY object_type;

-- 4.2 Objetos inválidos en el clon (debe salir vacío)
SELECT object_name, object_type
FROM   dba_objects
WHERE  owner = 'COMU_VIR_DESA' AND status <> 'VALID';

-- 4.3 Mismos datos en las tablas principales
SELECT 'usuario' tabla, (SELECT COUNT(*) FROM comunidades.usuario) original, (SELECT COUNT(*) FROM comu_vir_desa.usuario) clon FROM dual UNION ALL
SELECT 'comunidad',     (SELECT COUNT(*) FROM comunidades.comunidad),         (SELECT COUNT(*) FROM comu_vir_desa.comunidad)         FROM dual UNION ALL
SELECT 'seccion',       (SELECT COUNT(*) FROM comunidades.seccion),           (SELECT COUNT(*) FROM comu_vir_desa.seccion)           FROM dual UNION ALL
SELECT 'matricula',     (SELECT COUNT(*) FROM comunidades.matricula),         (SELECT COUNT(*) FROM comu_vir_desa.matricula)         FROM dual UNION ALL
SELECT 'nota',          (SELECT COUNT(*) FROM comunidades.nota),              (SELECT COUNT(*) FROM comu_vir_desa.nota)              FROM dual;


-- ---------------------------------------------------------------------
-- (Para repetir la práctica desde cero) Borrar el clon:
-- DROP USER comu_vir_desa CASCADE;
-- ---------------------------------------------------------------------

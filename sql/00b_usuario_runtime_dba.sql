-- SQL*Plus/SQLcl, ejecutar como DBA una sola vez.
-- Elegir una contraseña sin comillas dobles ni ampersand para esta sustitución SQL*Plus.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET VERIFY OFF
ACCEPT runtime_password CHAR PROMPT 'Contraseña de APP_COMUNIDADES: ' HIDE
CREATE USER APP_COMUNIDADES IDENTIFIED BY "&runtime_password";
UNDEFINE runtime_password
GRANT CREATE SESSION TO APP_COMUNIDADES;
CREATE ROLE R_COMUNIDADES_APP;
GRANT R_COMUNIDADES_APP TO APP_COMUNIDADES;

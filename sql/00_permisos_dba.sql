-- Ejecutar por DBA; no contiene contraseñas ni cambia las tablas.
-- El propietario COMUNIDADES ya existe según el informe.
GRANT CREATE TRIGGER, CREATE PROCEDURE, CREATE VIEW TO COMUNIDADES;
-- Usuario de runtime: crearlo con contraseña gestionada externamente.
-- Ejemplo SQL*Plus (entrada oculta; usar contraseña sin comillas internas):
-- ACCEPT runtime_password CHAR PROMPT 'Clave de APP_COMUNIDADES: ' HIDE
-- CREATE USER APP_COMUNIDADES IDENTIFIED BY "&runtime_password";
-- UNDEFINE runtime_password
-- GRANT CREATE SESSION TO APP_COMUNIDADES;
-- El siguiente script genera permisos sobre objetos una vez creado ese usuario.

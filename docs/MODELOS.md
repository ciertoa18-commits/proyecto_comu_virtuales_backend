# Modelos del proyecto Comunidades Virtuales

Este documento organiza la entrega de backend y sus modelos para la iteración 1 del 03/11/2026. La plataforma facilita a estudiantes de comunidades nativas el acceso a contenido educativo y la interacción con docentes. Se utiliza el modelo Oracle de 28 tablas descrito en el informe actualizado; el proyecto monolítico anterior sirve como referencia funcional, no como esquema vigente.

## Modelo de negocio

El modelo de negocio explica quién participa, qué problema atiende la organización y cómo se trabaja, antes de describir pantallas o código. MINEDU/PerúEduca ofrece espacios de aprendizaje organizados por categoría, idioma y pueblo originario. El administrador organiza comunidades y resuelve admisiones privadas; el docente comparte contenido; el estudiante participa y aprende.

Procesos: identificación institucional → selección de comunidad → admisión pública o revisión privada → publicación y moderación → interacción mediante comentarios → avisos. La gestión académica añade curso → sección por periodo y grupo → unidad → sesión → recurso. Matrícula, notas y asistencia pertenecen a la sección, evitando mezclar periodos.

Durante la iteración se deben corroborar con el responsable del negocio: quién es moderador de cada comunidad, política ante expulsiones, catálogo de palabras no permitidas, forma institucional de autenticar, necesidad real de chat grupal y si se permiten comentarios en recursos. Son preguntas pendientes de validación, no descubrimientos confirmados. El brief excluye videollamadas propias, app móvil e integraciones gubernamentales; solo se comparten enlaces de clase.

## Modelo de requerimientos

Este modelo define lo que el sistema debe permitir y sus restricciones: RF01–RF55, RN01–RN26 y RES del Excel. Cada CU agrupa varios RF y se vincula a actores, reglas, diagramas y endpoints. La matriz de trazabilidad identifica esas relaciones. Un cambio descubierto durante una iteración se registra, se evalúa y se aprueba antes de reemplazar la línea base.

Hay dos catálogos incompatibles en la plantilla: CU01–CU13 en requerimientos y CUS01–CUS12 en priorización/trazabilidad. Se adopta CU01–CU13 para esta entrega y se documenta la diferencia. El ejemplo de 30 CUS con incrementos 7+10+13 es metodológico; no se inventan 17 CU adicionales para completarlo.

Propuestas de RF adicionales, pendientes de aprobación: RF-EXT01 matrícula y retiro; RF-EXT02 evaluaciones y notas; RF-EXT03 asistencia; RF-EXT04 entregas de tareas; RF-EXT05 mensajería directa. Criterios: autorización por titular/matrícula, notas de 0 a 20, cierre con pesos al 100 %, cupos controlados concurrentemente y plazos de tarea respetados.

## Iteración 1

Objetivo propuesto: entregar un backend verificable que permita autenticarse, mantener el perfil, encontrar comunidades, ingresar por el flujo correspondiente, administrar comunidades y miembros y compartir publicaciones con comentarios y archivos sujetos a permisos y moderación.

Selección propuesta: CU01–CU07, 7 CU. Incremento acumulado: 0+7=7. La implementación incluye controles transversales de CU13 y notificaciones de soporte de CU12 por ser dependencias necesarias; esto no significa que se hayan aceptado todos los CU posteriores. El carácter iterativo consiste en recorrer negocio, requerimientos, análisis, diseño, implementación y verificación una vez. La siguiente iteración revisa estos modelos y agrega CU08–CU13: 7+6=13. Una tercera iteración no se inventa: se planifica cuando se apruebe nuevo alcance.

Criterios de aceptación: login válido devuelve JWT; cinco errores bloquean 15 minutos; datos personales solo los cambia el propietario; búsquedas omiten comunidades inactivas; unirse no permite saltar admisión privada; resolver solicitud deja revisor y fecha; solo autor edita publicación; contenido pendiente no es visible en el muro; respuesta pertenece a la misma publicación; archivo se valida por extensión, tamaño y contenido; logout invalida el token. Ejecutar pruebas automatizadas y una prueba final con Oracle antes de aceptar entrega.

## Análisis y diseño

Se entregan 13 diagramas de clases de análisis, 13 de diseño y 13 secuencias en PlantUML, junto a los dos diagramas generales y los modelos conceptual y relacional. El archivo Draw.io permite editar los diagramas de clases y BD. Las fronteras del análisis representan al cliente que consumirá JSON; no se implementa frontend. El diseño distingue controller, DTO, servicio, repositorio y entidad; seguridad comprueba rol global, pertenencia, titularidad y autoría.

Los diagramas se reconstruyeron a partir del Excel y el informe actual. No equivalen a una revisión visual completa de todos los diagramas originales alojados en Drive: esos enlaces no fueron accesibles. El esquema Draw.io anterior referencia tablas T_PERSONA y otras ya reemplazadas.

## Base de datos

El modelo conceptual extrae solo las entidades del dominio. El relacional representa las 28 tablas existentes. Las relaciones N:M se resuelven con MIEMBRO_COMUNIDAD (usuario/comunidad), MATRICULA (usuario/sección) y PUBLICACION_ARCHIVO (publicación/archivo). NOTA, ASISTENCIA y ENTREGA_TAREA agregan atributos a relaciones académicas. TAREA comparte PK con RECURSO; PUBLICACION_ARCHIVO tiene PK compuesta.

Los scripts de esta entrega se separan entre instalación en esquema vacío, permisos de ejecución, paquete, triggers y verificación. No se ejecutan DDL sobre la base remota. Hibernate se configura con validate; no crea ni actualiza tablas. El DDL se reconstruye del diccionario recibido y debe compararse con DBMS_METADATA antes de usarlo como respaldo oficial. Los nombres y el número de índices originales no se pueden certificar sin el SQL original.

## Implementación

Java 21 y Spring Boot 4.0.7 con arquitectura por capas: controller, dto, service, service/impl, mapper, repository, entity, config, security y exception. La base contiene 28 entidades y repositorios. Los requests validados impiden asignar campos internos desde JSON; Usuario nunca serializa hashes ni datos de bloqueo. No hay páginas HTML, JSP, CSS ni JavaScript de frontend.

La autenticación local usa BCrypt y JWT firmados. La revocación se registra en AUDITORIA para persistir entre reinicios sin añadir tablas. Los roles se consultan en BD en cada petición; no se confía en un rol enviado por el cliente. Se incluye CORS configurable. El modo local no sustituye la integración SSO institucional; su aceptación requiere configurar y verificar Keycloak.

El almacenamiento usa un directorio privado o S3, seleccionable por entorno. S3 tiene carga mediante SDK y descargas autorizadas con URL prefirmada de cinco minutos; requiere bucket y credenciales reales. No se instala ni altera el paquete remoto automáticamente. Los servicios implementan las validaciones necesarias mientras los triggers están pendientes. Al activar los triggers, configurar DB_TRIGGERS_ACTIVOS=true para evitar notificaciones y auditoría duplicadas.

## Entrega y validación pendiente

La contraseña de Oracle no figura en el informe; DB_PASSWORD debe suministrarse por entorno. Los hashes de ejemplo de la BD son ficticios; un administrador debe provisionar hashes BCrypt reales. No se crean usuarios públicos ni se asigna ADMIN desde una petición anónima. La prueba con Oracle, instalación del paquete/triggers por DBA, integración SSO, política de moderación, aceptación de los 7 CU y revisión de prototipos deben constar como pendientes hasta su validación real.

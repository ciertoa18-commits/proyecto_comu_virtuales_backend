# Trazabilidad de requerimientos y casos de uso

Fuente: hoja 2 REQ FUNCIONALES del Excel recibido. Se conserva CU01–CU13; CUS01–CUS12 en otras hojas no representa la misma numeración. La selección CU01–CU07 para la iteración 1 es una propuesta pendiente de confirmación del equipo.

| CU | Nombre | RF | Iteración propuesta | Implementación | Evidencia UML |
|---|---|---|---|---|---|
| CU01 | Ingresar a la plataforma | RF01–RF04 | 1 | AuthController → AuthServiceImpl | [Análisis](diagramas/analisis/CU01.puml), [Diseño](diagramas/diseno/CU01.puml), [Secuencia](diagramas/secuencia/CU01.puml) |
| CU02 | Gestionar perfil de usuario | RF05–RF07 | 1 | PerfilController → PerfilService | [Análisis](diagramas/analisis/CU02.puml), [Diseño](diagramas/diseno/CU02.puml), [Secuencia](diagramas/secuencia/CU02.puml) |
| CU03 | Buscar comunidades | RF08–RF12 | 1 | ComunidadController → ComunidadServiceImpl | [Análisis](diagramas/analisis/CU03.puml), [Diseño](diagramas/diseno/CU03.puml), [Secuencia](diagramas/secuencia/CU03.puml) |
| CU04 | Unirse a comunidad | RF13–RF16 | 1 | ComunidadController → ComunidadServiceImpl | [Análisis](diagramas/analisis/CU04.puml), [Diseño](diagramas/diseno/CU04.puml), [Secuencia](diagramas/secuencia/CU04.puml) |
| CU05 | Crear comunidad | RF17–RF20 | 1 | ComunidadController → ComunidadServiceImpl | [Análisis](diagramas/analisis/CU05.puml), [Diseño](diagramas/diseno/CU05.puml), [Secuencia](diagramas/secuencia/CU05.puml) |
| CU06 | Gestionar miembros | RF21–RF24 | 1 | ComunidadController → ComunidadServiceImpl | [Análisis](diagramas/analisis/CU06.puml), [Diseño](diagramas/diseno/CU06.puml), [Secuencia](diagramas/secuencia/CU06.puml) |
| CU07 | Gestionar publicaciones educativas | RF25–RF35 | 1 | PublicacionController → PublicacionServiceImpl | [Análisis](diagramas/analisis/CU07.puml), [Diseño](diagramas/diseno/CU07.puml), [Secuencia](diagramas/secuencia/CU07.puml) |
| CU08 | Acceder a clases virtuales | RF36–RF37 | 2 | AcademicoController → AcademicoService | [Análisis](diagramas/analisis/CU08.puml), [Diseño](diagramas/diseno/CU08.puml), [Secuencia](diagramas/secuencia/CU08.puml) |
| CU09 | Gestionar cursos de la comunidad | RF38–RF41 | 2 | AcademicoController → AcademicoService | [Análisis](diagramas/analisis/CU09.puml), [Diseño](diagramas/diseno/CU09.puml), [Secuencia](diagramas/secuencia/CU09.puml) |
| CU10 | Obtener recursos | RF42–RF44 | 2 | AcademicoController → AcademicoService | [Análisis](diagramas/analisis/CU10.puml), [Diseño](diagramas/diseno/CU10.puml), [Secuencia](diagramas/secuencia/CU10.puml) |
| CU11 | Gestionar recursos educativos | RF45–RF49 | 2 | AcademicoController → AcademicoService | [Análisis](diagramas/analisis/CU11.puml), [Diseño](diagramas/diseno/CU11.puml), [Secuencia](diagramas/secuencia/CU11.puml) |
| CU12 | Gestionar notificaciones | RF50–RF52 | 2 | ComunicacionController → NotificationService | [Análisis](diagramas/analisis/CU12.puml), [Diseño](diagramas/diseno/CU12.puml), [Secuencia](diagramas/secuencia/CU12.puml) |
| CU13 | Proteger cuenta de usuario | RF53–RF55 | 2 | AuthController → AuthServiceImpl | [Análisis](diagramas/analisis/CU13.puml), [Diseño](diagramas/diseno/CU13.puml), [Secuencia](diagramas/secuencia/CU13.puml) |

El Excel asigna RF38–RF41 a CU09 y RF42–RF44 a CU10; se conserva esta separación de la fuente. Matrícula, notas, asistencia, tareas y mensajes requieren formalizar RF adicionales; se implementan como extensión del informe Oracle. No se presentan como RF originales aprobados.

## Requerimientos agregados por las revisiones del docente

Estos RF salen de las correcciones de la revisión y deben copiarse a la hoja 2 REQ FUNCIONALES del Excel. Los CU14–CU17 son nuevos; los demás RF amplían un CU existente.

| RF | El sistema debe… | CU | Implementación |
|---|---|---|---|
| RF56 | permitir registrar los idiomas que habla el usuario y marcar uno como principal. | CU02 | PerfilController → PerfilService, tabla USUARIO_IDIOMA |
| RF57 | mostrar los datos del perfil en modo lectura y editarlos solo con el botón Editar. | CU02 | perfil.html (frontend) |
| RF58 | exigir el Gmail personal y dejar el teléfono como opcional. | CU02 | Requests.Perfil, CuentaRequests |
| RF59 | confirmar el cambio de contraseña con un código de un solo uso enviado al Gmail personal. | CU13 | PerfilService.enviarCodigoPassword, CuentaService.nuevoCodigo |
| RF60 | permitir al administrador ver y editar todos los datos de un usuario. | CU14 | AdminUsuarioController → AdminUsuarioService |
| RF61 | permitir al administrador asignar una contraseña nueva o quitar el bloqueo de una cuenta. | CU14 | AdminUsuarioController (password, desbloquear) |
| RF62 | listar las solicitudes de ingreso con buscador, paginación y motivo al aceptar o rechazar. | CU06 | ComunidadController.requests, ComunidadServiceImpl.resolver |
| RF63 | filtrar los resultados de búsqueda mientras el usuario escribe. | CU03 | lista.html, usuarios.html, solicitudes.ts |
| RF64 | buscar el pueblo originario escribiendo su nombre al crear una comunidad. | CU05 | formulario.html (ng-select) |
| RF65 | registrar el idioma dominante y las segundas lenguas de una comunidad. | CU05 | ComunidadServiceImpl.guardar, tabla COMUNIDAD_IDIOMA |
| RF66 | permitir crear una categoría nueva desde Crear comunidad con la opción "Otro". | CU05 | formulario.ts, CatalogoService |
| RF67 | permitir crear, editar y eliminar categorías, pueblos originarios e idiomas. | CU15 | CatalogoController → CatalogoService |
| RF68 | permitir al docente crear categorías. | CU15 | CatalogoService.guardarCategoria |
| RF69 | importar varias comunidades desde un archivo Excel o CSV. | CU05 | ComunidadController.importar, importar.ts |
| RF70 | recomendar comunidades por idioma y lugar, excluyendo aquellas donde el usuario ya participa. | CU03 | ComunidadServiceImpl.recomendadas |
| RF71 | mostrar en los mensajes solo la hora (formato de 12 horas) y la fecha como separador. | CU12 | mensajes.html |
| RF72 | avisar en tiempo real de mensajes y notificaciones nuevas. | CU12 | WebSocket /ws, TiempoRealService, NotificacionListener |
| RF73 | permitir al docente crear el sílabo de su sección y a los estudiantes consultarlo. | CU16 | SilaboController → SilaboService, tabla SILABO |
| RF74 | registrar la asistencia como puntual, tardanza, falta o justificado con razón, y permitir modificarla. | CU08 | AcademicoService.asistencia, tab-asistencia.ts |
| RF75 | mostrar en Mis cursos el promedio, la asistencia, el sílabo y los recursos de cada curso. | CU10 | SilaboService.misCursos |
| RF76 | mostrar los recursos de Mis recursos después de elegir el curso. | CU10 | AcademicoController.myResources |
| RF77 | exigir permiso del administrador para modificar una nota ya registrada. | CU17 | AcademicoService.nota, PermisoNotaService |
| RF78 | notificar por el sistema y por correo las solicitudes de permiso de notas y su respuesta. | CU17 | PermisoNotaService |
| RF79 | confirmar las acciones con un cuadro propio de la aplicación. | Todos | shared/dialogo.ts |
| RF80 | usar íconos en lugar de emojis en toda la interfaz. | Todos | shared/icono.ts |

| CU nuevo | Nombre | RF | Implementación |
|---|---|---|---|
| CU14 | Recuperar y administrar cuentas | RF60–RF61 | AdminUsuarioController, recuperar-cuenta.ts |
| CU15 | Gestionar catálogos | RF67–RF68 | CatalogoController, catalogos.ts |
| CU16 | Gestionar sílabo | RF73 | SilaboController, tab-silabo.ts |
| CU17 | Solicitar permiso para modificar notas | RF77–RF78 | PermisoNotaController, tab-libreta.ts, permisos-nota.ts |

Scripts de base de datos de estas revisiones: `12_usuario_idiomas.sql`, `13_catalogos_y_segundas_lenguas.sql`, `14_silabo.sql`, `15_permiso_nota.sql` y `16_datos_mis_clases.sql` (datos de prueba).

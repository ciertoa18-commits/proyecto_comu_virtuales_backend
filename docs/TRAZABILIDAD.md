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

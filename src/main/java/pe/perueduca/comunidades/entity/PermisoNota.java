package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PERMISO_NOTA")
@Getter
@Setter
public class PermisoNota {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_permiso", nullable = false)
  private Long idPermiso;

  @Column(name = "id_seccion", nullable = false)
  private Long idSeccion;

  @Column(name = "id_docente", nullable = false)
  private Long idDocente;

  @Column(name = "motivo", nullable = false, length = 500)
  private String motivo;

  @Column(name = "estado", nullable = false, length = 10)
  private String estado;

  @Column(name = "fecha_solicitud", nullable = false)
  private LocalDateTime fechaSolicitud;

  @Column(name = "id_revisor")
  private Long idRevisor;

  @Column(name = "respuesta", length = 500)
  private String respuesta;

  @Column(name = "fecha_respuesta")
  private LocalDateTime fechaRespuesta;

  @Column(name = "vence")
  private LocalDateTime vence;
}

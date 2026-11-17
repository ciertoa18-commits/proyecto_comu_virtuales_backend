package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.SolicitudIngresoEstado;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SOLICITUD_INGRESO")
@Getter
@Setter
public class SolicitudIngreso {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_solicitud", nullable = false)
  private Long idSolicitud;

  @Column(name = "id_comunidad", nullable = false)
  private Long idComunidad;

  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private SolicitudIngresoEstado estado;

  @Column(name = "mensaje", length = 500)
  private String mensaje;

  @Column(name = "fecha_solicitud", nullable = false)
  private LocalDateTime fechaSolicitud;

  @Column(name = "id_revisor")
  private Long idRevisor;

  @Column(name = "fecha_respuesta")
  private LocalDateTime fechaRespuesta;

  @Column(name = "motivo_rechazo", length = 500)
  private String motivoRechazo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_comunidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Comunidad refIdComunidad;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdUsuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_revisor", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdRevisor;
}

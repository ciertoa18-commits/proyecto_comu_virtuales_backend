package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.util.BooleanNumericoConverter;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "NOTIFICACION")
@Getter
@Setter
public class Notificacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_notificacion", nullable = false)
  private Long idNotificacion;

  @Column(name = "id_destinatario", nullable = false)
  private Long idDestinatario;

  @Column(name = "id_emisor")
  private Long idEmisor;

  @Column(name = "id_comunidad")
  private Long idComunidad;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 30)
  private NotificacionTipo tipo;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Column(name = "contenido", length = 500)
  private String contenido;

  @Column(name = "entidad_tipo", length = 30)
  private String entidadTipo;

  @Column(name = "entidad_id")
  private Long entidadId;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "leida", nullable = false)
  private Boolean leida;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  @Column(name = "fecha_lectura")
  private LocalDateTime fechaLectura;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_destinatario", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdDestinatario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_emisor", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdEmisor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_comunidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Comunidad refIdComunidad;
}

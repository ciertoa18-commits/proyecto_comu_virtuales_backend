package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.util.BooleanNumericoConverter;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "MENSAJE")
@Getter
@Setter
@org.hibernate.annotations.SQLRestriction("eliminado = 0")
public class Mensaje {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_mensaje", nullable = false)
  private Long idMensaje;

  @Column(name = "id_emisor", nullable = false)
  private Long idEmisor;

  @Column(name = "id_receptor", nullable = false)
  private Long idReceptor;

  @Column(name = "contenido", nullable = false, length = 4000)
  private String contenido;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "leido", nullable = false)
  private Boolean leido;

  @Column(name = "fecha_envio", nullable = false)
  private LocalDateTime fechaEnvio;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "eliminado", nullable = false)
  private Boolean eliminado;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_emisor", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdEmisor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_receptor", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdReceptor;
}

package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PUEBLO_ORIGINARIO")
@Getter
@Setter
public class PuebloOriginario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_pueblo", nullable = false)
  private Long idPueblo;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Column(name = "familia_linguistica", length = 100)
  private String familiaLinguistica;
}

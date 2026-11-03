package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "IDIOMA")
@Getter
@Setter
public class Idioma {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_idioma", nullable = false)
  private Long idIdioma;

  @Column(name = "codigo", nullable = false, length = 10)
  private String codigo;

  @Column(name = "nombre", nullable = false, length = 60)
  private String nombre;
}

package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ROL")
@Getter
@Setter
public class Rol {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_rol", nullable = false)
  private Long idRol;

  @Enumerated(EnumType.STRING)
  @Column(name = "codigo", nullable = false, length = 20)
  private RolCodigo codigo;

  @Column(name = "nombre", nullable = false, length = 50)
  private String nombre;
}

package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.UsuarioEstado;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "USUARIO")
@Getter
@Setter
public class Usuario {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_foto_perfil", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Archivo refFotoPerfil;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Column(name = "id_rol", nullable = false)
  private Long idRol;

  @Column(name = "correo", nullable = false, length = 150)
  private String correo;

  @Column(name = "nombres", nullable = false, length = 100)
  private String nombres;

  @Column(name = "apellidos", nullable = false, length = 100)
  private String apellidos;

  @Column(name = "especialidad", length = 100)
  private String especialidad;

  @Column(name = "telefono", length = 15)
  private String telefono;

  @Column(name = "correo_recuperacion", length = 150)
  private String correoRecuperacion;

  @Column(name = "keycloak_sub", length = 64)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private String keycloakSub;

  @Column(name = "password_hash", length = 255)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private String passwordHash;

  @Column(name = "intentos_fallidos", nullable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Long intentosFallidos;

  @Column(name = "bloqueado_hasta")
  @com.fasterxml.jackson.annotation.JsonIgnore
  private LocalDateTime bloqueadoHasta;

  @Column(name = "id_foto_perfil")
  private Long idFotoPerfil;

  @Column(name = "id_idioma_pref")
  private Long idIdiomaPref;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private UsuarioEstado estado;

  @Column(name = "fecha_registro", nullable = false)
  private LocalDateTime fechaRegistro;

  @Column(name = "fecha_actualizacion")
  private LocalDateTime fechaActualizacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_rol", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Rol refIdRol;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_idioma_pref", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Idioma refIdIdiomaPref;
}

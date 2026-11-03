package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;

@Converter
public class BooleanNumericoConverter implements AttributeConverter<Boolean, Integer> {
  public Integer convertToDatabaseColumn(Boolean v) {
    return v == null ? null : v ? 1 : 0;
  }

  public Boolean convertToEntityAttribute(Integer v) {
    return v == null ? null : v == 1;
  }
}

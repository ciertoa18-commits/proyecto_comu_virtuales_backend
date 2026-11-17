package pe.perueduca.comunidades.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;

  public JwtService(@Value("${app.jwt.secret}") String secret) {
    key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  public String issue(String correo) {
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer("comunidades-api")
        .subject(correo)
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(3600)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    Claims c =
        Jwts.parser()
            .verifyWith(key)
            .requireIssuer("comunidades-api")
            .build()
            .parseSignedClaims(token)
            .getPayload();
    if (c.getExpiration() == null || c.getId() == null || c.getSubject() == null)
      throw new JwtException("Token incompleto");
    return c;
  }
}

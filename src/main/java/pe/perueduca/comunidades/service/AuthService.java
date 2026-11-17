package pe.perueduca.comunidades.service;

import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.service.impl.AuthServiceImpl.LoginResult;

public interface AuthService {
  LoginResult login(Requests.Login request);

  void logout(String token);
}

package pe.edu.upeu.comunidades.service;

import pe.edu.upeu.comunidades.dto.Requests;
import pe.edu.upeu.comunidades.service.impl.AuthServiceImpl.LoginResult;

public interface AuthService {
  LoginResult login(Requests.Login request);

  void logout(String token);
}

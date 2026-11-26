package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarPasswordRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.LoginRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuthenticatedUserResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void logout(HttpServletRequest request, HttpServletResponse response);

    AuthenticatedUserResponse me();

    void cambiarPassword(CambiarPasswordRequest request, HttpServletRequest httpRequest);
}

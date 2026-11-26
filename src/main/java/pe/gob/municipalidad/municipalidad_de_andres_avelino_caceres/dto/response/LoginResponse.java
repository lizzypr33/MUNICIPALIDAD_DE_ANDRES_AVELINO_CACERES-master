package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.util.List;

public record LoginResponse(String message, String username, List<String> roles) {
}

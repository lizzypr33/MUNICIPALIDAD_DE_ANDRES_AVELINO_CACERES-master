package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Expira las sesiones HTTP activas de un usuario (desactivación, cambio de contraseña o de roles).
 * El ConcurrentSessionFilter invalida la sesión expirada en la siguiente petición.
 */
@Component
public class SesionesUsuarioManager {

    private final SessionRegistry sessionRegistry;

    public SesionesUsuarioManager(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * El SessionRegistry indexa por el objeto principal (no por nombre), por eso se recorren
     * los principales registrados y se compara su username.
     *
     * @param username        usuario cuyas sesiones se cierran
     * @param conservarActual si es true, no cierra la sesión de la petición en curso
     */
    public void cerrarSesiones(String username, boolean conservarActual) {
        String sesionActual = conservarActual ? idSesionActual() : null;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails detalles && username.equals(detalles.getUsername())) {
                for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                    if (!info.getSessionId().equals(sesionActual)) {
                        info.expireNow();
                    }
                }
            }
        }
    }

    private String idSesionActual() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            HttpSession session = request.getSession(false);
            return session != null ? session.getId() : null;
        }
        return null;
    }
}

package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.RestAccessDeniedHandler;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.RestAuthenticationEntryPoint;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.RestSessionExpiredStrategy;

/**
 * Seguridad basada en sesiones HTTP.
 * <ul>
 *   <li>Públicos: GET /api/public/health, GET /api/auth/csrf, POST /api/auth/login.</li>
 *   <li>/api/usuarios/** y /api/roles/**: solo ADMINISTRADOR.</li>
 *   <li>/api/ciudadanos/**, /api/representantes/** y /api/establecimientos/**: según matriz de roles
 *       (ADMINISTRADOR, FUNCIONARIO, SUPERVISOR, CIUDADANO sobre sus propios registros).</li>
 *   <li>Resto de /api/**: cualquier usuario autenticado.</li>
 *   <li>CSRF activo con cookie XSRF-TOKEN legible por JavaScript (cabecera X-XSRF-TOKEN).</li>
 *   <li>Sin HTTP Basic ni formulario de login: respuestas 401/403 en JSON.</li>
 *   <li>Sin CORS: frontend y backend comparten origen.</li>
 * </ul>
 * La cookie de sesión usa HttpOnly y SameSite=Lax (application.properties); Secure se activa en producción.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            SessionRegistry sessionRegistry,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            RestSessionExpiredStrategy sessionExpiredStrategy) throws Exception {

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/", "/index.html", "/login.html", "/dashboard.html",
                                "/pages/verificar-licencia.html",
                                "/favicon.ico", "/assets/**")
                                .permitAll()
                        // Páginas HTML privadas de módulos administrativos (PROMPT 11, 12, 13 y 14)
                        .requestMatchers(HttpMethod.GET, "/pages/usuarios.html", "/pages/tipos-tramite.html", "/pages/requisitos.html")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/pages/auditoria.html", "/pages/auditoria-detalle.html")
                                .hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/pages/representantes.html", "/pages/emitir-licencia.html",
                                "/pages/reportes.html", "/pages/reporte-detalle.html")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET,
                                "/pages/ciudadanos.html", "/pages/establecimientos.html",
                                "/pages/solicitudes.html", "/pages/nueva-solicitud.html", "/pages/detalle-solicitud.html",
                                "/pages/licencias.html", "/pages/detalle-licencia.html",
                                "/pages/inspecciones.html", "/pages/detalle-inspeccion.html",
                                "/pages/pagos.html", "/pages/detalle-pago.html",
                                "/pages/notificaciones.html", "/pages/notificacion-detalle.html")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.GET, "/pages/**")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/public/health", "/api/auth/csrf",
                                "/api/public/licencias/verificar/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/usuarios/**", "/api/roles/**").hasRole("ADMINISTRADOR")
                        // Ciudadanos
                        .requestMatchers(HttpMethod.GET, "/api/ciudadanos", "/api/ciudadanos/documento")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/ciudadanos/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/ciudadanos")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/ciudadanos/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "CIUDADANO")
                        .requestMatchers(HttpMethod.PATCH, "/api/ciudadanos/*/estado")
                                .hasRole("ADMINISTRADOR")
                        // Representantes
                        .requestMatchers(HttpMethod.GET, "/api/representantes", "/api/representantes/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.POST, "/api/representantes")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/representantes/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.PATCH, "/api/representantes/*/estado")
                                .hasRole("ADMINISTRADOR")
                        // Establecimientos
                        .requestMatchers(HttpMethod.GET, "/api/establecimientos", "/api/establecimientos/ruc/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/establecimientos/ciudadano/*", "/api/establecimientos/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/establecimientos")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/establecimientos/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "CIUDADANO")
                        .requestMatchers(HttpMethod.PATCH, "/api/establecimientos/*/estado")
                                .hasRole("ADMINISTRADOR")
                        // Tipos de trámite y requisitos
                        .requestMatchers(HttpMethod.GET, "/api/tipos-tramite", "/api/tipos-tramite/*",
                                "/api/tipos-tramite/*/requisitos", "/api/requisitos", "/api/requisitos/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/tipos-tramite", "/api/requisitos")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/tipos-tramite/*", "/api/tipos-tramite/*/requisitos",
                                "/api/requisitos/*")
                                .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/tipos-tramite/*/estado", "/api/requisitos/*/estado")
                                .hasRole("ADMINISTRADOR")
                        // Solicitudes y documentos
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes", "/api/solicitudes/*",
                                "/api/solicitudes/expediente/*", "/api/solicitudes/*/historial",
                                "/api/solicitudes/*/documentos", "/api/documentos/*", "/api/documentos/*/descargar")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes", "/api/solicitudes/*/presentar",
                                "/api/solicitudes/*/documentos")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "CIUDADANO")
                        .requestMatchers(HttpMethod.PUT, "/api/solicitudes/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "CIUDADANO")
                        .requestMatchers(HttpMethod.PATCH, "/api/solicitudes/*/subsanar")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "CIUDADANO")
                        .requestMatchers(HttpMethod.DELETE, "/api/documentos/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "CIUDADANO")
                        .requestMatchers(HttpMethod.PATCH, "/api/solicitudes/*/asignar")
                                .hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/solicitudes/*/iniciar-revision",
                                "/api/solicitudes/*/observar", "/api/solicitudes/*/aprobar",
                                "/api/solicitudes/*/rechazar", "/api/solicitudes/*/archivar")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        // Inspecciones municipales (PROMPT 06)
                        .requestMatchers(HttpMethod.GET, "/api/inspecciones", "/api/inspecciones/*",
                                "/api/inspecciones/solicitud/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/inspecciones")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PUT, "/api/inspecciones/*/reprogramar")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/inspecciones/*/iniciar",
                                "/api/inspecciones/*/resultado", "/api/inspecciones/*/cancelar")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        // Licencias de funcionamiento (PROMPT 06)
                        .requestMatchers(HttpMethod.GET, "/api/licencias", "/api/licencias/*",
                                "/api/licencias/codigo/*", "/api/licencias/solicitud/*",
                                "/api/licencias/*/certificado", "/api/licencias/*/historial")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/licencias/emitir")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/licencias/*/suspender",
                                "/api/licencias/*/reactivar", "/api/licencias/*/revocar")
                                .hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
                        // Pagos municipales (PROMPT 07)
                        .requestMatchers(HttpMethod.GET, "/api/pagos/resumen")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/pagos", "/api/pagos/*",
                                "/api/pagos/solicitud/*", "/api/pagos/*/constancia")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/pagos")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "CIUDADANO")
                        .requestMatchers(HttpMethod.PATCH, "/api/pagos/*/verificar",
                                "/api/pagos/*/rechazar", "/api/pagos/*/anular")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        // Notificaciones internas (PROMPT 07)
                        .requestMatchers(HttpMethod.GET, "/api/notificaciones", "/api/notificaciones/no-leidas",
                                "/api/notificaciones/contador", "/api/notificaciones/*")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.PATCH, "/api/notificaciones/leer-todas",
                                "/api/notificaciones/*/leer")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/api/notificaciones")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        // Auditoría municipal (PROMPT 07)
                        .requestMatchers(HttpMethod.GET, "/api/auditorias", "/api/auditorias/*")
                                .hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
                        // Dashboard y Reportes municipales (PROMPT 08)
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**", "/api/reportes/**")
                                .hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(-1)
                        .expiredSessionStrategy(sessionExpiredStrategy)
                        .sessionRegistry(sessionRegistry))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // El cierre de sesión lo gestiona AuthController para auditar antes de invalidar.
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Expone el AuthenticationManager que usa el UserDetailsService y el PasswordEncoder de arriba. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Notifica al SessionRegistry cuando una sesión se destruye, para no dejar entradas huérfanas. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}

package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.*;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.*;

/**
 * Requieren PostgreSQL real (base andres_test) y la variable DB_PASSWORD.
 * Cada prueba se revierte al terminar (@Transactional).
 */
@SpringBootTest
@Transactional
class PersistenciaTests {

    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired CiudadanoRepository ciudadanos;
    @Autowired EstablecimientoRepository establecimientos;
    @Autowired TipoTramiteRepository tiposTramite;
    @Autowired SolicitudRepository solicitudes;
    @Autowired LicenciaRepository licencias;
    @Autowired PagoRepository pagos;
    @Autowired NotificacionRepository notificaciones;

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Ciudadano nuevoCiudadano() {
        Ciudadano c = new Ciudadano();
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento(uid());
        c.setNombres("Prueba");
        c.setApellidos("Test");
        return ciudadanos.saveAndFlush(c);
    }

    private Establecimiento nuevoEstablecimiento(Ciudadano titular) {
        Establecimiento e = new Establecimiento();
        e.setNombreComercial("Local " + uid());
        e.setDireccion("Jr. Prueba 123");
        e.setAreaMetrosCuadrados(new BigDecimal("45.50"));
        e.setCiudadano(titular);
        return establecimientos.saveAndFlush(e);
    }

    private TipoTramite nuevoTipoTramite() {
        TipoTramite t = new TipoTramite();
        t.setCodigo("T-" + uid());
        t.setNombre("Tipo de prueba");
        return tiposTramite.saveAndFlush(t);
    }

    private Solicitud nuevaSolicitud(String expediente) {
        Ciudadano c = nuevoCiudadano();
        Solicitud s = new Solicitud();
        s.setNumeroExpediente(expediente);
        s.setCiudadano(c);
        s.setEstablecimiento(nuevoEstablecimiento(c));
        s.setTipoTramite(nuevoTipoTramite());
        return s;
    }

    @Test
    void flywayCreoLasTablasYRegistroLasMigraciones() {
        List<String> esperadas = List.of("usuarios", "roles", "usuarios_roles", "ciudadanos", "representantes",
                "establecimientos", "tipos_tramite", "solicitudes", "requisitos", "tipos_tramite_requisitos",
                "documentos", "inspecciones", "licencias", "pagos", "notificaciones", "auditorias");
        List<String> existentes = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = database()", String.class);
        assertThat(existentes).containsAll(esperadas);

        Integer migraciones = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = 1 and version in ('1','2')", Integer.class);
        assertThat(migraciones).isEqualTo(2);
    }

    @Test
    void rolesInicialesCargadosYSinUsuarios() {
        for (String nombre : List.of("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR", "CIUDADANO")) {
            assertThat(roles.findByNombre(nombre)).as(nombre).isPresent();
        }
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void persisteUsuarioConRoles() {
        Usuario u = new Usuario();
        u.setUsername("u_" + uid());
        u.setPassword("$2a$10$hashDePruebaNoReal");
        u.setNombres("Ana");
        u.setApellidos("Prueba");
        u.setEmail(uid() + "@test.pe");
        u.setRoles(Set.of(roles.findByNombre("FUNCIONARIO").orElseThrow()));
        Usuario guardado = usuarios.saveAndFlush(u);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(usuarios.findByUsername(u.getUsername())).isPresent();
        assertThat(usuarios.existsByUsername(u.getUsername())).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from usuarios_roles where usuario_id = ?", Integer.class,
                guardado.getId())).isEqualTo(1);
    }

    @Test
    void usernameDuplicadoSeRechaza() {
        String username = "dup_" + uid();
        for (int i = 0; i < 2; i++) {
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setPassword("hash");
            u.setNombres("A");
            u.setApellidos("B");
            u.setEmail(uid() + "@test.pe");
            if (i == 0) {
                usuarios.saveAndFlush(u);
            } else {
                assertThrows(DataIntegrityViolationException.class, () -> usuarios.saveAndFlush(u));
            }
        }
    }

    @Test
    void persisteCiudadanoYRechazaDocumentoDuplicado() {
        Ciudadano c = nuevoCiudadano();
        assertThat(ciudadanos.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, c.getNumeroDocumento()))
                .isPresent();
        // el mismo número con otro tipo de documento es válido
        Ciudadano otro = new Ciudadano();
        otro.setTipoDocumento(TipoDocumento.CARNE_EXTRANJERIA);
        otro.setNumeroDocumento(c.getNumeroDocumento());
        otro.setNombres("X");
        otro.setApellidos("Y");
        ciudadanos.saveAndFlush(otro);

        Ciudadano dup = new Ciudadano();
        dup.setTipoDocumento(TipoDocumento.DNI);
        dup.setNumeroDocumento(c.getNumeroDocumento());
        dup.setNombres("X");
        dup.setApellidos("Y");
        assertThrows(DataIntegrityViolationException.class, () -> ciudadanos.saveAndFlush(dup));
    }

    @Test
    void relacionEstablecimientoSolicitudYConsultas() {
        Solicitud s = nuevaSolicitud("EXP-" + uid());
        Solicitud guardada = solicitudes.saveAndFlush(s);

        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(solicitudes.findByNumeroExpediente(s.getNumeroExpediente())).isPresent();
        assertThat(solicitudes.findByCiudadanoId(s.getCiudadano().getId())).hasSize(1);
        assertThat(solicitudes.findByEstado(EstadoSolicitud.BORRADOR)).contains(guardada);
        assertThat(establecimientos.findByCiudadanoId(s.getCiudadano().getId())).hasSize(1);
    }

    @Test
    void expedienteDuplicadoSeRechaza() {
        String exp = "EXP-" + uid();
        solicitudes.saveAndFlush(nuevaSolicitud(exp));
        Solicitud dup = nuevaSolicitud(exp);
        assertThrows(DataIntegrityViolationException.class, () -> solicitudes.saveAndFlush(dup));
    }

    @Test
    void licenciaConCodigoUnicoYUnaPorSolicitud() {
        Solicitud s1 = solicitudes.saveAndFlush(nuevaSolicitud("EXP-" + uid()));
        Licencia l = new Licencia();
        l.setCodigoLicencia("LIC-" + uid());
        l.setSolicitud(s1);
        l.setFechaEmision(LocalDate.now());
        licencias.saveAndFlush(l);
        assertThat(licencias.findByCodigoLicencia(l.getCodigoLicencia())).isPresent();
        assertThat(l.getFechaVencimiento()).isNull();

        Solicitud s2 = solicitudes.saveAndFlush(nuevaSolicitud("EXP-" + uid()));
        Licencia mismoCodigo = new Licencia();
        mismoCodigo.setCodigoLicencia(l.getCodigoLicencia());
        mismoCodigo.setSolicitud(s2);
        mismoCodigo.setFechaEmision(LocalDate.now());
        assertThrows(DataIntegrityViolationException.class, () -> licencias.saveAndFlush(mismoCodigo));
    }

    @Test
    void segundaLicenciaParaLaMismaSolicitudSeRechaza() {
        Solicitud s = solicitudes.saveAndFlush(nuevaSolicitud("EXP-" + uid()));
        Licencia a = new Licencia();
        a.setCodigoLicencia("LIC-" + uid());
        a.setSolicitud(s);
        a.setFechaEmision(LocalDate.now());
        licencias.saveAndFlush(a);

        Licencia b = new Licencia();
        b.setCodigoLicencia("LIC-" + uid());
        b.setSolicitud(s);
        b.setFechaEmision(LocalDate.now());
        assertThrows(DataIntegrityViolationException.class, () -> licencias.saveAndFlush(b));
    }

    @Test
    void pagosYNotificacionesPorEntidad() {
        Solicitud s = solicitudes.saveAndFlush(nuevaSolicitud("EXP-" + uid()));
        Pago p = new Pago();
        p.setSolicitud(s);
        p.setConcepto("Concepto de prueba");
        p.setMonto(new BigDecimal("10.50"));
        pagos.saveAndFlush(p);
        assertThat(pagos.findBySolicitudId(s.getId())).hasSize(1);
        assertThat(p.getMoneda()).isEqualTo("PEN");
        assertThat(p.getEstado()).isEqualTo(EstadoPago.PENDIENTE);

        Usuario u = new Usuario();
        u.setUsername("n_" + uid());
        u.setPassword("hash");
        u.setNombres("N");
        u.setApellidos("N");
        u.setEmail(uid() + "@test.pe");
        usuarios.saveAndFlush(u);
        Notificacion n = new Notificacion();
        n.setUsuario(u);
        n.setTitulo("Hola");
        n.setMensaje("Mensaje");
        notificaciones.saveAndFlush(n);
        assertThat(notificaciones.findByUsuarioIdOrderByFechaCreacionDesc(u.getId())).hasSize(1);
        assertThat(notificaciones.countByUsuarioIdAndLeidaFalse(u.getId())).isEqualTo(1);
    }

    @Test
    void clavesForaneasSeRespetan() {
        Solicitud huerfana = nuevaSolicitud("EXP-" + uid());
        Ciudadano inexistente = new Ciudadano();
        inexistente.setId(-1L);
        huerfana.setCiudadano(inexistente);
        assertThrows(Exception.class, () -> solicitudes.saveAndFlush(huerfana));
    }
}

package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Pago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;

/**
 * Validador centralizado de montos, referencias, seguridad de datos financieros
 * y transiciones de estado para {@link Pago}.
 */
@Component
public class PagoValidator {

    private static final String MONEDA_PERMITIDA = "PEN";
    private static final Pattern PATRON_POSIBLE_PAN_TARJETA = Pattern.compile(
            "(?:\\b(?:\\d[ -]*?){13,19}\\b)");
    private static final Pattern PATRON_CVV_EXPLICITO = Pattern.compile(
            "(?i)\\b(cvv|cvc|ccv)\\s*[:=]?\\s*\\d{3,4}\\b");

    private static final Map<EstadoPago, Set<EstadoPago>> TRANSICIONES_PERMITIDAS;

    static {
        Map<EstadoPago, Set<EstadoPago>> mapa = new EnumMap<>(EstadoPago.class);
        Set<EstadoPago> desdePendiente = EnumSet.of(
                EstadoPago.VERIFICADO,
                EstadoPago.RECHAZADO,
                EstadoPago.ANULADO);
        mapa.put(EstadoPago.PENDIENTE_VERIFICACION, desdePendiente);
        mapa.put(EstadoPago.PENDIENTE, desdePendiente);
        mapa.put(EstadoPago.VERIFICADO, EnumSet.of(EstadoPago.ANULADO));
        mapa.put(EstadoPago.PAGADO, EnumSet.of(EstadoPago.ANULADO));
        mapa.put(EstadoPago.RECHAZADO, EnumSet.noneOf(EstadoPago.class));
        mapa.put(EstadoPago.ANULADO, EnumSet.noneOf(EstadoPago.class));
        mapa.put(EstadoPago.REEMBOLSADO, EnumSet.noneOf(EstadoPago.class));
        TRANSICIONES_PERMITIDAS = Map.copyOf(mapa);
    }

    public void validarSolicitudCompatible(Solicitud solicitud) {
        if (solicitud == null) {
            throw new BusinessException("La solicitud es obligatoria para registrar un pago.");
        }
        if (solicitud.getEstado() == EstadoSolicitud.RECHAZADA
                || solicitud.getEstado() == EstadoSolicitud.ARCHIVADA) {
            throw new ConflictException(
                    "No se pueden registrar pagos para una solicitud en estado " + solicitud.getEstado() + ".");
        }
    }

    public String validarYNormalizarMoneda(String moneda) {
        if (!StringUtils.hasText(moneda)) {
            return MONEDA_PERMITIDA;
        }
        String limpia = moneda.trim().toUpperCase();
        if (!MONEDA_PERMITIDA.equals(limpia)) {
            throw new BusinessException(
                    "Moneda no soportada ('" + limpia + "'). Únicamente se admiten pagos en moneda PEN.");
        }
        return MONEDA_PERMITIDA;
    }

    public void validarDatosPago(
            BigDecimal monto,
            String codigoOperacion,
            String concepto,
            String medioPago,
            LocalDateTime fechaPago,
            String observaciones) {

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto del pago debe ser estrictamente mayor que cero.");
        }
        if (monto.scale() > 2) {
            throw new BusinessException("El monto del pago no puede tener más de dos decimales.");
        }
        if (!StringUtils.hasText(codigoOperacion)) {
            throw new BusinessException("El código de operación o referencia es obligatorio.");
        }
        if (!StringUtils.hasText(concepto)) {
            throw new BusinessException("El concepto del pago es obligatorio.");
        }
        if (!StringUtils.hasText(medioPago)) {
            throw new BusinessException("El medio de pago es obligatorio.");
        }
        if (fechaPago == null) {
            throw new BusinessException("La fecha declarada del pago es obligatoria.");
        }
        if (fechaPago.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessException("La fecha declarada del pago no puede ser una fecha futura.");
        }

        validarSinDatosDeTarjeta(codigoOperacion, "código de operación");
        validarSinDatosDeTarjeta(observaciones, "observaciones");
    }

    public void validarSinDatosDeTarjeta(String texto, String nombreCampo) {
        if (!StringUtils.hasText(texto)) {
            return;
        }
        String soloDigitos = texto.replaceAll("[\\s\\-]", "");
        if (soloDigitos.matches("^\\d{15,19}$") || PATRON_CVV_EXPLICITO.matcher(texto).find()
                || (PATRON_POSIBLE_PAN_TARJETA.matcher(texto).find() && texto.contains("-"))) {
            throw new BusinessException(
                    "Por seguridad no se permite registrar números completos de tarjetas ni códigos CVV en "
                            + nombreCampo + ".");
        }
    }

    public void validarTransicion(
            Pago pago,
            EstadoPago estadoNuevo,
            String motivoORespaldo,
            boolean tieneAutoridadExcepcional) {

        if (pago == null || estadoNuevo == null) {
            throw new BusinessException("El pago y el estado destino son obligatorios.");
        }
        if (!StringUtils.hasText(motivoORespaldo)) {
            throw new BusinessException(
                    "El motivo o sustento administrativo es obligatorio para cambiar el estado del pago.");
        }

        EstadoPago estadoActual = pago.getEstado();
        Set<EstadoPago> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoNuevo)) {
            throw new ConflictException(
                    "Transición de pago no permitida: no se puede pasar de " + estadoActual + " a " + estadoNuevo
                            + ".");
        }

        if ((estadoActual == EstadoPago.VERIFICADO || estadoActual == EstadoPago.PAGADO)
                && estadoNuevo == EstadoPago.ANULADO
                && !tieneAutoridadExcepcional) {
            throw new ConflictException(
                    "La anulación excepcional de un pago ya VERIFICADO requiere autorización de un ADMINISTRADOR o SUPERVISOR.");
        }
    }
}

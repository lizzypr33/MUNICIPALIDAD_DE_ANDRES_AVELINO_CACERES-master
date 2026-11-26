package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(length = 50)
    private String codigoOperacion;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    private LocalDateTime fechaPago;

    @Column(length = 50)
    private String medioPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_verificador_id")
    private Usuario usuarioVerificador;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "motivo_rechazo_anulacion", columnDefinition = "TEXT")
    private String motivoRechazoAnulacion;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Version
    @Column(nullable = false)
    private Long version = 0L;
}

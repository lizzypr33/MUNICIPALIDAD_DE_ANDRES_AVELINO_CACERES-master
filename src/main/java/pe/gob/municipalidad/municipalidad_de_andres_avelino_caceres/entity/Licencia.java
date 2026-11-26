package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDate;
import java.util.UUID;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "licencias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_licencias_codigo", columnNames = "codigo_licencia"),
        @UniqueConstraint(name = "uk_licencias_solicitud", columnNames = "solicitud_id"),
        @UniqueConstraint(name = "uk_licencias_token_verificacion", columnNames = "token_verificacion") })
@Getter
@Setter
@NoArgsConstructor
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigoLicencia;

    /** Token aleatorio no predecible para verificación pública mediante código QR. */
    @Column(name = "token_verificacion", nullable = false, length = 64)
    private String tokenVerificacion = UUID.randomUUID().toString().replace("-", "");

    /** Como máximo una licencia por solicitud. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    /** Opcional: no toda licencia tiene vigencia limitada. */
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoLicencia estado = EstadoLicencia.VIGENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_emisor_id")
    private Usuario usuarioEmisor;
}

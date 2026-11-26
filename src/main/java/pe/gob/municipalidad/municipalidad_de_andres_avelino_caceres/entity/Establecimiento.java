package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "establecimientos")
@Getter
@Setter
@NoArgsConstructor
public class Establecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombreComercial;

    @Column(length = 200)
    private String razonSocial;

    /** Opcional; un mismo RUC puede tener varios locales. */
    @Column(length = 11)
    private String ruc;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(length = 255)
    private String referencia;

    @Column(length = 150)
    private String giroNegocio;

    @Column(precision = 10, scale = 2)
    private BigDecimal areaMetrosCuadrados;

    private Integer numeroPisos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudadano_id")
    private Ciudadano ciudadano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representante_id")
    private Representante representante;
}

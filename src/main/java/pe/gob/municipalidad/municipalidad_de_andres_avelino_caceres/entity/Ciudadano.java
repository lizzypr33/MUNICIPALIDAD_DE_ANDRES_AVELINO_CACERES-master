package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ciudadanos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ciudadanos_documento", columnNames = { "tipo_documento", "numero_documento" }),
        @UniqueConstraint(name = "uk_ciudadanos_usuario", columnNames = "usuario_id") })
@Getter
@Setter
@NoArgsConstructor
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** Acceso opcional al portal. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}

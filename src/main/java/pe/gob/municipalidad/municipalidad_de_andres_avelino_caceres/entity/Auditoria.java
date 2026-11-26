package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro de operaciones. No debe contener contraseñas, tokens ni datos sensibles. */
@Entity
@Table(name = "auditorias")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null para operaciones sin usuario autenticado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(nullable = false, length = 100)
    private String entidadAfectada;

    private Long entidadId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaOperacion;

    @Column(length = 45)
    private String direccionIp;
}

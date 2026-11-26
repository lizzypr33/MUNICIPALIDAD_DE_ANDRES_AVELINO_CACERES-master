package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuarios_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email") })
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    /** Solo hash BCrypt, nunca texto plano. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    private LocalDateTime ultimoAcceso;

    /** BatchSize evita consultas N+1 al cargar roles de varios usuarios a la vez. */
    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @JoinTable(name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();
}

package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipos_tramite_requisitos", uniqueConstraints = @UniqueConstraint(
        name = "uk_tipo_tramite_requisito", columnNames = { "tipo_tramite_id", "requisito_id" }))
@Getter
@Setter
@NoArgsConstructor
public class TipoTramiteRequisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_tramite_id", nullable = false)
    private TipoTramite tipoTramite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisito_id", nullable = false)
    private Requisito requisito;

    @Column(nullable = false)
    private boolean obligatorio = true;

    @Column(nullable = false)
    private int ordenVisualizacion = 0;
}

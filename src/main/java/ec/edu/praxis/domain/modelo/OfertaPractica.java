package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla oferta_practica.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "oferta_practica")
public class OfertaPractica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "empresa_id")
    public Long empresa_id;

    @Column(name = "convenio_id")
    public Long convenio_id;

    @Column(name = "tipo_practica_id")
    public Long tipo_practica_id;

    @Column(name = "periodo_id")
    public Long periodo_id;

    @Column(name = "titulo")
    public String titulo;

    @Column(name = "descripcion")
    public String descripcion;

    @Column(name = "fecha_inicio_actividades")
    public LocalDate fecha_inicio_actividades;

    @Column(name = "fecha_fin_actividades")
    public LocalDate fecha_fin_actividades;

    @Column(name = "fecha_cierre")
    public LocalDate fecha_cierre;

    @Column(name = "horas_requeridas_snapshot", precision = 6, scale = 2)
    public BigDecimal horas_requeridas_snapshot;

    @Column(name = "nota_minima_snapshot", precision = 4, scale = 2)
    public BigDecimal nota_minima_snapshot;

    @Column(name = "fecha_primera_publicacion")
    public OffsetDateTime fecha_primera_publicacion;

    @Column(name = "fecha_publicacion")
    public OffsetDateTime fecha_publicacion;

    @Column(name = "estado")
    public String estado;

    @Column(name = "creada_por_usuario_id")
    public Long creada_por_usuario_id;

    @Column(name = "resuelta_por_usuario_id")
    public Long resuelta_por_usuario_id;

    @Column(name = "fecha_resolucion")
    public OffsetDateTime fecha_resolucion;

    @Column(name = "motivo_decision")
    public String motivo_decision;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}

package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla practica.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "practica")
public class Practica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "estudiante_id")
    public Long estudiante_id;

    @Column(name = "carrera_id")
    public Long carrera_id;

    @Column(name = "empresa_id")
    public Long empresa_id;

    @Column(name = "convenio_id")
    public Long convenio_id;

    @Column(name = "tipo_practica_id")
    public Long tipo_practica_id;

    @Column(name = "periodo_id")
    public Long periodo_id;

    @Column(name = "docente_id")
    public Long docente_id;

    @Column(name = "tutor_empresarial_id")
    public Long tutor_empresarial_id;

    @Column(name = "estado")
    public String estado;

    @Column(name = "horas_requeridas_snapshot", precision = 6, scale = 2)
    public BigDecimal horas_requeridas_snapshot;

    @Column(name = "nota_minima_snapshot", precision = 4, scale = 2)
    public BigDecimal nota_minima_snapshot;

    @Column(name = "fecha_postulacion")
    public OffsetDateTime fecha_postulacion;

    @Column(name = "fecha_aprobacion")
    public OffsetDateTime fecha_aprobacion;

    @Column(name = "fecha_inicio")
    public LocalDate fecha_inicio;

    @Column(name = "fecha_fin")
    public LocalDate fecha_fin;

    @Column(name = "nota_final", precision = 4, scale = 2)
    public BigDecimal nota_final;

    @Column(name = "observaciones")
    public String observaciones;

    @Column(name = "creado_por")
    public Long creado_por;

    @Column(name = "version")
    public Long version;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}

package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla postulacion.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "postulacion")
public class Postulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "perfil_id")
    public Long perfil_id;

    @Column(name = "estudiante_id")
    public Long estudiante_id;

    @Column(name = "estado")
    public String estado;

    @Column(name = "mensaje")
    public String mensaje;

    @Column(name = "fecha_postulacion")
    public OffsetDateTime fecha_postulacion;

    @Column(name = "recomendacion_empresa")
    public String recomendacion_empresa;

    @Column(name = "observacion_empresa")
    public String observacion_empresa;

    @Column(name = "evaluada_por_empresa_usuario_id")
    public Long evaluada_por_empresa_usuario_id;

    @Column(name = "fecha_evaluacion_empresa")
    public OffsetDateTime fecha_evaluacion_empresa;

    @Column(name = "resuelta_por")
    public Long resuelta_por;

    @Column(name = "fecha_resolucion")
    public OffsetDateTime fecha_resolucion;

    @Column(name = "motivo_decision")
    public String motivo_decision;

    @Column(name = "cancelada_por")
    public Long cancelada_por;

    @Column(name = "fecha_cancelacion")
    public OffsetDateTime fecha_cancelacion;

    @Column(name = "version")
    public Long version;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}

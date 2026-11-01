package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla postulacion_recomendacion_historial.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "postulacion_recomendacion_historial")
public class PostulacionRecomendacionHistorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "postulacion_id")
    public Long postulacion_id;

    @Column(name = "recomendacion")
    public String recomendacion;

    @Column(name = "observacion")
    public String observacion;

    @Column(name = "empresa_usuario_id")
    public Long empresa_usuario_id;

    @Column(name = "usuario_id")
    public Long usuario_id;

    @Column(name = "fecha")
    public OffsetDateTime fecha;

}

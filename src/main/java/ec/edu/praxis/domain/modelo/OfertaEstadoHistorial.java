package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla oferta_estado_historial.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "oferta_estado_historial")
public class OfertaEstadoHistorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "oferta_id")
    public Long oferta_id;

    @Column(name = "estado_anterior")
    public String estado_anterior;

    @Column(name = "estado_nuevo")
    public String estado_nuevo;

    @Column(name = "usuario_id")
    public Long usuario_id;

    @Column(name = "es_sistema")
    public Boolean es_sistema;

    @Column(name = "motivo")
    public String motivo;

    @Column(name = "fecha")
    public OffsetDateTime fecha;

}

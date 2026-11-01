package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla oferta_perfil_cambio_historial.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "oferta_perfil_cambio_historial")
public class OfertaPerfilCambioHistorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "oferta_id")
    public Long oferta_id;

    @Column(name = "perfil_id")
    public Long perfil_id;

    @Column(name = "tabla")
    public String tabla;

    @Column(name = "operacion")
    public String operacion;

    @Column(name = "registro_id")
    public Long registro_id;

    @Column(name = "antes", columnDefinition = "jsonb")
    public String antes;

    @Column(name = "despues", columnDefinition = "jsonb")
    public String despues;

    @Column(name = "usuario_id")
    public Long usuario_id;

    @Column(name = "fecha")
    public OffsetDateTime fecha;

}

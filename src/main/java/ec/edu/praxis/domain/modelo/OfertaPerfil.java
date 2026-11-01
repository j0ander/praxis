package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla oferta_perfil.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "oferta_perfil")
public class OfertaPerfil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "oferta_id")
    public Long oferta_id;

    @Column(name = "nombre")
    public String nombre;

    @Column(name = "descripcion")
    public String descripcion;

    @Column(name = "plazas")
    public Integer plazas;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}

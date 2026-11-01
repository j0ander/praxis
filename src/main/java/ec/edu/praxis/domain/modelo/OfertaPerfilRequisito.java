package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla oferta_perfil_requisito.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "oferta_perfil_requisito")
public class OfertaPerfilRequisito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "perfil_id")
    public Long perfil_id;

    @Column(name = "descripcion")
    public String descripcion;

    @Column(name = "obligatorio")
    public Boolean obligatorio;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}

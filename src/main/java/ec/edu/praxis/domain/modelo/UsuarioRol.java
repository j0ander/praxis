package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.io.Serializable;
import java.util.Objects;

/** Modelo persistente correspondiente a la tabla usuario_rol.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "usuario_rol")
@IdClass(UsuarioRolId.class)
public class UsuarioRol {
    @Id
    @Column(name = "usuario_id")
    public Long usuario_id;

    @Id
    @Column(name = "rol_id")
    public Long rol_id;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

}

package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla usuario.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "correo")
    public String correo;

    @Column(name = "hash_password")
    public String hash_password;

    @Column(name = "nombres")
    public String nombres;

    @Column(name = "apellidos")
    public String apellidos;

    @Column(name = "numero_documento")
    public String numero_documento;

    @Column(name = "tipo_documento")
    public String tipo_documento;

    @Column(name = "telefono")
    public String telefono;

    @Column(name = "activo")
    public Boolean activo;

    @Column(name = "alcance_global")
    public Boolean alcance_global;

    @Column(name = "debe_cambiar_password")
    public Boolean debe_cambiar_password;

    @Column(name = "intentos_fallidos")
    public Integer intentos_fallidos;

    @Column(name = "bloqueado_hasta")
    public OffsetDateTime bloqueado_hasta;

    @Column(name = "ultimo_login")
    public OffsetDateTime ultimo_login;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}

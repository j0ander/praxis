package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla empresa.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "ruc")
    public String ruc;

    @Column(name = "razon_social")
    public String razon_social;

    @Column(name = "nombre_comercial")
    public String nombre_comercial;

    @Column(name = "direccion")
    public String direccion;

    @Column(name = "telefono")
    public String telefono;

    @Column(name = "correo")
    public String correo;

    @Column(name = "activa")
    public Boolean activa;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}

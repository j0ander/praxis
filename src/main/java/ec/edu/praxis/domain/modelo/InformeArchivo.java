package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;

/**
 * Modelo persistente correspondiente a la tabla informe_archivo.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "informe_archivo")
public class InformeArchivo {
    @Id
    @Column(name = "informe_id")
    public Long informe_id;

    @Column(name = "contenido", nullable = false)
    public byte[] contenido;

    @Column(name = "tamano_bytes")
    public Integer tamano_bytes;

    @Column(name = "sha256")
    public String sha256;

}

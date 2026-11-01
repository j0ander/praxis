
package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Modelo persistente correspondiente a la tabla facultad.
 */
@Entity
@Table(name = "facultad")
public class Facultad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "codigo")
    public String codigo;

    @Column(name = "nombre")
    public String nombre;

    @Column(name = "activa")
    public Boolean activa;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime created_at;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updated_at;

    @PrePersist
    protected void antesDePersistir() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

        if (created_at == null) {
            created_at = ahora;
        }

        if (updated_at == null) {
            updated_at = ahora;
        }
    }

    @PreUpdate
    protected void antesDeActualizar() {
        updated_at = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

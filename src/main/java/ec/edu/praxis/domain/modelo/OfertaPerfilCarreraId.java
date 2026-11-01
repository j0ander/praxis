package ec.edu.praxis.domain.modelo;

import java.io.Serializable;
import java.util.Objects;

public class OfertaPerfilCarreraId implements Serializable {
    public Long perfil_id;
    public Long carrera_id;

    public OfertaPerfilCarreraId() {}

    public OfertaPerfilCarreraId(Long perfil_id, Long carrera_id) {
        this.perfil_id = perfil_id; this.carrera_id = carrera_id;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfertaPerfilCarreraId that = (OfertaPerfilCarreraId) o;
        return Objects.equals(perfil_id, that.perfil_id) && Objects.equals(carrera_id, that.carrera_id);
    }

    @Override public int hashCode() {
        return Objects.hash(perfil_id, carrera_id);
    }
}

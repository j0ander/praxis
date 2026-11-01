package ec.edu.praxis.domain.modelo;

import java.io.Serializable;
import java.util.Objects;

public class UsuarioCarreraId implements Serializable {
    public Long usuario_id;
    public Long carrera_id;

    public UsuarioCarreraId() {}

    public UsuarioCarreraId(Long usuario_id, Long carrera_id) {
        this.usuario_id = usuario_id; this.carrera_id = carrera_id;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioCarreraId that = (UsuarioCarreraId) o;
        return Objects.equals(usuario_id, that.usuario_id) && Objects.equals(carrera_id, that.carrera_id);
    }

    @Override public int hashCode() {
        return Objects.hash(usuario_id, carrera_id);
    }
}

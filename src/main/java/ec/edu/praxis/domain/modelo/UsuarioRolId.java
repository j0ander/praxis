package ec.edu.praxis.domain.modelo;

import java.io.Serializable;
import java.util.Objects;

public class UsuarioRolId implements Serializable {
    public Long usuario_id;
    public Long rol_id;

    public UsuarioRolId() {}

    public UsuarioRolId(Long usuario_id, Long rol_id) {
        this.usuario_id = usuario_id; this.rol_id = rol_id;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioRolId that = (UsuarioRolId) o;
        return Objects.equals(usuario_id, that.usuario_id) && Objects.equals(rol_id, that.rol_id);
    }

    @Override public int hashCode() {
        return Objects.hash(usuario_id, rol_id);
    }
}

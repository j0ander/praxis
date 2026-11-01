package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.UsuarioCarrera;
import ec.edu.praxis.domain.modelo.UsuarioCarreraId;
import java.util.List;

public interface RepositorioUsuarioCarrera extends RepositorioBase<UsuarioCarrera, UsuarioCarreraId> {
    List<UsuarioCarrera> listarPorUsuario(Long usuarioId);
    void eliminarPorUsuario(Long usuarioId);
}

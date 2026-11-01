package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.UsuarioRol;
import ec.edu.praxis.domain.modelo.UsuarioRolId;
import java.util.List;

public interface RepositorioUsuarioRol extends RepositorioBase<UsuarioRol, UsuarioRolId> {
    List<UsuarioRol> listarPorUsuario(Long usuarioId);
    List<UsuarioRol> listarPorRol(Long rolId);
    void eliminarPorUsuario(Long usuarioId);
}

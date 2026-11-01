package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Rol;
import java.util.List;
import java.util.Optional;

public interface RepositorioRol extends RepositorioBase<Rol, Long> {
    Optional<Rol> buscarPorCodigo(String codigo);
    List<Rol> listarTodos();
}

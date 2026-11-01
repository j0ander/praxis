package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Usuario;
import java.util.List;
import java.util.Optional;

public interface RepositorioUsuario extends RepositorioBase<Usuario, Long> {
    Optional<Usuario> buscarPorCorreo(String correo);
    boolean existeCorreo(String correo);
    List<Usuario> listarTodos();
}

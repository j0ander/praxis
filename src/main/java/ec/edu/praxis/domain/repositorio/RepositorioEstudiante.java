package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Estudiante;
import java.util.List;

public interface RepositorioEstudiante extends RepositorioBase<Estudiante, Long> {
    List<Estudiante> listarTodos();
    boolean existePorUsuario(Long usuarioId);
    java.util.Optional<Estudiante> buscarPorUsuario(Long usuarioId);
}

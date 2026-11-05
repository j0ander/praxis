package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Docente;
import java.util.List;

public interface RepositorioDocente extends RepositorioBase<Docente, Long> {
    List<Docente> listarTodos();
}

package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Carrera;
import java.util.List;

public interface RepositorioCarrera extends RepositorioBase<Carrera, Long> {
    List<Carrera> listarTodos();
}

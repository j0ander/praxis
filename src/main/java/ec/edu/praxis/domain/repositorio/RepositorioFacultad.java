package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Facultad;
import java.util.List;

public interface RepositorioFacultad extends RepositorioBase<Facultad, Long> {
    List<Facultad> listarTodos();
}

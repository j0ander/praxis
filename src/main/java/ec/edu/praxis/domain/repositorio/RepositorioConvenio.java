package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Convenio;
import java.util.List;

public interface RepositorioConvenio extends RepositorioBase<Convenio, Long> {
    List<Convenio> listarTodos();
    boolean existeNumero(String numero);
}

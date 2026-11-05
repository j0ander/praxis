package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.TipoPractica;
import java.util.List;

public interface RepositorioTipoPractica extends RepositorioBase<TipoPractica, Long> {
    List<TipoPractica> listarTodos();
}

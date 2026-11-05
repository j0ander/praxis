package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPractica;
import java.util.List;

public interface RepositorioOfertaPractica extends RepositorioBase<OfertaPractica, Long> {
    List<OfertaPractica> listarTodas();
}

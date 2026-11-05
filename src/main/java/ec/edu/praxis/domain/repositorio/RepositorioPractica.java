package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Practica;
import java.util.List;

public interface RepositorioPractica extends RepositorioBase<Practica, Long> {
    List<Practica> listarAprobadasSinDocente();
}

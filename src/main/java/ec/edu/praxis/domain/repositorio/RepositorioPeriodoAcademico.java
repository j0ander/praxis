package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.PeriodoAcademico;
import java.util.List;
import java.util.Optional;

public interface RepositorioPeriodoAcademico extends RepositorioBase<PeriodoAcademico, Long> {
    List<PeriodoAcademico> listarTodos();
    Optional<PeriodoAcademico> buscarActivo();
    void desactivarTodos();
}

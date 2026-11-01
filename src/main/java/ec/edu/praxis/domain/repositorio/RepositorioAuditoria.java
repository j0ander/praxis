package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Auditoria;
import java.util.List;

public interface RepositorioAuditoria extends RepositorioBase<Auditoria, Long> {
    List<Auditoria> listarRecientes(int limite);
}

package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfilRequisito;
import java.util.List;

public interface RepositorioOfertaPerfilRequisito extends RepositorioBase<OfertaPerfilRequisito, Long> {
    List<OfertaPerfilRequisito> listarPorPerfil(Long perfilId);
}

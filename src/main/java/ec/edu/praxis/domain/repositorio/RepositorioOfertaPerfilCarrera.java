package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfilCarrera;
import ec.edu.praxis.domain.modelo.OfertaPerfilCarreraId;

public interface RepositorioOfertaPerfilCarrera extends RepositorioBase<OfertaPerfilCarrera, OfertaPerfilCarreraId> {
    java.util.List<OfertaPerfilCarrera> listarPorPerfil(Long perfilId);
}

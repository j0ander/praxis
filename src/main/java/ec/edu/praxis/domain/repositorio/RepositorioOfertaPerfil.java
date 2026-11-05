package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfil;
import java.util.List;

public interface RepositorioOfertaPerfil extends RepositorioBase<OfertaPerfil, Long> {
    List<OfertaPerfil> listarPorOferta(Long ofertaId);
}

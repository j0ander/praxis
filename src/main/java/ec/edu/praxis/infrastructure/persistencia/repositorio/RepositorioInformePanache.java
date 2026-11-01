package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Informe;
import ec.edu.praxis.domain.repositorio.RepositorioInforme;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para informe; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioInformePanache implements RepositorioInforme, PanacheRepositoryBase<Informe, Long> {
    @Override public Optional<Informe> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Informe entidad) { persist(entidad); }
    @Override public void eliminar(Informe entidad) { delete(entidad); }
}

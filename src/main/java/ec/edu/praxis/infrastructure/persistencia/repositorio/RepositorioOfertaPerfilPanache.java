package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfil;
import ec.edu.praxis.domain.repositorio.RepositorioOfertaPerfil;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para oferta_perfil; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioOfertaPerfilPanache implements RepositorioOfertaPerfil, PanacheRepositoryBase<OfertaPerfil, Long> {
    @Override public Optional<OfertaPerfil> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(OfertaPerfil entidad) { persist(entidad); }
    @Override public void eliminar(OfertaPerfil entidad) { delete(entidad); }
}

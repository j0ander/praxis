package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfil;
import ec.edu.praxis.domain.repositorio.RepositorioOfertaPerfil;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioOfertaPerfilPanache implements RepositorioOfertaPerfil, PanacheRepositoryBase<OfertaPerfil, Long> {
    @Override public Optional<OfertaPerfil> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(OfertaPerfil entidad) { persist(entidad); }
    @Override public void eliminar(OfertaPerfil entidad) { delete(entidad); }
    @Override public List<OfertaPerfil> listarPorOferta(Long ofertaId) { return find("oferta_id", ofertaId).list(); }
}

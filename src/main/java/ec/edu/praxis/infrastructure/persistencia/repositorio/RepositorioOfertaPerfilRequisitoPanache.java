package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfilRequisito;
import ec.edu.praxis.domain.repositorio.RepositorioOfertaPerfilRequisito;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.List;

@ApplicationScoped
public class RepositorioOfertaPerfilRequisitoPanache implements RepositorioOfertaPerfilRequisito, PanacheRepositoryBase<OfertaPerfilRequisito, Long> {
    @Override public Optional<OfertaPerfilRequisito> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(OfertaPerfilRequisito entidad) { persist(entidad); }
    @Override public void eliminar(OfertaPerfilRequisito entidad) { delete(entidad); }
    @Override public List<OfertaPerfilRequisito> listarPorPerfil(Long perfilId) { return find("perfil_id", perfilId).list(); }
}

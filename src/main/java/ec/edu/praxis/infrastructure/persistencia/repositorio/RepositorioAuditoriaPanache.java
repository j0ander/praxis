package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Auditoria;
import ec.edu.praxis.domain.repositorio.RepositorioAuditoria;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioAuditoriaPanache implements RepositorioAuditoria, PanacheRepositoryBase<Auditoria, Long> {
    @Override public Optional<Auditoria> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Auditoria entidad) { persist(entidad); }
    @Override public void eliminar(Auditoria entidad) { throw new UnsupportedOperationException("La auditoría es inmutable."); }
    @Override public List<Auditoria> listarRecientes(int limite) { return findAll(Sort.by("fecha").descending()).range(0, Math.max(0, limite - 1)).list(); }
}

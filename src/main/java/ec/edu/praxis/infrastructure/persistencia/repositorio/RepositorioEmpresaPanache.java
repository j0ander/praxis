package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Empresa;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresa;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para empresa; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioEmpresaPanache implements RepositorioEmpresa, PanacheRepositoryBase<Empresa, Long> {
    @Override public Optional<Empresa> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Empresa entidad) { persist(entidad); }
    @Override public void eliminar(Empresa entidad) { delete(entidad); }
}

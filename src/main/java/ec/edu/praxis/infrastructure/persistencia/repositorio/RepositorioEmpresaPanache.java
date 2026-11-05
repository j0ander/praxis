package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Empresa;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresa;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioEmpresaPanache implements RepositorioEmpresa, PanacheRepositoryBase<Empresa, Long> {
    @Override public Optional<Empresa> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Empresa entidad) { persist(entidad); }
    @Override public void eliminar(Empresa entidad) { delete(entidad); }
    @Override public List<Empresa> listarTodas() { return findAll(Sort.by("razon_social")).list(); }
    @Override public boolean existeRuc(String ruc) { return count("ruc", ruc) > 0; }
}

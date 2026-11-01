package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.EmpresaUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresaUsuario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para empresa_usuario; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioEmpresaUsuarioPanache implements RepositorioEmpresaUsuario, PanacheRepositoryBase<EmpresaUsuario, Long> {
    @Override public Optional<EmpresaUsuario> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(EmpresaUsuario entidad) { persist(entidad); }
    @Override public void eliminar(EmpresaUsuario entidad) { delete(entidad); }
}

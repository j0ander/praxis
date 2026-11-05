package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.EmpresaUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresaUsuario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioEmpresaUsuarioPanache
        implements RepositorioEmpresaUsuario, PanacheRepositoryBase<EmpresaUsuario, Long> {

    @Override
    public Optional<EmpresaUsuario> buscarPorId(Long id) {
        return findByIdOptional(id);
    }

    @Override
    public void guardar(EmpresaUsuario entidad) {
        persist(entidad);
    }

    @Override
    public void eliminar(EmpresaUsuario entidad) {
        delete(entidad);
    }

    @Override
    public List<EmpresaUsuario> listarTodas() {
        return findAll(io.quarkus.panache.common.Sort.by("id")).list();
    }

    @Override
    public Optional<EmpresaUsuario> buscarPorEmpresaYUsuario(Long empresaId, Long usuarioId) {
        return find("empresa_id = ?1 and usuario_id = ?2", empresaId, usuarioId).firstResultOptional();
    }

    @Override
    public boolean existeMiembroActivo(Long empresaId) {
        return count("empresa_id = ?1 and activo = true", empresaId) > 0;
    }

    @Override
    public boolean existeOtroAdministradorActivo(Long empresaId, Long excluirId) {
        return count("empresa_id = ?1 and activo = true and rol_empresa = 'ADMIN_EMPRESA' and id <> ?2",
                empresaId, excluirId) > 0;
    }
}

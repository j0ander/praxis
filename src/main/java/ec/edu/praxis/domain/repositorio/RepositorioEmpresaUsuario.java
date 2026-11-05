package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.EmpresaUsuario;
import java.util.List;
import java.util.Optional;

public interface RepositorioEmpresaUsuario extends RepositorioBase<EmpresaUsuario, Long> {
    List<EmpresaUsuario> listarTodas();
    Optional<EmpresaUsuario> buscarPorEmpresaYUsuario(Long empresaId, Long usuarioId);
    boolean existeMiembroActivo(Long empresaId);
    boolean existeOtroAdministradorActivo(Long empresaId, Long excluirId);
}

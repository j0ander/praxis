package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Empresa;
import java.util.List;

public interface RepositorioEmpresa extends RepositorioBase<Empresa, Long> {
    List<Empresa> listarTodas();
    boolean existeRuc(String ruc);
}

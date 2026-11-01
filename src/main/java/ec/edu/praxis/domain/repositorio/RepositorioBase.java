package ec.edu.praxis.domain.repositorio;

import java.util.Optional;

/** Contrato de persistencia independiente de la tecnología utilizada. */
public interface RepositorioBase<T, ID> {
    Optional<T> buscarPorId(ID id);
    void guardar(T entidad);
    void eliminar(T entidad);
}

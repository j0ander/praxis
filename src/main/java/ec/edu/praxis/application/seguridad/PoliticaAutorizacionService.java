
package ec.edu.praxis.application.seguridad;

import ec.edu.praxis.domain.modelo.Usuario;
import ec.edu.praxis.domain.modelo.UsuarioRol;
import ec.edu.praxis.domain.modelo.UsuarioCarrera;
import ec.edu.praxis.domain.repositorio.RepositorioUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioRol;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioRol;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioCarrera;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class PoliticaAutorizacionService {

    @Inject
    RepositorioUsuario usuarios;

    @Inject
    RepositorioRol roles;

    @Inject
    RepositorioUsuarioRol usuariosRoles;

    @Inject
    RepositorioUsuarioCarrera usuariosCarreras;

    // Comprueba que el usuario exista y esté activo.
    public boolean usuarioActivo(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }

        Usuario usuario = usuarios.buscarPorId(usuarioId).orElse(null);

        return usuario != null && Boolean.TRUE.equals(usuario.activo);
    }

    // Comprueba si el usuario tiene asignado un rol.
    public boolean tieneRol(Long usuarioId, String codigoRol) {
        if (!usuarioActivo(usuarioId) || codigoRol == null) {
            return false;
        }

        List<UsuarioRol> asignaciones =
                usuariosRoles.listarPorUsuario(usuarioId);

        for (UsuarioRol asignacion : asignaciones) {
            var rol = roles.buscarPorId(asignacion.rol_id).orElse(null);

            if (rol != null && codigoRol.equalsIgnoreCase(rol.codigo)) {
                return true;
            }
        }

        return false;
    }

    // Comprueba si el usuario tiene alcance global.
    public boolean tieneAlcanceGlobal(Long usuarioId) {
        if (!usuarioActivo(usuarioId)) {
            return false;
        }

        Usuario usuario = usuarios.buscarPorId(usuarioId).orElse(null);

        return usuario != null
                && Boolean.TRUE.equals(usuario.alcance_global);
    }

    // Comprueba si el usuario puede acceder a una carrera.
    public boolean tieneAccesoCarrera(Long usuarioId, Long carreraId) {
        if (!usuarioActivo(usuarioId) || carreraId == null) {
            return false;
        }

        if (tieneAlcanceGlobal(usuarioId)) {
            return true;
        }

        List<UsuarioCarrera> asignaciones =
                usuariosCarreras.listarPorUsuario(usuarioId);

        for (UsuarioCarrera asignacion : asignaciones) {
            if (carreraId.equals(asignacion.carrera_id)) {
                return true;
            }
        }

        return false;
    }
}

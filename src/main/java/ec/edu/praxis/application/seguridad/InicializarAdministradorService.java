package ec.edu.praxis.application.seguridad;

import ec.edu.praxis.domain.modelo.Auditoria;
import ec.edu.praxis.domain.modelo.Rol;
import ec.edu.praxis.domain.modelo.Usuario;
import ec.edu.praxis.domain.modelo.UsuarioRol;
import ec.edu.praxis.domain.repositorio.RepositorioAuditoria;
import ec.edu.praxis.domain.repositorio.RepositorioRol;
import ec.edu.praxis.domain.repositorio.RepositorioUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioRol;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class InicializarAdministradorService {
    private static final Logger LOG = Logger.getLogger(InicializarAdministradorService.class);
    @Inject
    RepositorioUsuario usuarios;
    @Inject
    RepositorioRol roles;
    @Inject
    RepositorioUsuarioRol usuariosRoles;
    @Inject
    RepositorioAuditoria auditoria;
    @ConfigProperty(name = "praxis.admin.correo")
    Optional<String> correo;
    @ConfigProperty(name = "praxis.admin.password")
    Optional<String> password;

    @Transactional
    void iniciar(@Observes StartupEvent evento) {
        inicializar();
    }

    void inicializar() {
        Rol rolAdmin = roles.buscarPorCodigo("ADMINISTRADOR").orElse(null);
        if (rolAdmin == null) {
            LOG.error("No existe el rol ADMINISTRADOR en los catálogos.");
            return;
        }
        if (!usuariosRoles.listarPorRol(rolAdmin.id).isEmpty())
            return;
        String correoConfigurado = correo.orElse("").trim();
        String passwordConfigurada = password.orElse("");
        if (correoConfigurado.isBlank() || passwordConfigurada.length() < 12) {
            LOG.warn(
                    "No existe administrador inicial. Configure ADMIN_CORREO y ADMIN_PASSWORD (mínimo 12 caracteres).");
            return;
        }
        String normalizado = correoConfigurado.toLowerCase();
        if (usuarios.existeCorreo(normalizado)) {
            LOG.error(
                    "El correo configurado ya existe, pero no tiene rol ADMINISTRADOR. Revise la asignación manualmente.");
            return;
        }
        Usuario usuario = new Usuario();
        usuario.correo = normalizado;
        usuario.hash_password = BcryptUtil.bcryptHash(passwordConfigurada);
        usuario.nombres = "Administrador";
        usuario.apellidos = "Inicial";
        usuario.tipo_documento = "CEDULA";
        usuario.debe_cambiar_password = true;
        usuario.alcance_global = true;
        usuario.activo = true;
        usuario.intentos_fallidos = 0;
        OffsetDateTime ahora = OffsetDateTime.now();
        usuario.created_at = ahora;
        usuario.updated_at = ahora;
        usuarios.guardar(usuario);

        UsuarioRol asignacion = new UsuarioRol();
        asignacion.usuario_id = usuario.id;
        asignacion.rol_id = rolAdmin.id;
        asignacion.created_at = ahora;

        usuariosRoles.guardar(asignacion);
        Auditoria registro = new Auditoria();
        registro.usuario_id = usuario.id;
        registro.accion = "CREAR_ADMIN_INICIAL";
        registro.entidad = "usuario";
        registro.entidad_id = usuario.id;
        registro.detalle = Map.of();
        auditoria.guardar(registro);
        LOG.info("Administrador inicial creado. Debe cambiar la contraseña en el primer acceso.");
    }
}

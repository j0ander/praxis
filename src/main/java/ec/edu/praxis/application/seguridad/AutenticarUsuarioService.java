package ec.edu.praxis.application.seguridad;

import ec.edu.praxis.application.excepcion.NoAutenticadoException;
import ec.edu.praxis.domain.modelo.Usuario;
import ec.edu.praxis.domain.modelo.UsuarioRol;
import ec.edu.praxis.domain.modelo.Rol;
import ec.edu.praxis.domain.repositorio.RepositorioUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioRol;
import ec.edu.praxis.domain.repositorio.RepositorioRol;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class AutenticarUsuarioService {
    private static final String HASH_COMPARACION = BcryptUtil.bcryptHash("praxis-comparacion-interna-no-utilizable");

    @Inject RepositorioUsuario usuarios;
    @Inject RepositorioUsuarioRol usuariosRoles;
    @Inject RepositorioRol rolesRepositorio;

    @ConfigProperty(name = "praxis.jwt.duracion-segundos", defaultValue = "3600")
    long duracion;
    @ConfigProperty(name = "praxis.login.max-intentos", defaultValue = "5")
    int maxIntentos;
    @ConfigProperty(name = "praxis.login.bloqueo-minutos", defaultValue = "15")
    int bloqueoMinutos;

    public record LoginRequest(String correo, String password) {}
    public record LoginResponse(String token, long id, String correo, String nombres, String apellidos,
            List<String> roles, boolean debeCambiarPassword, boolean alcanceGlobal) {}

    @Transactional(dontRollbackOn = NoAutenticadoException.class)
    public LoginResponse login(LoginRequest entrada) {
        String correo = entrada == null || entrada.correo() == null ? ""
                : entrada.correo().trim().toLowerCase(Locale.ROOT);
        String password = entrada == null || entrada.password() == null ? "" : entrada.password();
        Usuario usuario = usuarios.buscarPorCorreo(correo).orElse(null);
        if (usuario == null) {
            BcryptUtil.matches(password, HASH_COMPARACION);
            throw new NoAutenticadoException();
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        boolean bloqueado = usuario.bloqueado_hasta != null && usuario.bloqueado_hasta.isAfter(ahora);
        boolean passwordValido = BcryptUtil.matches(password, usuario.hash_password);
        if (!Boolean.TRUE.equals(usuario.activo) || bloqueado || !passwordValido) {
            int intentos = (usuario.intentos_fallidos == null ? 0 : usuario.intentos_fallidos) + 1;
            if (intentos >= maxIntentos) {
                usuario.intentos_fallidos = 0;
                usuario.bloqueado_hasta = ahora.plusMinutes(bloqueoMinutos);
            } else {
                usuario.intentos_fallidos = intentos;
            }
            usuarios.guardar(usuario);
            throw new NoAutenticadoException();
        }

        usuario.intentos_fallidos = 0;
        usuario.bloqueado_hasta = null;
        usuario.ultimo_login = ahora;
        usuarios.guardar(usuario);

        List<String> codigosRoles = usuariosRoles.listarPorUsuario(usuario.id).stream()
                .map(r -> r.rol_id)
                .map(rolesRepositorio::buscarPorId)
                .flatMap(Optional::stream)
                .map(r -> r.codigo)
                .sorted()
                .toList();

        String token = Jwt.issuer("praxis").subject(usuario.correo).upn(usuario.correo)
                .groups(new HashSet<>(codigosRoles))
                .claim("usuarioId", usuario.id)
                .claim("alcanceGlobal", Boolean.TRUE.equals(usuario.alcance_global))
                .expiresIn(duracion).sign();

        return new LoginResponse(token, usuario.id, usuario.correo, usuario.nombres, usuario.apellidos,
                codigosRoles, Boolean.TRUE.equals(usuario.debe_cambiar_password),
                Boolean.TRUE.equals(usuario.alcance_global));
    }
}

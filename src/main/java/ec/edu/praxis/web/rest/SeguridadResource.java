package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.seguridad.AutenticarUsuarioService;
import ec.edu.praxis.application.seguridad.AdministrarSeguridadService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.security.Authenticated;
import java.util.Map;
import jakarta.json.JsonNumber;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SeguridadResource {
    @Inject
    AutenticarUsuarioService autenticar;
    @Inject
    AdministrarSeguridadService administrar;
    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    @PermitAll
    public AutenticarUsuarioService.LoginResponse login(AutenticarUsuarioService.LoginRequest request) {
        return autenticar.login(request);
    }

    @GET
    @Path("/me")
    @Authenticated
    public Map<String, Object> me() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");

        return Map.of(
                "correo", jwt.getName(),
                "roles", jwt.getGroups(),
                "usuarioId", usuarioId.longValue(),
                "alcanceGlobal", Boolean.TRUE.equals(jwt.getClaim("alcanceGlobal")));
    }

    public record CambioPassword(String actual, String nueva) {
    }

    @POST
    @Path("/cambiar-password")
    @Authenticated
    public Map<String, String> cambiarPassword(CambioPassword d) {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        long id = usuarioId.longValue();

        administrar.cambiarPassword(id, d.actual(), d.nueva());

        return Map.of("mensaje", "Contraseña actualizada correctamente.");
    }
}

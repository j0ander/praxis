package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.oferta.GestionarPostulacionEstudianteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

/** Endpoints para crear y consultar las postulaciones del estudiante autenticado. */
@Path("/estudiante/postulaciones")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ESTUDIANTE")
public class PostulacionEstudianteResource {

    @Inject GestionarPostulacionEstudianteService service;
    @Inject JsonWebToken jwt;

    private long actor() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) {
            throw new IllegalStateException("El token no contiene el identificador del usuario.");
        }
        return usuarioId.longValue();
    }

    @GET
    public List<Map<String, Object>> listarPropias() {
        return service.listarPropias(actor());
    }

    @POST
    public Map<String, Object> crear(Map<String, Object> datos) {
        return service.crear(datos, actor());
    }
}

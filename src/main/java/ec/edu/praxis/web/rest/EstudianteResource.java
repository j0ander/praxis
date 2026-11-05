package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.estudiante.GestionarEstudianteService;
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

@Path("/admin/estudiantes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMINISTRADOR", "COORDINADOR"})
public class EstudianteResource {

    @Inject GestionarEstudianteService service;
    @Inject JsonWebToken jwt;

    private long actor() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) throw new IllegalStateException("El token no contiene el identificador del usuario.");
        return usuarioId.longValue();
    }

    @GET
    public List<Map<String, Object>> listar() {
        return service.listarEstudiantes();
    }

    @GET
    @Path("/opciones")
    public Map<String, Object> opciones() {
        return service.opciones();
    }

    @POST
    public Map<String, Object> crear(Map<String, Object> datos) {
        long id = service.crearEstudiante(datos, actor());
        return Map.of("id", id, "mensaje", "Perfil de estudiante registrado correctamente.");
    }
}

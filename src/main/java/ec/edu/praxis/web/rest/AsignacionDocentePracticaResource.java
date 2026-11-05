package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.practica.AsignarDocentePracticaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

/** Endpoints de asignación de docente tutor por coordinación. */
@Path("/coordinacion/practicas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("COORDINADOR")
public class AsignacionDocentePracticaResource {
    @Inject AsignarDocentePracticaService service;
    @Inject JsonWebToken jwt;

    private long actor() {
        JsonNumber usuarioId = jwt.getClaim("usuarioId");
        if (usuarioId == null) throw new IllegalStateException("El token no contiene el identificador del usuario.");
        return usuarioId.longValue();
    }

    @GET
    @Path("/pendientes-docente")
    public List<Map<String, Object>> listarPendientes() {
        return service.listarPendientes(actor());
    }

    @GET
    @Path("/{id}/docentes-disponibles")
    public List<Map<String, Object>> docentesDisponibles(@PathParam("id") long id) {
        return service.listarDocentesDisponibles(id, actor());
    }

    @PATCH
    @Path("/{id}/docente")
    public Map<String, Object> asignar(@PathParam("id") long id, Map<String, Number> datos) {
        Long docenteId = datos == null || datos.get("docenteId") == null
                ? null : datos.get("docenteId").longValue();
        return service.asignar(id, docenteId, actor());
    }
}

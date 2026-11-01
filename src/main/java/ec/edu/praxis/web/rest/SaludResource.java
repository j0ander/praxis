package ec.edu.praxis.web.rest;

import ec.edu.praxis.application.salud.ConsultarSaludService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** GET /api/salud: comprueba la aplicación, la base de datos y la versión del esquema. */
@Path("/salud")
@Produces(MediaType.APPLICATION_JSON)
public class SaludResource {
    @Inject ConsultarSaludService servicio;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "dev")
    String version;

    @GET
    public Map<String, Object> estado() throws Exception {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("aplicacion", "praxis");
        respuesta.put("version", version);
        respuesta.put("hora", OffsetDateTime.now());
        respuesta.put("baseDatos", "OK");
        respuesta.put("versionEsquema", servicio.versionEsquema());
        return respuesta;
    }
}

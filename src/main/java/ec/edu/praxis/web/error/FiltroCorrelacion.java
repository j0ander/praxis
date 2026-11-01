package ec.edu.praxis.web.error;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.util.UUID;

/**
 * Asigna un id de correlación a cada petición: sale en la cabecera, en los logs
 * y en los errores.
 */
@Provider
public class FiltroCorrelacion implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String CABECERA = "X-Correlation-Id";
    public static final String PROPIEDAD = "praxis.idCorrelacion";

    @Override
    public void filter(ContainerRequestContext req) {
        String id = req.getHeaderString(CABECERA);
        if (id == null || id.isBlank() || id.length() > 64) {
            id = UUID.randomUUID().toString();
        }
        req.setProperty(PROPIEDAD, id);
        MDC.put("idCorrelacion", id);
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        Object id = req.getProperty(PROPIEDAD);
        if (id != null) {
            res.getHeaders().putSingle(CABECERA, id.toString());
        }
        MDC.remove("idCorrelacion");
    }

    public static String de(ContainerRequestContext req) {
        Object id = req.getProperty(PROPIEDAD);
        return id == null ? null : id.toString();
    }
}

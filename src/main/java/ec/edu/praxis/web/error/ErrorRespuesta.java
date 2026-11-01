package ec.edu.praxis.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/** Formato único de error de toda la API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(
        int estado,
        String codigo,
        String mensaje,
        List<Detalle> detalles,
        String idCorrelacion,
        OffsetDateTime marca) {

    public record Detalle(String campo, String mensaje) {
    }

    public static ErrorRespuesta de(int estado, String codigo, String mensaje, String idCorrelacion) {
        return new ErrorRespuesta(estado, codigo, mensaje, null, idCorrelacion, OffsetDateTime.now());
    }

    public static ErrorRespuesta de(int estado, String codigo, String mensaje,
            List<Detalle> detalles, String idCorrelacion) {
        return new ErrorRespuesta(estado, codigo, mensaje, detalles, idCorrelacion, OffsetDateTime.now());
    }
}

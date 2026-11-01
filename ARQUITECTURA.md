# PRAXIS — arquitectura Domain First

## Estructura

- `domain/modelo`: modelos JPA alineados con las tablas de V1, V2 y V3.
- `domain/repositorio`: contratos de persistencia independientes de Panache.
- `application`: casos de uso, servicios y puertos que necesita la aplicación.
- `infrastructure/persistencia/repositorio`: adaptadores Panache que implementan los contratos del dominio.
- `infrastructure/salud`: adaptador JDBC para comprobar la versión de Flyway.
- `web`: recursos REST y manejo de errores HTTP.

## Reglas de dependencia

1. `domain` no depende de `application`, `infrastructure` ni `web`.
2. `application` utiliza modelos y contratos; no importa `EntityManager`, Panache, JDBC ni ejecuta SQL.
3. `infrastructure` contiene las consultas y detalles técnicos de persistencia.
4. `web` recibe peticiones HTTP y delega los casos de uso; no accede directamente a la base de datos.
5. Las transacciones de los casos de uso se controlan en `application`.

## Persistencia y modelos

- Los nombres de tabla y columna corresponden a V1, V2 y V3. Las migraciones se conservaron sin cambios.
- Las claves foráneas se representan como identificadores escalares (`Long`) para evitar asociaciones JPA implícitas incorrectas, especialmente en claves compuestas.
- Las tablas de unión con clave primaria compuesta utilizan `@IdClass`.
- Las columnas JSONB se representan con `Map<String, Object>` y `@JdbcTypeCode(SqlTypes.JSON)`.
- Las fechas, marcas de tiempo, UUID y decimales usan tipos Java compatibles con el esquema.

## Funcionalidades migradas a modelos y repositorios

- Inicio de sesión, consulta de roles, bloqueo por intentos fallidos y actualización del último acceso.
- Creación y estado de usuarios, asignación de roles y carreras, cambio y restablecimiento de contraseñas.
- Creación y consulta de facultades, carreras y periodos académicos; activación de periodo y desactivación de catálogos.
- Registro y consulta de auditoría.
- Comprobación de salud: el recurso REST delega en un servicio y un adaptador de infraestructura consulta Flyway.

La lógica de aplicación ya no ejecuta SQL nativo ni accede a `EntityManager`. Las consultas y operaciones de persistencia se encuentran en los adaptadores de `infrastructure`.

## Correcciones preservadas

- `AdministracionResource.actor()` obtiene `usuarioId` como `jakarta.json.JsonNumber`.
- El administrador inicial se crea con `alcance_global = TRUE`, coherente con el rol global `ADMINISTRADOR`.
- Los intentos fallidos de inicio de sesión se confirman aunque la operación termine con `NoAutenticadoException`, para que el contador y el bloqueo no se reviertan.
- La auditoría se trata como inmutable y sus detalles se almacenan como JSONB.

## Validación

La entrega anterior compiló correctamente en el equipo del usuario. Esta nueva etapa modifica servicios y adaptadores; debe validarse localmente con:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd quarkus:dev
```

El proyecto todavía no contiene pruebas automatizadas de servicio (`No tests to run` en la ejecución anterior), por lo que `BUILD SUCCESS` confirma compilación, no todas las reglas funcionales. Las migraciones V1, V2 y V3 se conservaron sin cambios. No reiniciar ni eliminar la base de datos para aplicar esta reorganización.

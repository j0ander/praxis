# PRAXIS — Gestión de prácticas preprofesionales

**Prototipo funcional — Incrementos 0 y 1**

Java 21 · Quarkus 3.40.1 · React · Vite · Quinoa · PostgreSQL · Hibernate Panache · Flyway · JWT.

PRAXIS es una aplicación web para apoyar la gestión de prácticas preprofesionales universitarias. El proyecto está organizado por capas para separar la interfaz, las reglas de negocio, el dominio y el acceso a datos.

## 1. Funcionalidades actuales

### Incremento 0 — Base del sistema
- Estructura inicial del proyecto.
- Conexión con PostgreSQL.
- Migraciones de base de datos con Flyway.
- API REST y documentación con Swagger.
- Interfaz web con React y Vite.
- Manejo centralizado de errores.
- Endpoint de salud para comprobar el estado de la aplicación.

### Incremento 1 — Seguridad y estructura académica
- Inicio de sesión con correo y contraseña.
- Autenticación mediante JWT.
- Administración de usuarios.
- Asignación de roles y carreras.
- Administración de facultades.
- Administración de carreras.
- Administración de periodos académicos.
- Activación de periodos.
- Registro de eventos de auditoría.
- Primeros componentes de la política central de autorización.

## 2. Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje del backend |
| Quarkus 3.40.1 | Framework del backend |
| React | Interfaz de usuario |
| Vite | Desarrollo del frontend |
| Quinoa | Integración del frontend con Quarkus |
| PostgreSQL | Base de datos |
| Hibernate ORM Panache | Persistencia |
| Flyway | Migraciones de base de datos |
| JWT | Autenticación |
| Maven | Compilación y ejecución |

## 3. Requisitos

Para ejecutar el proyecto localmente se necesita:

- JDK 21.
- PostgreSQL 15 o superior.
- Git.
- Docker, para ejecutar las pruebas que utilizan Testcontainers.
- Visual Studio Code o IntelliJ IDEA (opcional).

El proyecto incluye Maven Wrapper, por lo que no es necesario instalar Maven manualmente. Quinoa se encarga de instalar la versión de Node.js configurada para el frontend.

## 4. Descargar el proyecto

Clonar el repositorio:

```bash
git clone URL_DEL_REPOSITORIO
```

Ingresar a la carpeta:

```bash
cd praxis
```

Abrir el proyecto en Visual Studio Code (opcional):

```bash
code .
```

## 5. Configurar PostgreSQL

Crear el usuario y la base de datos para el entorno local:

```sql
CREATE USER praxis WITH PASSWORD 'praxis';
CREATE DATABASE praxis OWNER praxis;
```

Configuración local predeterminada:

| Parámetro | Valor |
|---|---|
| Host | localhost |
| Puerto | 5432 |
| Base de datos | praxis |
| Usuario | praxis |
| Contraseña | praxis |

Estos valores son únicamente para desarrollo local. Si se utilizan otros datos de conexión, deben configurarse mediante las variables de entorno correspondientes.

Al iniciar la aplicación, Flyway ejecuta las migraciones disponibles para preparar la estructura de la base de datos.

## 6. Configurar las claves de seguridad

Para utilizar la autenticación JWT se necesita un par de claves RSA.

El proyecto incluye el archivo `GenerarClaves.java`, que permite generar las claves automáticamente.

Desde la raíz del proyecto, ejecutar:

```powershell
javac GenerarClaves.java
java GenerarClaves
```

Se crearán dos archivos:

- `privateKey.pem`: clave privada.
- `publicKey.pem`: clave pública.

**Importante:** cada integrante debe generar sus propias claves. No se deben subir archivos `.pem` al repositorio ni compartir la clave privada.

También se debe configurar el archivo `.env` a partir de `.env.example`, si está disponible, con los datos de conexión y las variables necesarias para crear el administrador inicial.

## 7. Ejecutar el proyecto

En Windows, desde la carpeta raíz:

```powershell
.\mvnw.cmd quarkus:dev
```

En Linux o macOS:

```bash
./mvnw quarkus:dev
```

Esperar a que Quarkus termine de iniciar. Durante el arranque se establece la conexión con PostgreSQL y se ejecutan las migraciones pendientes.

No cerrar la terminal mientras se utiliza la aplicación.

### Direcciones locales

| Servicio | Dirección |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/q/swagger-ui |
| Salud de la API | http://localhost:8080/api/salud |

Para detener el servidor, presionar `Ctrl + C` en la terminal.

## 8. Otros comandos

| Acción | Comando |
|---|---|
| Compilar el backend | `.\mvnw.cmd compile` |
| Ejecutar en desarrollo | `.\mvnw.cmd quarkus:dev` |
| Ejecutar pruebas y verificaciones | `.\mvnw.cmd verify` |
| Construir el proyecto | `.\mvnw.cmd package` |
| Instalar dependencias del frontend | `cd src/main/webui && npm install` |
| Ejecutar pruebas del frontend | `cd src/main/webui && npm test` |
| Construir imagen Docker | `docker build -t praxis .` |

Algunos comandos de pruebas y construcción requieren que Docker esté instalado y funcionando, según la configuración de Testcontainers.

## 9. Estructura del proyecto

```text
praxis/
├── src/
│   ├── main/
│   │   ├── java/ec/edu/praxis/
│   │   │   ├── domain/
│   │   │   │   ├── modelo/
│   │   │   │   └── repositorio/
│   │   │   ├── application/
│   │   │   ├── infrastructure/
│   │   │   │   └── persistencia/
│   │   │   └── web/
│   │   │       ├── rest/
│   │   │       ├── dto/
│   │   │       └── error/
│   │   ├── resources/
│   │   │   └── db/
│   │   │       ├── migration/
│   │   │       └── seed/
│   │   └── webui/
│   └── test/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

### Organización por capas

- **domain:** contiene los modelos del dominio y los contratos de los repositorios.
- **application:** contiene los servicios y las reglas de aplicación.
- **infrastructure:** implementa el acceso a datos y otros componentes técnicos.
- **web:** contiene los recursos REST, DTO y manejo de errores.
- **webui:** contiene la interfaz desarrollada con React.

La comunicación sigue la separación de responsabilidades: la capa web utiliza los servicios de aplicación, el dominio define los contratos y la infraestructura implementa la persistencia.

## 10. Base de datos y migraciones

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
```

Los archivos de migración se ejecutan en orden mediante Flyway.

**Las migraciones que ya fueron ejecutadas no deben modificarse.** Para agregar cambios a la base de datos se debe crear una nueva migración con la siguiente versión disponible.

Los datos de desarrollo se encuentran en:

```text
src/main/resources/db/seed/
```

Estos datos se utilizan en el perfil de desarrollo.

## 11. API principal

Las rutas de la API están bajo `/api`.

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/auth/login` | Público |
| GET | `/auth/me` | Autenticado |
| POST | `/auth/cambiar-password` | Autenticado |
| GET | `/admin/roles` | ADMINISTRADOR |
| GET / POST | `/admin/usuarios` | ADMINISTRADOR |
| PATCH | `/admin/usuarios/{id}/estado` | ADMINISTRADOR |
| PUT | `/admin/usuarios/{id}/roles` | ADMINISTRADOR |
| PUT | `/admin/usuarios/{id}/carreras` | ADMINISTRADOR |
| POST | `/admin/usuarios/{id}/restablecer-password` | ADMINISTRADOR |
| GET / POST | `/admin/facultades` | ADMINISTRADOR |
| PATCH | `/admin/facultades/{id}` | ADMINISTRADOR |
| GET / POST | `/admin/carreras` | ADMINISTRADOR |
| PATCH | `/admin/carreras/{id}` | ADMINISTRADOR |
| GET / POST | `/admin/periodos` | ADMINISTRADOR |
| POST | `/admin/periodos/{id}/activar` | ADMINISTRADOR |
| GET | `/admin/auditoria` | ADMINISTRADOR |

La documentación interactiva de los endpoints está disponible en Swagger UI cuando la aplicación se ejecuta en desarrollo.

## 12. Reglas de seguridad implementadas

- El inicio de sesión normaliza el correo electrónico.
- Las contraseñas se almacenan mediante hashes BCrypt.
- Se aplica bloqueo temporal después de alcanzar el umbral de intentos fallidos configurado.
- Los tokens JWT se firman con claves RSA.
- Los roles se transportan en el token como grupos.
- Los endpoints administrativos requieren el rol `ADMINISTRADOR`.
- Las contraseñas temporales exigen cambio.
- Las operaciones administrativas registran eventos de auditoría.
- La asignación de carreras y el alcance global se almacenan en la base de datos.

## 13. Limitaciones y trabajo pendiente

PRAXIS es un prototipo funcional en desarrollo, no una versión lista para producción.

Actualmente, la interfaz cubre el acceso y las operaciones iniciales de usuarios y estructura académica.

Quedan pendientes, entre otros aspectos:

- Incorporar visualmente el formulario de asignación detallada de carreras.
- Integrar la política de alcance en las consultas de los módulos académicos.
- Completar los flujos de gestión de prácticas preprofesionales.
- Ampliar las pruebas de integración y seguridad.
- Validar los umbrales de bloqueo y la duración de los tokens con la institución.

La política central de autorización cuenta con una primera implementación, pero todavía debe integrarse en los servicios y consultas de los distintos módulos.

## 14. Despliegue gratuito

El proyecto contempla la posibilidad de utilizar servicios gratuitos para publicar una demostración:

- **Neon:** alojamiento de PostgreSQL.
- **Render:** despliegue del backend.
- **GitHub Actions:** automatización de tareas y respaldos.

La configuración de despliegue utiliza variables de entorno y archivos secretos. No se deben incluir credenciales ni claves privadas directamente en el repositorio.

Para Render se contemplan las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`, además de los archivos secretos RSA configurados para el entorno de producción.

El despliegue depende de la disponibilidad y las limitaciones de los planes gratuitos de cada proveedor.

## 15. Trabajo en equipo

Para colaborar en el proyecto:

1. Clonar el repositorio.
2. Crear una rama para los cambios.
3. Realizar modificaciones respetando la estructura por capas.
4. No modificar migraciones que ya fueron ejecutadas.
5. Crear una nueva migración cuando sea necesario cambiar la base de datos.
6. Probar los cambios antes de realizar un `push`.
7. No subir contraseñas, archivos `.env` ni claves privadas.

---

**PRAXIS — Prototipo funcional de gestión de prácticas preprofesionales.**
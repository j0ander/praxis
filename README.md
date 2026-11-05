# PRAXIS — Sistema de Gestión de Prácticas Preprofesionales

## 1. Descripción general

**PRAXIS** es un sistema web diseñado para gestionar y controlar el proceso de prácticas preprofesionales de una institución universitaria.

Su objetivo principal es centralizar el proceso que conecta a las empresas, estudiantes y autoridades académicas, permitiendo administrar las ofertas de prácticas, las postulaciones, las aprobaciones y la asignación de docentes tutores.

El sistema busca garantizar que cada práctica tenga un origen identificado, un proceso de aprobación, un estudiante responsable y un seguimiento académico.

El flujo principal de PRAXIS es:

**Empresa → Oferta de práctica → Revisión del coordinador → Publicación → Postulación del estudiante → Aprobación → Generación de práctica individual → Asignación de docente tutor.**

Este proceso constituye el núcleo funcional del sistema.

---

## 2. Objetivo principal del sistema

El objetivo de PRAXIS es automatizar y organizar el ciclo institucional de las prácticas preprofesionales, evitando procesos manuales y permitiendo mantener un registro ordenado de cada operación.

Entre sus objetivos específicos se encuentran:

- Administrar usuarios y sus respectivos roles.
- Gestionar facultades, carreras y periodos académicos.
- Registrar empresas y sus representantes.
- Administrar convenios institucionales.
- Permitir que las empresas publiquen oportunidades de prácticas.
- Permitir que los coordinadores revisen y aprueben las ofertas.
- Permitir que los estudiantes consulten y soliciten oportunidades.
- Gestionar la aprobación o rechazo de postulaciones.
- Generar automáticamente una práctica individual cuando una postulación es aceptada.
- Asignar docentes tutores de acuerdo con la carrera y el alcance académico.
- Mantener trazabilidad de las operaciones y decisiones realizadas.

---

# 3. El flujo central de PRAXIS

El funcionamiento del sistema se basa en un proceso secuencial. Cada etapa depende de la anterior y tiene un responsable definido.

## 3.1. Administración y configuración institucional

Antes de iniciar el proceso de prácticas, el administrador configura la información necesaria para el funcionamiento del sistema.

Entre sus responsabilidades están:

- Crear y administrar usuarios.
- Asignar roles.
- Gestionar facultades.
- Gestionar carreras.
- Administrar periodos académicos.
- Registrar empresas.
- Registrar representantes empresariales.
- Gestionar convenios.
- Consultar auditoría del sistema.

Esta información constituye la base sobre la que funcionan los demás módulos.

## 3.2. Registro de empresas y convenios

Las empresas representan las organizaciones que ofrecen oportunidades de prácticas a los estudiantes.

Cada empresa puede tener representantes autorizados para ingresar al sistema y administrar sus ofertas.

Los convenios establecen la relación institucional entre la universidad y las empresas.

Para que una empresa pueda generar ofertas, debe cumplir las condiciones de actividad y contar con un convenio válido y vigente.

## 3.3. Creación de ofertas por parte de la empresa

El representante empresarial puede crear ofertas de prácticas desde su panel.

Una oferta contiene información como:

- Empresa.
- Convenio.
- Tipo de práctica.
- Periodo académico.
- Título de la oferta.
- Descripción.
- Fecha de inicio.
- Fecha de finalización.
- Fecha límite de postulación.
- Perfiles disponibles.
- Número de plazas.
- Carreras habilitadas.
- Requisitos del perfil.

Una oferta puede contener uno o varios perfiles, dependiendo de las necesidades de la empresa.

Cada perfil representa una oportunidad específica dentro de la oferta y puede establecer sus propias plazas y condiciones.

### Estado inicial

Cuando se registra una oferta, comienza en estado:

`BORRADOR`

El representante empresarial puede completar o modificar la información mientras las reglas del sistema lo permitan.

Cuando la oferta está lista, utiliza la opción **Enviar a revisión**.

La oferta cambia a:

`PENDIENTE_REVISION`

En este momento deja de ser una convocatoria directamente administrada por la empresa y pasa al proceso de revisión académica.

## 3.4. Revisión de ofertas por el coordinador

El coordinador es responsable de revisar las ofertas enviadas por las empresas.

Desde su panel puede consultar las ofertas pendientes y verificar:

- Información de la empresa.
- Convenio asociado.
- Tipo de práctica.
- Periodo académico.
- Fechas establecidas.
- Perfiles disponibles.
- Plazas.
- Carreras habilitadas.
- Requisitos.

El coordinador puede tomar dos decisiones.

**Aprobar la oferta**

La oferta cambia al estado:

`PUBLICADA`

A partir de ese momento puede ser consultada por los estudiantes que cumplan las condiciones establecidas.

**Rechazar la oferta**

La oferta cambia al estado:

`RECHAZADA`

El motivo del rechazo es obligatorio y queda registrado.

Esta etapa garantiza que las oportunidades empresariales sean revisadas antes de mostrarse a los estudiantes.

## 3.5. Consulta de ofertas por parte del estudiante

El estudiante ingresa a su panel y puede consultar las ofertas que se encuentran publicadas.

Puede visualizar información como:

- Empresa.
- Título de la oferta.
- Descripción.
- Periodo.
- Fechas.
- Perfiles disponibles.
- Requisitos.
- Plazas.

El estudiante puede revisar las oportunidades disponibles y seleccionar el perfil que corresponda a su formación académica.

Las ofertas que no han sido aprobadas por el coordinador no deben aparecer como oportunidades disponibles para postularse.

## 3.6. Postulación del estudiante

Cuando el estudiante encuentra una oportunidad adecuada, puede enviar su postulación.

La postulación queda relacionada con:

- El estudiante autenticado.
- La oferta seleccionada.
- El perfil al que desea postularse.

El estado inicial de la postulación es:

`PENDIENTE`

El sistema realiza validaciones para evitar solicitudes incorrectas.

Entre ellas:

- La oferta debe estar publicada.
- El plazo de postulación debe encontrarse vigente.
- La carrera del estudiante debe ser compatible con el perfil.
- El estudiante debe tener su información académica registrada.
- Deben existir plazas disponibles.
- No debe existir otra postulación activa del mismo estudiante para el mismo perfil.

El estudiante puede consultar sus postulaciones y conocer si continúan pendientes, fueron aceptadas o fueron rechazadas.

## 3.7. Revisión de postulaciones por el coordinador

Una vez que los estudiantes envían sus solicitudes, el coordinador puede consultar las postulaciones pendientes.

En esta etapa se revisa la información del estudiante y la oportunidad a la que se postuló.

El coordinador puede:

**Aceptar la postulación**

La postulación cambia a:

`ACEPTADA`

Como consecuencia de esta decisión, el sistema genera automáticamente una práctica individual para el estudiante.

**Rechazar la postulación**

La postulación cambia a:

`RECHAZADA`

El motivo del rechazo es obligatorio y queda registrado.

Esta decisión es fundamental porque determina qué estudiantes pasan oficialmente a la etapa de prácticas.

## 3.8. Generación automática de la práctica individual

Esta es una de las funcionalidades más importantes de PRAXIS.

Cuando el coordinador acepta una postulación, el sistema crea automáticamente un registro de práctica individual.

Es importante comprender la diferencia entre una oferta y una práctica:

| Concepto | Descripción |
|---|---|
| Oferta | Convocatoria publicada por una empresa. |
| Perfil | Oportunidad específica dentro de una oferta. |
| Postulación | Solicitud realizada por un estudiante. |
| Práctica | Registro individual generado para el estudiante cuya postulación fue aceptada. |

Una oferta puede recibir varias postulaciones, pero cada práctica corresponde a un estudiante específico.

La práctica generada conserva la información necesaria para su posterior seguimiento:

- Estudiante.
- Carrera.
- Empresa.
- Convenio.
- Tipo de práctica.
- Periodo académico.
- Postulación de origen.
- Fecha de inicio.
- Fecha de finalización.
- Horas requeridas.
- Nota mínima.
- Fecha de aprobación.

El estado inicial de la práctica es:

`APROBADA`

La aceptación de la postulación y la generación de la práctica se realizan dentro de una misma transacción, evitando que una solicitud quede aceptada sin que se genere su correspondiente práctica.

**Regla fundamental:** una práctica no se crea cuando el estudiante consulta una oferta ni cuando envía una postulación. Se crea únicamente cuando el coordinador acepta la postulación.

## 3.9. Asignación del docente tutor

Después de generar la práctica individual, el coordinador puede consultar las prácticas aprobadas que todavía no tienen un docente tutor asignado.

El sistema permite seleccionar docentes que cumplan las condiciones académicas correspondientes.

Para que un docente aparezca como disponible debe:

- Tener un usuario activo.
- Tener asignado el rol `DOCENTE`.
- Contar con un perfil docente registrado.
- Tener asignada la carrera correspondiente a la práctica o disponer de alcance académico global.
- Ser compatible con el alcance académico del coordinador que realiza la asignación.

El coordinador selecciona un docente y lo asigna a la práctica.

De esta manera, cada práctica puede quedar vinculada con el docente responsable de su tutoría académica.

Además, el sistema contempla la creación automática del perfil docente cuando se asigna el rol `DOCENTE` desde la administración de usuarios, así como la regularización de perfiles faltantes mediante la migración correspondiente.

---

# 4. Diagrama general del flujo

```text
                 ADMINISTRADOR
                       |
                       v
          Configuración institucional
          Usuarios, carreras, periodos,
          empresas, representantes
                y convenios
                       |
                       v
              EMPRESA / REPRESENTANTE
                       |
                       v
               Crear oferta
                       |
                       v
                   BORRADOR
                       |
                       v
              Enviar a revisión
                       |
                       v
              PENDIENTE_REVISION
                       |
                       v
                  COORDINADOR
                       |
               Revisar la oferta
                       |
              +--------+--------+
              |                 |
              v                 v
           APROBAR           RECHAZAR
              |                 |
              v                 v
          PUBLICADA          RECHAZADA
              |
              v
            ESTUDIANTE
              |
              v
        Consultar ofertas
              |
              v
        Seleccionar perfil
              |
              v
        Enviar postulación
              |
              v
           PENDIENTE
              |
              v
          COORDINADOR
              |
       Revisar postulación
              |
        +-----+------+
        |            |
        v            v
      ACEPTAR      RECHAZAR
        |            |
        v            v
    ACEPTADA      RECHAZADA
        |
        v
 Generación automática
 de práctica individual
        |
        v
      APROBADA
        |
        v
 Asignación de docente
        |
        v
 PRÁCTICA CON DOCENTE
      ASIGNADO
```

---

# 5. Estados del proceso

## 5.1. Estados de las ofertas

| Estado | Descripción |
|---|---|
| `BORRADOR` | Oferta creada que todavía no ha sido enviada a revisión. |
| `PENDIENTE_REVISION` | Oferta enviada por la empresa y pendiente de decisión del coordinador. |
| `PUBLICADA` | Oferta aprobada y disponible para los estudiantes. |
| `RECHAZADA` | Oferta rechazada por el coordinador. |
| `CERRADA` | Oferta que ha finalizado su periodo de publicación o fue cerrada según las reglas del sistema. |
| `CANCELADA` | Oferta cancelada según las reglas correspondientes. |

## 5.2. Estados de las postulaciones

| Estado | Descripción |
|---|---|
| `PENDIENTE` | Postulación enviada por el estudiante y pendiente de revisión. |
| `ACEPTADA` | Postulación aprobada por el coordinador. Genera una práctica individual. |
| `RECHAZADA` | Postulación rechazada por el coordinador. |
| `RETIRADA` | Estado contemplado para una postulación retirada. |
| `CANCELADA` | Estado contemplado para una postulación cancelada. |

## 5.3. Estados de las prácticas

La práctica se crea inicialmente con el estado:

`APROBADA`

El modelo de datos contempla información para las siguientes etapas del ciclo de vida de la práctica. Sin embargo, no todas las etapas posteriores se encuentran habilitadas todavía en la interfaz y en los servicios.

---

# 6. Roles del sistema

PRAXIS utiliza un sistema de roles para controlar las funcionalidades disponibles para cada usuario.

## 6.1. Administrador — `ADMINISTRADOR`

Es responsable de la configuración general del sistema.

Funciones principales:

- Administración de usuarios.
- Asignación de roles.
- Gestión de facultades.
- Gestión de carreras.
- Gestión de periodos académicos.
- Gestión de empresas.
- Gestión de representantes.
- Gestión de convenios.
- Consulta de auditoría.

## 6.2. Coordinador — `COORDINADOR`

Es responsable del control académico de las prácticas.

Funciones principales:

- Consultar ofertas pendientes.
- Aprobar o rechazar ofertas.
- Consultar postulaciones pendientes.
- Aceptar o rechazar postulaciones.
- Generar prácticas mediante la aceptación de postulaciones.
- Consultar prácticas sin docente tutor.
- Consultar docentes disponibles.
- Asignar docentes tutores.
- Gestionar las operaciones dentro de su alcance académico.

## 6.3. Representante empresarial — `REPRESENTANTE_EMPRESA`

Es el usuario autorizado para administrar las oportunidades de una empresa.

Funciones principales:

- Consultar sus empresas autorizadas.
- Crear ofertas.
- Administrar perfiles de oferta.
- Definir plazas y requisitos.
- Editar ofertas según su estado.
- Enviar ofertas a revisión.
- Consultar el estado de sus ofertas.

## 6.4. Estudiante — `ESTUDIANTE`

Es el usuario que participa en las oportunidades publicadas.

Funciones principales:

- Consultar ofertas publicadas.
- Consultar perfiles disponibles.
- Revisar requisitos.
- Enviar postulaciones.
- Consultar sus propias postulaciones.
- Conocer el resultado de sus solicitudes.

## 6.5. Docente — `DOCENTE`

Es el usuario que puede ser asignado como tutor académico de una práctica.

El sistema utiliza su perfil docente, carrera y alcance académico para determinar si puede ser asignado a una práctica.

El portal completo de seguimiento de prácticas por parte del docente corresponde a una etapa posterior de desarrollo.

---

# 7. Reglas de negocio principales

El funcionamiento de PRAXIS se encuentra protegido por validaciones en el backend y reglas de integridad de la base de datos.

Las reglas más importantes son:

1. Una empresa debe encontrarse activa para gestionar ofertas.
2. Un representante debe tener una vinculación activa con la empresa.
3. El convenio debe pertenecer a la empresa correspondiente.
4. El convenio debe estar vigente.
5. El tipo de práctica debe estar activo.
6. El periodo académico debe ser válido.
7. Las fechas de la oferta deben respetar las reglas establecidas.
8. Las plazas de un perfil deben ser mayores que cero.
9. Una oferta debe ser aprobada por un coordinador antes de publicarse.
10. Un estudiante solo puede postularse a ofertas publicadas.
11. La postulación debe realizarse dentro del plazo establecido.
12. La carrera del estudiante debe ser compatible con el perfil.
13. No se deben superar las plazas disponibles.
14. No se deben duplicar postulaciones activas del mismo estudiante al mismo perfil.
15. Solamente un coordinador autorizado puede aceptar o rechazar postulaciones.
16. El rechazo de una oferta o postulación requiere un motivo.
17. La aceptación de una postulación genera una práctica individual.
18. La generación de la práctica debe ejecutarse de manera transaccional.
19. Solo se pueden asignar docentes activos con rol `DOCENTE`.
20. El docente debe tener compatibilidad académica con la práctica.
21. Las operaciones sensibles deben mantener trazabilidad.

---

# 8. Trazabilidad y auditoría

La trazabilidad es una parte importante de PRAXIS porque permite conservar el historial de las decisiones tomadas durante el proceso.

El sistema cuenta con estructuras para registrar cambios relevantes, como:

- Historial de estados de ofertas.
- Historial de recomendaciones de postulaciones.
- Historial de estados de prácticas.
- Auditoría de operaciones administrativas.

Esto permite mantener un registro de los eventos importantes del proceso y facilitar su revisión institucional.

---

# 9. Arquitectura del sistema

PRAXIS está desarrollado utilizando una arquitectura por capas, con separación entre la lógica de negocio, la persistencia y la interfaz de usuario.

```text
             INTERFAZ WEB
              React / Vite
                   |
                   v
              API REST
                Quarkus
                   |
                   v
          CAPA DE APLICACIÓN
          Servicios y reglas
             de negocio
                   |
                   v
             CAPA DOMINIO
          Entidades y contratos
                   |
                   v
          CAPA INFRAESTRUCTURA
          Persistencia y acceso
               a datos
                   |
                   v
               PostgreSQL
```

## 9.1. Backend

El backend está desarrollado con Java y Quarkus.

Se encarga de:

- Exponer los endpoints REST.
- Validar las solicitudes.
- Aplicar las reglas de negocio.
- Controlar los permisos.
- Ejecutar transacciones.
- Gestionar la autenticación.
- Acceder a la base de datos.
- Registrar eventos y auditoría.

## 9.2. Frontend

La interfaz está desarrollada con React y Vite.

Se encarga de:

- Mostrar las opciones correspondientes a cada rol.
- Permitir la interacción con los módulos.
- Consultar información mediante la API.
- Enviar solicitudes al backend.
- Mostrar estados, resultados y mensajes de validación.
- Presentar las pantallas de administración y gestión.

La interfaz mantiene una apariencia visual uniforme, mientras que las opciones disponibles dependen de los roles asignados al usuario.

## 9.3. Base de datos

El sistema utiliza PostgreSQL para almacenar la información institucional y operativa.

Hibernate ORM Panache facilita el acceso a los datos, mientras que Flyway administra las migraciones de la estructura de la base de datos.

---

# 10. Tecnologías utilizadas

| Tecnología | Función |
|---|---|
| Java 21 | Lenguaje del backend. |
| Quarkus 3.40.1 | Framework del backend. |
| React | Desarrollo de la interfaz web. |
| Vite | Herramienta de desarrollo del frontend. |
| PostgreSQL | Base de datos relacional. |
| Hibernate ORM Panache | Persistencia de datos. |
| Flyway | Control de migraciones. |
| JWT | Autenticación mediante tokens. |
| RSA | Firma y validación de tokens. |
| BCrypt | Hash de contraseñas. |
| Maven | Construcción y administración del backend. |

---

# 11. Estructura general del proyecto

```text
praxis/
│
├── src/
│   ├── main/
│   │   │
│   │   ├── java/ec/edu/praxis/
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── modelo/
│   │   │   │   └── repositorio/
│   │   │   │
│   │   │   ├── application/
│   │   │   │   ├── oferta/
│   │   │   │   ├── practica/
│   │   │   │   ├── empresa/
│   │   │   │   ├── estudiante/
│   │   │   │   └── seguridad/
│   │   │   │
│   │   │   ├── infrastructure/
│   │   │   │   └── persistencia/
│   │   │   │
│   │   │   └── web/
│   │   │       ├── rest/
│   │   │       ├── dto/
│   │   │       └── error/
│   │   │
│   │   ├── resources/
│   │   │   └── db/
│   │   │       ├── migration/
│   │   │       └── seed/
│   │   │
│   │   └── webui/
│   │
│   └── test/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

### Descripción de las capas

- `domain`: contiene los modelos del negocio y contratos de repositorios.
- `application`: contiene los servicios, casos de uso y reglas de negocio.
- `infrastructure`: contiene las implementaciones técnicas de persistencia.
- `web`: contiene los recursos REST, DTO y manejo de errores.
- `resources/db`: contiene migraciones y datos iniciales.
- `webui`: contiene la aplicación frontend.

---

# 12. Base de datos y migraciones

Las migraciones de la base de datos se encuentran en:

```text
src/main/resources/db/migration/
```

Flyway se encarga de ejecutarlas en orden.

Entre las migraciones del proyecto se encuentran:

| Migración | Descripción |
|---|---|
| `V1__esquema.sql` | Creación del esquema inicial, tablas y relaciones. |
| `V2__catalogos.sql` | Datos de referencia y catálogos institucionales. |
| `V3__ofertas_practicas.sql` | Estructuras y reglas relacionadas con ofertas y postulaciones. |
| `V4__perfiles_docentes_automaticos.sql` | Creación y regularización de perfiles docentes para usuarios con rol `DOCENTE`. |

Los datos iniciales o de desarrollo se encuentran en:

```text
src/main/resources/db/seed/
```

**Importante:** no se deben modificar migraciones que ya fueron ejecutadas en una base de datos. Para incorporar cambios se debe crear una nueva migración con la siguiente versión disponible.

---

# 13. Seguridad

PRAXIS implementa mecanismos de seguridad para proteger la información y las operaciones.

Entre ellos:

- Autenticación de usuarios.
- Tokens JWT.
- Firma RSA.
- Contraseñas protegidas mediante BCrypt.
- Control de acceso por roles.
- Restricciones en endpoints REST.
- Validación de permisos en los servicios.
- Alcance académico por carrera.
- Bloqueo temporal por intentos fallidos, según la configuración.
- Cambio de contraseña cuando corresponde.
- Registro de operaciones sensibles.

La seguridad no depende únicamente de las opciones visibles en el frontend. El backend valida nuevamente los permisos antes de permitir una operación.

---

# 14. API REST principal

Los endpoints de la aplicación se encuentran organizados bajo el prefijo `/api`.

Algunas de las rutas principales del flujo son:

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/login` | Inicio de sesión. |
| GET | `/auth/me` | Consultar usuario autenticado. |
| GET | `/admin/usuarios` | Consultar usuarios. |
| POST | `/admin/usuarios` | Registrar usuarios. |
| GET/POST | `/admin/empresas` | Consultar y registrar empresas. |
| GET/POST | `/admin/convenios` | Consultar y registrar convenios. |
| GET/POST | `/empresa/ofertas` | Consultar y crear ofertas. |
| PUT | `/empresa/ofertas/{id}` | Actualizar oferta. |
| PATCH | `/empresa/ofertas/{id}/enviar-revision` | Enviar oferta a revisión. |
| GET | `/coordinacion/ofertas/pendientes` | Consultar ofertas pendientes. |
| PATCH | `/coordinacion/ofertas/{id}/aprobar` | Aprobar oferta. |
| PATCH | `/coordinacion/ofertas/{id}/rechazar` | Rechazar oferta. |
| GET | `/estudiante/ofertas` | Consultar ofertas publicadas. |
| GET | `/estudiante/ofertas/{id}` | Consultar detalle de oferta. |
| GET/POST | `/estudiante/postulaciones` | Consultar y registrar postulaciones. |
| GET | `/coordinacion/postulaciones/pendientes` | Consultar postulaciones pendientes. |
| PATCH | `/coordinacion/postulaciones/{id}/aceptar` | Aceptar postulación y generar práctica. |
| PATCH | `/coordinacion/postulaciones/{id}/rechazar` | Rechazar postulación. |
| GET | `/coordinacion/practicas/pendientes-docente` | Consultar prácticas sin docente tutor. |
| GET | `/coordinacion/practicas/{id}/docentes-disponibles` | Consultar docentes compatibles. |
| PATCH | `/coordinacion/practicas/{id}/docente` | Asignar docente tutor. |

Para consultar el detalle de los endpoints y sus parámetros se puede utilizar Swagger UI.

---

# 15. Funcionalidades desarrolladas y siguientes etapas

## Funcionalidades operativas del flujo principal

- [x] Autenticación y gestión de roles.
- [x] Administración de usuarios.
- [x] Gestión de facultades y carreras.
- [x] Gestión de periodos académicos.
- [x] Administración de empresas.
- [x] Administración de representantes empresariales.
- [x] Gestión de convenios.
- [x] Creación de ofertas empresariales.
- [x] Envío de ofertas a revisión.
- [x] Revisión y aprobación de ofertas por coordinación.
- [x] Consulta de ofertas por estudiantes.
- [x] Registro de postulaciones.
- [x] Consulta de postulaciones del estudiante.
- [x] Revisión de postulaciones por coordinación.
- [x] Aceptación y rechazo de postulaciones.
- [x] Generación automática de prácticas individuales.
- [x] Consulta de prácticas aprobadas sin docente tutor.
- [x] Consulta de docentes disponibles.
- [x] Asignación de docente tutor.

## Etapas posteriores por desarrollar o completar

El sistema contempla la evolución del proceso hacia un seguimiento integral de las prácticas.

Entre las siguientes funcionalidades se encuentran:

- [ ] Portal del docente tutor.
- [ ] Asignación y gestión del tutor empresarial.
- [ ] Registro de actividades.
- [ ] Registro y control de horas realizadas.
- [ ] Carga de informes y evidencias.
- [ ] Revisión y aprobación de documentos.
- [ ] Gestión de incidencias.
- [ ] Registro de visitas de seguimiento.
- [ ] Evaluación de estudiantes.
- [ ] Cierre de prácticas.
- [ ] Generación de certificados.
- [ ] Reportes académicos y administrativos.

Estas funcionalidades representan etapas posteriores del proyecto. La existencia de entidades relacionadas en la base de datos no significa que todos estos procesos estén ya disponibles en la interfaz o en la API.

---

# 16. Instalación y ejecución local

## Requisitos

- JDK 21.
- PostgreSQL 15 o superior.
- Git.
- Docker para las pruebas que utilizan Testcontainers.
- Node.js, según la configuración del frontend.

## 16.1. Clonar el proyecto

```bash
git clone URL_DEL_REPOSITORIO
cd praxis
```

## 16.2. Crear la base de datos

En PostgreSQL ejecutar:

```sql
CREATE USER praxis WITH PASSWORD 'praxis';
CREATE DATABASE praxis OWNER praxis;
```

Configuración local predeterminada:

| Parámetro | Valor |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base de datos | `praxis` |
| Usuario | `praxis` |
| Contraseña | `praxis` |

Estos valores son para desarrollo local. En producción deben utilizarse credenciales seguras.

## 16.3. Generar claves RSA

El proyecto incluye `GenerarClaves.java`.

En Windows:

```powershell
javac GenerarClaves.java
java GenerarClaves
```

Se generan las claves necesarias para la autenticación JWT.

**Importante:** no se debe compartir ni subir la clave privada al repositorio.

## 16.4. Ejecutar el backend

Windows:

```powershell
.\mvnw.cmd quarkus:dev
```

Linux o macOS:

```bash
./mvnw quarkus:dev
```

## 16.5. Direcciones locales

| Servicio | Dirección |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/q/swagger-ui |
| API de salud | http://localhost:8080/api/salud |

## 16.6. Comandos útiles

| Acción | Comando |
|---|---|
| Ejecutar en desarrollo | `.\mvnw.cmd quarkus:dev` |
| Compilar | `.\mvnw.cmd compile` |
| Ejecutar verificaciones | `.\mvnw.cmd verify` |
| Construir | `.\mvnw.cmd package` |
| Instalar dependencias frontend | `cd src/main/webui && npm install` |
| Ejecutar pruebas frontend | `cd src/main/webui && npm test` |

---

# 17. Despliegue

El proyecto contempla la posibilidad de desplegar sus componentes utilizando servicios externos.

Entre las opciones consideradas se encuentran:

- Neon para PostgreSQL.
- Render para el backend.
- GitHub Actions para automatización.

La configuración de producción debe utilizar variables de entorno y secretos administrados por la plataforma.

No se deben subir al repositorio:

- Contraseñas reales.
- Tokens.
- Archivos `.env`.
- Claves privadas RSA.
- Credenciales de bases de datos.

---

# 18. Convenciones de desarrollo

Para mantener la estabilidad del sistema se deben seguir estas recomendaciones:

1. Desarrollar una funcionalidad a la vez.
2. Revisar el código existente antes de modificarlo.
3. Mantener la arquitectura por capas.
4. No trasladar las reglas de negocio únicamente al frontend.
5. Validar permisos tanto en la interfaz como en el backend.
6. Crear nuevas migraciones en lugar de modificar las ya ejecutadas.
7. Utilizar transacciones en operaciones que dependan unas de otras.
8. Mantener la trazabilidad de las decisiones.
9. Evitar duplicar información o registros.
10. Probar el flujo completo después de cada modificación.

---

# 19. Conclusión: el corazón de PRAXIS

El propósito fundamental de PRAXIS es transformar el proceso de prácticas preprofesionales en un flujo institucional organizado, verificable y trazable.

El sistema comienza cuando una empresa registra una oportunidad. Esa oportunidad pasa por una revisión académica antes de publicarse. Posteriormente, los estudiantes pueden postularse y el coordinador evalúa cada solicitud.

Cuando una postulación es aceptada, PRAXIS genera automáticamente una práctica individual para el estudiante y permite asignarle un docente tutor compatible con su carrera.

Por lo tanto, el flujo central puede resumirse en:

**La empresa ofrece una oportunidad. La universidad la valida. El estudiante se postula. El coordinador aprueba. PRAXIS genera la práctica y permite asignar al docente tutor.**

Esta es la base sobre la que se construirán las siguientes etapas de seguimiento, control de horas, actividades, evidencias, evaluaciones y cierre de las prácticas.

---

**PRAXIS — Gestión de Prácticas Preprofesionales**  
*Organizando la relación entre universidad, estudiantes y empresas.*
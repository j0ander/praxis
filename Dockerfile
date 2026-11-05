
# Etapa de construcción
FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /app

COPY . .

RUN mvn package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/quarkus-app/ ./

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]

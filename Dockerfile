
# Etapa de construcción
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/quarkus-app/ ./

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]

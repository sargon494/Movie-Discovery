# ─────────────────────────────────────────────────────────────────────────────
# STAGE 1 — BUILD
# Compila el proyecto y genera el fat JAR con Maven
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar la caché de Docker
# Si el código cambia pero el pom no, Maven no re-descarga dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 2 — RUN
# Imagen final ligera, solo con Java — sin Maven ni código fuente
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copiamos solo el JAR generado en el stage anterior
COPY --from=build /app/target/backend-1.0.0.jar app.jar

# Puerto donde Jetty escucha
EXPOSE 8090

# Arranca el backend
ENTRYPOINT ["java", "-jar", "app.jar"]
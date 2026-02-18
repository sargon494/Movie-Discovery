# ─────────────────────────────────────────────────────────────────────────────
# STAGE 1 — BUILD
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 2 — RUN
# Necesita JDK completo (no solo JRE) para Swing + soporte gráfico X11
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Librerías necesarias para que Swing pueda dibujar ventanas
# a través de WSLg / X11
RUN apt-get update && apt-get install -y \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libx11-6 \
    fonts-dejavu \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/frontend-1.0.0.jar app.jar

# DISPLAY le dice a Swing dónde pintar la ventana
# WSLg lo captura automáticamente y lo muestra en Windows
ENV DISPLAY=:0

ENTRYPOINT ["java", "-jar", "app.jar"]
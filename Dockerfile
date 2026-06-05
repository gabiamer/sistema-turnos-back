# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Descarga dependencias primero (capa cacheada mientras pom.xml no cambie)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Compila sin tests
COPY src/ src/
RUN mvn package -DskipTests -B

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]

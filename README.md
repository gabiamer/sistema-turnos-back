# Sistema de Turnos Médicos — Backend

API REST para gestión de turnos médicos. Desarrollada con **Spring Boot 3**, **PostgreSQL** y **Flyway**.

## Requisitos

- Java 21
- Maven 3.9+ (o usar el wrapper `./mvnw` incluido)
- PostgreSQL 14+

## Instalación

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd sistema-turnos-back
```

### 2. Crear la base de datos

Conectarse a PostgreSQL y ejecutar:

```sql
CREATE DATABASE turnos_db;
```

### 3. Configurar variables de entorno

Copiar el archivo de ejemplo y completar los valores:

```bash
cp .env.example .env
```

Editar `.env`:

```env
# Base de datos
DB_URL=jdbc:postgresql://localhost:5432/turnos_db
DB_USERNAME=tu_usuario
DB_PASSWORD=tu_contraseña

# Correo (Gmail SMTP) — opcional, solo para notificaciones por email
MAIL_USERNAME=tu-correo@gmail.com
MAIL_PASSWORD=tu-app-password-de-gmail

# Twilio — opcional, solo para notificaciones por SMS / WhatsApp
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_PHONE=+1XXXXXXXXXX
TWILIO_WHATSAPP_FROM=+14155238886
```

> Las variables de Twilio y Mail son opcionales. Si no se configuran, las notificaciones se registran solo en el log.

### 4. Ejecutar la aplicación

```bash
./mvnw spring-boot:run
```

En Windows sin bash:

```cmd
mvnw.cmd spring-boot:run
```

Flyway aplica las migraciones automáticamente al iniciar. La API queda disponible en:

```
http://localhost:8090
```

## Compilar y ejecutar el JAR

```bash
./mvnw clean package -DskipTests
java -jar target/turnos-medicos-backend-0.0.1-SNAPSHOT.jar
```

## Ejecutar los tests

```bash
./mvnw test
```

## Estructura de la base de datos

Las tablas se crean mediante migraciones Flyway ubicadas en `src/main/resources/db/migration/`. No es necesario ejecutar ningún script SQL manualmente.

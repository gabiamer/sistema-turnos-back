# Contexto del proyecto: sistema-turnos-back

Backend REST de un sistema de gestión de turnos médicos. Desarrollado con Spring Boot 3, JPA/Hibernate, PostgreSQL y Flyway. Sigue una arquitectura en capas (dominio → aplicación → infraestructura) por módulo de negocio.

---

## Raíz del proyecto

| Archivo / carpeta | Descripción |
|---|---|
| `pom.xml` | Dependencias Maven: Spring Boot, Lombok, Flyway, Twilio, Spring Mail |
| `api-contracts.md` | Contrato de los endpoints REST acordado con el frontend |
| `src/` | Todo el código fuente y tests |
| `target/` | Artefactos compilados (generado, no versionar) |

---

## src/main/java/…/

Raíz del paquete: `com.turnos.turnos_medicos_backend`

### Archivos en la raíz del paquete

| Archivo | Descripción |
|---|---|
| `TurnosMedicosBackendApplication.java` | Punto de entrada de la aplicación Spring Boot |
| `DataSeeder.java` | Carga datos iniciales (médicos, pacientes, agendas) al arrancar en perfil de desarrollo |

---

### Módulo `agenda/`

Gestiona la disponibilidad horaria de los médicos: configuración semanal de franjas y bloqueos de días.

```
agenda/
├── application/
│   └── AgendaMedicoService.java       — CRUD de franjas horarias y bloqueos de días
├── domain/
│   ├── model/
│   │   ├── AgendaMedico.java          — Entidad: franja horaria semanal de un médico
│   │   │                                (diaSemana, horaInicio, horaFin, duracionMinutos, activo)
│   │   └── BloqueoDia.java            — Entidad: rango de fechas en que el médico no atiende
│   └── port/
│       ├── AgendaMedicoRepository.java  — Puerto (interfaz) de persistencia de AgendaMedico
│       └── BloqueoDiaRepository.java   — Puerto (interfaz) de persistencia de BloqueoDia
└── infrastructure/
    ├── AgendaMedicoController.java    — REST /api/agenda (GET/POST/PUT franjas, POST bloqueos)
    ├── AgendaMedicoRepositoryImpl.java — Implementación JPA de AgendaMedicoRepository
    └── BloqueoDiaRepositoryImpl.java   — Implementación JPA de BloqueoDiaRepository
```

---

### Módulo `medico/`

Gestiona el alta y consulta de médicos del sistema.

```
medico/
├── application/
│   └── MedicoService.java             — CRUD de médicos
├── domain/
│   ├── model/
│   │   └── Medico.java                — Entidad: médico (nombre, apellido, especialidad, email)
│   └── port/
│       └── MedicoRepository.java      — Puerto de persistencia de Medico
└── infrastructure/
    ├── MedicoController.java          — REST /api/medicos (GET lista, GET por id, POST, PUT, DELETE)
    └── MedicoRepositoryImpl.java       — Implementación JPA de MedicoRepository
```

---

### Módulo `paciente/`

Gestiona el alta y consulta de pacientes.

```
paciente/
├── application/
│   └── PacienteService.java           — CRUD de pacientes
├── domain/
│   ├── model/
│   │   └── Paciente.java              — Entidad: paciente (ci, nombre, apellido, email, telefono)
│   └── port/
│       └── PacienteRepository.java    — Puerto de persistencia de Paciente
└── infrastructure/
    ├── PacienteController.java        — REST /api/pacientes (GET, POST, PUT, DELETE)
    └── PacienteRepositoryImpl.java     — Implementación JPA de PacienteRepository
```

---

### Módulo `turno/`

Núcleo del sistema. Maneja el ciclo de vida completo de un turno: solicitud, confirmación, cancelación, reprogramación y conclusión. También expone la disponibilidad de slots.

```
turno/
├── application/
│   ├── SolicitarTurnoService.java         — Bloquea un slot (5 min) y confirma el turno
│   │                                         Estados: BLOQUEADO → CONFIRMADO / EXPIRADO
│   ├── CancelarTurnoService.java          — Cancela un turno
│   │                                         · cancelarTurno(): flujo del paciente (valida owner + 2h)
│   │                                         · cancelarPorMedico(): flujo del médico (valida motivo + 2h)
│   ├── ConcluirReprogramarTurnoService.java — Marca CONCLUIDA y reprograma turnos
│   ├── DisponibilidadService.java         — Genera slots libres/ocupados de la semana del médico
│   ├── NotificacionService.java           — Envía notificaciones por canal (EMAIL / SMS / WHATSAPP)
│   │                                         Usa patrón Strategy; cada canal es un @Service interno
│   └── NotificacionTurnoService.java      — Versión anterior de notificaciones (Twilio + Spring Mail)
│
├── domain/
│   ├── model/
│   │   ├── Turno.java                     — Entidad principal: turno médico
│   │   │                                     (paciente, medico, fecha, hora, estado, motivo, auditoría)
│   │   ├── EstadoTurno.java               — Enum: PENDIENTE, BLOQUEADO, CONFIRMADO,
│   │   │                                     CANCELADO, EXPIRADO, CONCLUIDA
│   │   ├── SlotDTO.java                   — DTO de slot público (fecha, hora, disponible, bloqueado)
│   │   ├── AgendaSlotDTO.java             — DTO de slot con detalle de turno (para la vista del médico)
│   │   └── SlotMedicoDTO.java             — DTO alternativo de slot para agenda del médico
│   └── port/
│       └── TurnoRepository.java           — Puerto de persistencia de Turno
│
└── infrastructure/
    ├── TurnoController.java               — REST /api/turnos
    │                                         POST  /solicitar
    │                                         POST  /{id}/confirmar
    │                                         GET   /?pacienteId=
    │                                         DELETE /{id}              (paciente cancela)
    │                                         DELETE /{id}/medico       (médico cancela)
    │                                         PATCH  /{id}/estado       (médico concluye)
    │                                         PUT    /{id}/reprogramar  (médico reprograma)
    ├── DisponibilidadController.java       — REST /api/disponibilidad
    │                                         GET /slots   (slots públicos de la semana)
    │                                         GET /agenda  (agenda detallada para el médico)
    └── TurnoRepositoryImpl.java            — Implementación JPA de TurnoRepository
```

---

### Módulo `secretaria/`

Implementa el caso de uso CU-07: la secretaria/recepcionista puede registrar pacientes, agendar turnos directamente (sin bloqueo temporal) y cancelar turnos con privilegios de rol.

```
secretaria/
├── application/
│   └── SecretariaService.java             — 5 métodos del caso de uso:
│                                             · buscarPaciente(q): CI exacto o nombre/apellido ILIKE
│                                             · registrarPaciente(): verifica CI único antes de crear
│                                             · agendarTurnoDirecto(): crea turno CONFIRMADO directo,
│                                               setea agendadoPor, verifica slot y duplicado del día
│                                             · cancelarTurnoAdmin(): cancela sin restricción de 2h,
│                                               motivo mínimo 10 chars
│                                             · turnosHoy(): todos los turnos de la fecha actual
│
├── domain/
│   ├── model/
│   │   └── Recepcionista.java             — Entidad: recepcionista (nombre, apellido, email, rol)
│   └── port/
│       └── RecepcionistaRepository.java   — Puerto: findById, save, findAll
│
└── infrastructure/
    ├── RecepcionistaRepositoryImpl.java   — Implementación JPA
    └── SecretariaController.java          — REST /api/secretaria
                                             POST   /pacientes          → 201 o 409
                                             GET    /pacientes?q=       → 200 [Paciente]
                                             POST   /turnos             → 201 {turnoId, estado, agendadoPor}
                                             DELETE /turnos/{id}        → 204
                                             GET    /turnos/hoy         → 200 [Turno]
                                             Todos los endpoints validan header X-User-Role: SECRETARIA → 403
```

**Turno.java** — campo agregado en este sprint:
- `agendadoPor (Long, nullable)` — ID de la recepcionista que creó el turno

---

### Módulo `shared/`

Puertos e implementaciones transversales usados por múltiples módulos.

```
shared/
├── adapters/
│   ├── DisponibilidadAdapter.java    — Implementa DisponibilidadPort consultando AgendaMedico
│   │                                   y BloqueoDia para determinar si un slot está libre
│   └── LogEventPublisher.java        — Implementa IEventPublisher publicando eventos por log
└── ports/
    ├── DisponibilidadPort.java       — Interfaz: estaDisponible(medicoId, fecha, hora)
    └── IEventPublisher.java          — Interfaz: publicar(evento, turnoId)
```

---

## src/main/resources/

| Archivo | Descripción |
|---|---|
| `application.properties` | Configuración de base de datos, JPA, Flyway, Twilio y Spring Mail |
| `db/migration/V2__fix_estado_turno_check.sql` | Migración Flyway: amplía el check constraint de `estado` en la tabla `turnos` para incluir los 6 valores del enum |

---

## src/test/

| Archivo | Descripción |
|---|---|
| `TurnosMedicosBackendApplicationTests.java` | Test de carga del contexto Spring |
| `DataSeeder.java` | Copia del seeder usada en contexto de tests |
| `paciente/application/PacienteServiceTest.java` | Tests unitarios de PacienteService |
| `turno/application/DisponibilidadServiceTest.java` | Tests unitarios de DisponibilidadService |

---

## Flujo principal de un turno

```
Paciente solicita slot
        │
        ▼
POST /api/turnos/solicitar
  → SolicitarTurnoService.bloquearHorario()
  → estado: BLOQUEADO (expira en 5 min)
        │
        ▼
POST /api/turnos/{id}/confirmar
  → SolicitarTurnoService.confirmarTurno()
  → estado: CONFIRMADO
        │
   ┌────┴────────────────────┐
   ▼                         ▼
Médico concluye         Médico / Paciente cancelan
PATCH /{id}/estado      DELETE /{id}/medico  o  DELETE /{id}
estado: CONCLUIDA       estado: CANCELADO
                             │
                        NotificacionService
                        (EMAIL / SMS / WHATSAPP)
```

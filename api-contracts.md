# API Contracts - Sistema de Turnos

## Pacientes

### POST /api/pacientes
- **body:** `{ "ci", "nombre", "apellido", "fechaNacimiento", "telefono", "email" }`
- **201:** `{ "id", "ci", "nombre", "apellido", "fechaNacimiento", "telefono", "email", "creadoEn" }`
- **409:** `{ "error": "CI ya registrado" }`

### GET /api/pacientes?ci=12345
- **200:** `{ "id", "ci", "nombre", "apellido", "fechaNacimiento", "telefono", "email" }`
- **404:** `{ "error": "Paciente no encontrado" }`

---

## Médicos

### GET /api/medicos 
*(También válido: `/api/medicos?especialidad=Cardiología`)*
- **200:** `[{ "id", "nombre", "apellido", "especialidad", "email" }]`

### GET /api/medicos/{id}/disponibilidad?semana=2026-05-04
- **200:** `[{ "fecha", "hora", "disponible": true/false }]`
- **404:** `{ "error": "Médico no encontrado" }`

### PUT /api/medicos/{id}/agenda
- **body:** `{ "diasSemana": [0,1,2,3,4], "horaInicio", "horaFin", "duracionMinutos" }`
- **200:** `{ "message": "Agenda actualizada" }`
- **409:** `{ "error": "Conflicto con turnos", "turnos": [...] }`

---

## Turnos

### POST /api/turnos/solicitar
- **body:** `{ "pacienteId", "medicoId", "fecha", "hora" }`
- **200:** `{ "turnoId", "bloqueoExpira": "2026-05-04T10:05:00" }`
- **409:** `{ "error": "Slot ya ocupado" }`
- **422:** `{ "error": "Paciente ya tiene turno ese día" }`

### POST /api/turnos/{id}/confirmar
- **200:** `{ "id", "estado": "CONFIRMADO", "fecha", "hora", "medico", "paciente" }`
- **408:** `{ "error": "Bloqueo expirado, slot liberado" }`

### GET /api/turnos?pacienteId=1
- **200:** `[{ "id", "fecha", "hora", "estado", "medico": { "nombre", "especialidad" } }]`

### DELETE /api/turnos/{id}
- **body:** `{ "pacienteId" }`
- **200:** `{ "message": "Turno cancelado" }`
- **422:** `{ "error": "No se puede cancelar con menos de 2hs" }`
- **403:** `{ "error": "No autorizado" }`
package com.turnos.turnos_medicos_backend.secretaria.infrastructure;

import com.turnos.turnos_medicos_backend.secretaria.application.SecretariaService;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/secretaria")
@CrossOrigin(origins = "*")
public class SecretariaController {

    private final SecretariaService secretariaService;

    public SecretariaController(SecretariaService secretariaService) {
        this.secretariaService = secretariaService;
    }

    /** POST /api/secretaria/pacientes */
    @PostMapping("/pacientes")
    public ResponseEntity<?> registrarPaciente(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestBody RegistrarPacienteRequest req) {
        if (!"SECRETARIA".equals(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            var paciente = secretariaService.registrarPaciente(
                    req.ci(), req.nombre(), req.apellido(),
                    req.fechaNacimiento(), req.telefono(), req.email());
            return ResponseEntity.status(201).body(paciente);
        } catch (SecretariaService.CIDuplicadoException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/secretaria/pacientes?q= */
    @GetMapping("/pacientes")
    public ResponseEntity<?> buscarPaciente(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestParam String q) {
        if (!"SECRETARIA".equals(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            return ResponseEntity.ok(secretariaService.buscarPaciente(q));
        } catch (SecretariaService.BusquedaInsuficienteException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/secretaria/turnos */
    @PostMapping("/turnos")
    public ResponseEntity<?> agendarTurno(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestBody AgendarTurnoRequest req) {
        if (!"SECRETARIA".equals(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            Turno turno = secretariaService.agendarTurnoDirecto(
                    req.pacienteId(), req.medicoId(), req.fecha(), req.hora(), req.recepcionistaId());
            return ResponseEntity.status(201).body(Map.of(
                    "turnoId", turno.getId(),
                    "estado", turno.getEstado(),
                    "agendadoPor", turno.getAgendadoPor()
            ));
        } catch (SecretariaService.SlotOcupadoException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        } catch (SecretariaService.TurnoDuplicadoException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/secretaria/turnos/{id} */
    @DeleteMapping("/turnos/{id}")
    public ResponseEntity<?> cancelarTurno(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @PathVariable Long id,
            @RequestBody CancelarAdminRequest req) {
        if (!"SECRETARIA".equals(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            secretariaService.cancelarTurnoAdmin(id, req.recepcionistaId(), req.motivo());
            return ResponseEntity.noContent().build();
        } catch (SecretariaService.MotivoInsuficienteException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/secretaria/turnos/hoy */
    @GetMapping("/turnos/hoy")
    public ResponseEntity<?> turnosHoy(
            @RequestHeader(value = "X-User-Role", required = false) String rol) {
        if (!"SECRETARIA".equals(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        return ResponseEntity.ok(secretariaService.turnosHoy());
    }

    // ── Request records ───────────────────────────────────────────────────────
    record RegistrarPacienteRequest(
            String ci, String nombre, String apellido,
            LocalDate fechaNacimiento, String telefono, String email) {}

    record AgendarTurnoRequest(
            Long pacienteId,
            Long medicoId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            Long recepcionistaId) {}

    record CancelarAdminRequest(Long recepcionistaId, String motivo) {}
}

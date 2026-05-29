package com.turnos.turnos_medicos_backend.turno.infrastructure;

import com.turnos.turnos_medicos_backend.turno.application.CancelarTurnoService;
import com.turnos.turnos_medicos_backend.turno.application.ConcluirReprogramarTurnoService;
import com.turnos.turnos_medicos_backend.turno.application.NotificacionService;
import com.turnos.turnos_medicos_backend.turno.application.SolicitarTurnoService;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/turnos")
@CrossOrigin(origins = "*")
public class TurnoController {

    private final SolicitarTurnoService solicitarTurnoService;
    private final CancelarTurnoService cancelarTurnoService;
    private final ConcluirReprogramarTurnoService concluirReprogramarTurnoService;
    private final NotificacionService notificacionService;

    public TurnoController(SolicitarTurnoService solicitarTurnoService,
                            CancelarTurnoService cancelarTurnoService,
                            ConcluirReprogramarTurnoService concluirReprogramarTurnoService,
                            NotificacionService notificacionService) {
        this.solicitarTurnoService = solicitarTurnoService;
        this.cancelarTurnoService = cancelarTurnoService;
        this.concluirReprogramarTurnoService = concluirReprogramarTurnoService;
        this.notificacionService = notificacionService;
    }

    /** POST /api/turnos/solicitar */
    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitar(@RequestBody SolicitarRequest req) {
        try {
            Turno turno = solicitarTurnoService.bloquearHorario(
                    req.pacienteId(), req.medicoId(), req.fecha(), req.hora());
            return ResponseEntity.ok(Map.of(
                    "turnoId", turno.getId(),
                    "bloqueoExpira", turno.getBloqueoExpira()
            ));
        } catch (SolicitarTurnoService.SlotOcupadoException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (SolicitarTurnoService.TurnoDuplicadoException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/turnos/{id}/confirmar */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id) {
        try {
            Turno turno = solicitarTurnoService.confirmarTurno(id);
            return ResponseEntity.ok(turno);
        } catch (SolicitarTurnoService.BloqueoExpiradoException e) {
            return ResponseEntity.status(410).body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/turnos?pacienteId=1 */
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam Long pacienteId) {
        return ResponseEntity.ok(cancelarTurnoService.listarPorPaciente(pacienteId));
    }

    /** DELETE /api/turnos/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                       @RequestBody CancelarRequest req) {
        String motivo = req.motivoCancelacion() != null ? req.motivoCancelacion() : req.motivo();
        if (motivo == null || motivo.isBlank()) {
            return ResponseEntity.status(400).body(Map.of("error", "motivo es requerido"));
        }
        try {
            Turno turno = cancelarTurnoService.cancelarTurno(id, req.pacienteId(), motivo);
            notificacionService.notificar(turno, req.canales(), motivo);
            return ResponseEntity.noContent().build();
        } catch (CancelarTurnoService.NoAutorizadoException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (CancelarTurnoService.FueraDePlazoException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/turnos/{id}/medico */
    @DeleteMapping("/{id}/medico")
    public ResponseEntity<?> cancelarMedico(@PathVariable Long id,
                                             @RequestBody CancelarMedicoRequest req) {
        try {
            Turno turno = cancelarTurnoService.cancelarPorMedico(id, req.motivoCancelacion());
            notificacionService.notificar(turno, req.canales(), req.motivoCancelacion());
            return ResponseEntity.ok(Map.of("message", "Turno cancelado"));
        } catch (CancelarTurnoService.MotivoRequeridoException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (CancelarTurnoService.FueraDePlazoException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        } catch (CancelarTurnoService.TurnoNoEncontradoException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /** PATCH /api/turnos/{id}/estado */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                            @RequestBody EstadoRequest req) {
        EstadoTurno nuevoEstado;
        try {
            nuevoEstado = EstadoTurno.valueOf(req.estado().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", "Estado inválido"));
        }
        try {
            Turno turno = concluirReprogramarTurnoService.cambiarEstado(id, nuevoEstado);
            return ResponseEntity.ok(turno);
        } catch (ConcluirReprogramarTurnoService.TurnoNoEncontradoException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (ConcluirReprogramarTurnoService.EstadoInvalidoException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/turnos/{id}/reprogramar */
    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<?> reprogramar(@PathVariable Long id,
                                          @RequestBody ReprogramarRequest req) {
        try {
            Turno turno = concluirReprogramarTurnoService.reprogramar(id, req.nuevaFecha(), req.nuevaHora());
            return ResponseEntity.ok(turno);
        } catch (ConcluirReprogramarTurnoService.TurnoNoEncontradoException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (ConcluirReprogramarTurnoService.SlotOcupadoException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (ConcluirReprogramarTurnoService.EstadoInvalidoException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Request records ───────────────────────────────────────────────────────
    record SolicitarRequest(
            Long pacienteId,
            Long medicoId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora
    ) {}

    record CancelarRequest(
            Long pacienteId,
            String motivoCancelacion,
            String motivo,
            List<String> canales
    ) {}

    record CancelarMedicoRequest(String motivoCancelacion, List<String> canales) {}

    record EstadoRequest(String estado) {}

    record ReprogramarRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nuevaFecha,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nuevaHora
    ) {}
}

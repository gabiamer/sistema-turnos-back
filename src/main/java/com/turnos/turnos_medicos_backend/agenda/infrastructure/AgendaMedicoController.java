// /agenda/infrastructure/AgendaMedicoController.java
package com.turnos.turnos_medicos_backend.agenda.infrastructure;

import com.turnos.turnos_medicos_backend.agenda.application.AgendaMedicoService;
import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.model.BloqueoDia;
import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class AgendaMedicoController {

    private final AgendaMedicoService agendaMedicoService;
    private final MedicoRepository medicoRepository;

    public AgendaMedicoController(AgendaMedicoService agendaMedicoService,
                                   MedicoRepository medicoRepository) {
        this.agendaMedicoService = agendaMedicoService;
        this.medicoRepository = medicoRepository;
    }

    // ── Agenda ──────────────────────────────────────────────────────────────

    @PostMapping("/api/agendas")
    public ResponseEntity<AgendaMedico> guardarAgenda(@RequestBody AgendaMedico agenda) {
        return ResponseEntity.ok(agendaMedicoService.guardarConfig(agenda));
    }

    @GetMapping("/api/medicos/{medicoId}/agenda")
    public ResponseEntity<List<AgendaMedico>> getAgendaByMedico(@PathVariable Long medicoId) {
        return ResponseEntity.ok(agendaMedicoService.findByMedicoId(medicoId));
    }

    /**
     * PUT /api/medicos/{id}/agenda
     * Reemplaza la agenda del médico.
     * Responde 409 si hay turnos CONFIRMADOS afectados (esa lógica la controla Ana desde TurnoRepository;
     * aquí se devuelve 409 si el servicio lanza ConflictoAgendaException).
     */
    @PutMapping("/api/medicos/{medicoId}/agenda")
    public ResponseEntity<?> actualizarAgenda(
            @PathVariable Long medicoId,
            @RequestBody List<AgendaMedico> nuevaAgenda) {

        Medico medico = medicoRepository.findById(medicoId).orElse(null);
        if (medico == null) {
            return ResponseEntity.notFound().build();
        }

        nuevaAgenda.forEach(a -> a.setMedico(medico));

        List<AgendaMedico> resultado = agendaMedicoService.actualizarAgenda(medicoId, nuevaAgenda);
        return ResponseEntity.ok(resultado);
    }

    // ── Bloqueos ─────────────────────────────────────────────────────────────

    @GetMapping("/api/medicos/{medicoId}/bloqueos")
    public ResponseEntity<List<BloqueoDia>> getBloqueos(@PathVariable Long medicoId) {
        return ResponseEntity.ok(agendaMedicoService.findBloqueosByMedicoId(medicoId));
    }

    /**
     * POST /api/medicos/{id}/bloqueos
     * Crea un bloqueo de días para el médico.
     * (El 409 por turnos confirmados lo gestiona Ana desde su dominio /turno/)
     */
    @PostMapping("/api/medicos/{medicoId}/bloqueos")
    public ResponseEntity<?> crearBloqueo(
            @PathVariable Long medicoId,
            @RequestBody BloqueoDia bloqueo) {

        Medico medico = medicoRepository.findById(medicoId).orElse(null);
        if (medico == null) {
            return ResponseEntity.notFound().build();
        }

        bloqueo.setMedico(medico);
        return ResponseEntity.ok(agendaMedicoService.guardarBloqueo(bloqueo));
    }
}
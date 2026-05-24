package com.turnos.turnos_medicos_backend.agenda.infrastructure;

import com.turnos.turnos_medicos_backend.agenda.application.AgendaMedicoService;
import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agendas")
@CrossOrigin(origins = "*")
public class AgendaMedicoController {

    private final AgendaMedicoService agendaMedicoService;

    public AgendaMedicoController(AgendaMedicoService agendaMedicoService) {
        this.agendaMedicoService = agendaMedicoService;
    }

    @PostMapping
    public ResponseEntity<AgendaMedico> guardarAgenda(
            @RequestBody AgendaMedico agenda) {

        return ResponseEntity.ok(
                agendaMedicoService.guardarConfig(agenda)
        );
    }

    @GetMapping("/medico/{medicoId}")
    public ResponseEntity<List<AgendaMedico>> getAgendaByMedico(
            @PathVariable Long medicoId) {

        return ResponseEntity.ok(
                agendaMedicoService.findByMedicoId(medicoId)
        );
    }
}
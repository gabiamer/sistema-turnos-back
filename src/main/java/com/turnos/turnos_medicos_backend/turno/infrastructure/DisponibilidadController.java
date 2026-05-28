package com.turnos.turnos_medicos_backend.turno.infrastructure;

import com.turnos.turnos_medicos_backend.turno.application.DisponibilidadService;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotDTO;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotMedicoDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    /**
     * GET /api/medicos/{medicoId}/disponibilidad?semana=YYYY-MM-DD
     * Endpoint original — usado por la vista del paciente (BuscarMedico).
     * Devuelve { fecha, hora, disponible, bloqueado } sin datos de paciente.
     */
    @GetMapping("/api/medicos/{medicoId}/disponibilidad")
    public ResponseEntity<?> getDisponibilidad(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {

        List<SlotDTO> slots = disponibilidadService.generarSlots(medicoId, semana);
        if (slots.isEmpty()) return ResponseEntity.ok(List.of());

        List<SlotDTO> resultado = disponibilidadService.filtrarOcupados(medicoId, slots);
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /api/medicos/{medicoId}/agenda-semana?semana=YYYY-MM-DD
     * Nuevo endpoint para la vista del médico.
     * Devuelve slots enriquecidos con datos del turno y paciente:
     * { fecha, hora, disponible, bloqueado, turno?: { id, estado, paciente: { id, nombre, apellido, ci } } }
     */
    @GetMapping("/api/medicos/{medicoId}/agenda-semana")
    public ResponseEntity<List<SlotMedicoDTO>> getAgendaSemana(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {

        List<SlotMedicoDTO> slots = disponibilidadService.generarSlotsParaMedico(medicoId, semana);
        return ResponseEntity.ok(slots);
    }
}
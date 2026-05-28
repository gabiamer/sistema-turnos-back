package com.turnos.turnos_medicos_backend.turno.infrastructure;

import com.turnos.turnos_medicos_backend.turno.application.DisponibilidadService;
import com.turnos.turnos_medicos_backend.turno.domain.model.AgendaSlotDTO;
import com.turnos.turnos_medicos_backend.turno.domain.model.SlotDTO;
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

    @GetMapping("/api/medicos/{medicoId}/disponibilidad")
    public ResponseEntity<?> getDisponibilidad(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {

        List<SlotDTO> slots = disponibilidadService.generarSlots(medicoId, semana);

        if (slots.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<SlotDTO> resultado = disponibilidadService.filtrarOcupados(medicoId, slots);
        return ResponseEntity.ok(resultado);
    }

    /** Vista del médico: slots enriquecidos con turno + paciente */
    @GetMapping("/api/medicos/{medicoId}/agenda-semana")
    public ResponseEntity<List<AgendaSlotDTO>> getAgendaSemana(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {

        List<AgendaSlotDTO> slots = disponibilidadService.generarAgendaSemana(medicoId, semana);
        return ResponseEntity.ok(slots);
    }
}
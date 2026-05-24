package com.turnos.turnos_medicos_backend.turno.infrastructure;

import com.turnos.turnos_medicos_backend.turno.application.DisponibilidadService;
import com.turnos.turnos_medicos_backend.turno.application.DisponibilidadService.SlotDTO;
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
     * GET /api/medicos/{id}/disponibilidad?semana=YYYY-MM-DD
     *
     * Devuelve todos los slots de la semana con su estado:
     * {
     *   "fecha": "2025-05-12",
     *   "hora": "09:00",
     *   "disponible": true,
     *   "bloqueado": false
     * }
     *
     * - disponible=false → slot ocupado por un turno confirmado/pendiente
     * - bloqueado=true   → BloqueoDia activo para esa fecha (pinta en gris en el front)
     */
    @GetMapping("/api/medicos/{medicoId}/disponibilidad")
    public ResponseEntity<?> getDisponibilidad(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {

        List<SlotDTO> slots = disponibilidadService.generarSlots(medicoId, semana);

        if (slots.isEmpty()) {
            return ResponseEntity.ok(List.of()); // 200 lista vacía → front muestra "Sin disponibilidad"
        }

        List<SlotDTO> resultado = disponibilidadService.filtrarOcupados(medicoId, slots);
        return ResponseEntity.ok(resultado);
    }
}
package com.turnos.turnos_medicos_backend.medico.infrastructure;

import com.turnos.turnos_medicos_backend.medico.application.MedicoService;
import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicos")
@CrossOrigin(origins = "*") // Permite al front conectarse sin errores de CORS
public class MedicoController {

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @GetMapping
    public ResponseEntity<List<Medico>> getMedicos(@RequestParam(required = false) String especialidad) {
        if (especialidad != null && !especialidad.isEmpty()) {
            return ResponseEntity.ok(medicoService.findByEspecialidad(especialidad));
        }
        return ResponseEntity.ok(medicoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medico> getMedicoById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(medicoService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
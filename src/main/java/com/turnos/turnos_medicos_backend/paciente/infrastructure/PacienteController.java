package com.turnos.turnos_medicos_backend.paciente.infrastructure;

import com.turnos.turnos_medicos_backend.paciente.application.PacienteService;
import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    // POST /api/pacientes
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Paciente paciente) {
        try {
            Paciente nuevo = pacienteService.registrar(paciente);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/pacientes?ci=12345
    @GetMapping
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) String ci,
            @RequestParam(required = false) String nombre) {

        try {
            if (ci != null) {
                return ResponseEntity.ok(pacienteService.buscarPorCi(ci));
            }
            if (nombre != null) {
                List<Paciente> resultado = pacienteService.buscarPorNombre(nombre);
                return ResponseEntity.ok(resultado);
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe enviar ci o nombre como parámetro"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
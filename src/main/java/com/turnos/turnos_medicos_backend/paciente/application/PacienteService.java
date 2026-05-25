package com.turnos.turnos_medicos_backend.paciente.application;

import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.paciente.domain.port.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public Paciente registrar(Paciente paciente) {
        pacienteRepository.findByCi(paciente.getCi()).ifPresent(p -> {
            throw new IllegalArgumentException("Ya existe un paciente con ese CI");
        });
        return pacienteRepository.save(paciente);
    }

    public Paciente buscarPorCi(String ci) {
        return pacienteRepository.findByCi(ci)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
    }

    public List<Paciente> buscarPorNombre(String nombre) {
        return pacienteRepository.findByNombreContaining(nombre);
    }

    public Paciente buscarPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
    }
}
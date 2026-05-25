package com.turnos.turnos_medicos_backend.medico.application;

import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    public List<Medico> findAll() {
        return medicoRepository.findAll();
    }

    public Medico findById(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));
    }

    public List<Medico> findByEspecialidad(String especialidad) {
        return medicoRepository.findByEspecialidad(especialidad);
    }
}
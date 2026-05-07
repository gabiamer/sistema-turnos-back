package com.turnos.turnos_medicos_backend.agenda.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgendaMedicoService {

    private final AgendaMedicoRepository agendaMedicoRepository;

    public AgendaMedicoService(AgendaMedicoRepository agendaMedicoRepository) {
        this.agendaMedicoRepository = agendaMedicoRepository;
    }

    public AgendaMedico guardarConfig(AgendaMedico agenda) {
        return agendaMedicoRepository.save(agenda);
    }

    public List<AgendaMedico> findByMedicoId(Long medicoId) {
        return agendaMedicoRepository.findByMedicoId(medicoId);
    }
}
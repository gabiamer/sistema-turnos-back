package com.turnos.turnos_medicos_backend.agenda.domain.port;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;

import java.util.List;
import java.util.Optional;

public interface AgendaMedicoRepository {

    AgendaMedico save(AgendaMedico agendaMedico);

    Optional<AgendaMedico> findById(Long id);

    List<AgendaMedico> findAll();

    List<AgendaMedico> findByMedicoId(Long medicoId);
}
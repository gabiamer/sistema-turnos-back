package com.turnos.turnos_medicos_backend.agenda.infrastructure;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;
import com.turnos.turnos_medicos_backend.agenda.domain.port.AgendaMedicoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgendaMedicoRepositoryImpl
        extends JpaRepository<AgendaMedico, Long>, AgendaMedicoRepository {

    List<AgendaMedico> findByMedicoId(Long medicoId);
}
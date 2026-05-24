package com.turnos.turnos_medicos_backend.medico.infrastructure;

import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import com.turnos.turnos_medicos_backend.medico.domain.port.MedicoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicoRepositoryImpl
        extends JpaRepository<Medico, Long>, MedicoRepository {

    List<Medico> findByEspecialidad(String especialidad);
}
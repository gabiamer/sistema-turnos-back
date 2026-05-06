package com.turnos.turnos_medicos_backend.paciente.infrastructure;

import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import com.turnos.turnos_medicos_backend.paciente.domain.port.PacienteRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepositoryImpl extends JpaRepository<Paciente, Long>, PacienteRepository {

    Optional<Paciente> findByCi(String ci);

    List<Paciente> findByNombreContainingIgnoreCase(String nombre);
}
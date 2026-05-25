package com.turnos.turnos_medicos_backend.paciente.domain.port;

import com.turnos.turnos_medicos_backend.paciente.domain.model.Paciente;
import java.util.List;
import java.util.Optional;

public interface PacienteRepository {

    // Guarda un paciente nuevo o actualiza uno existente
    Paciente save(Paciente paciente);

    // Busca por ID interno
    Optional<Paciente> findById(Long id);

    // Busca por CI (para el login y evitar duplicados)
    Optional<Paciente> findByCi(String ci);

    // Busca por nombre (búsqueda parcial en el front)
    List<Paciente> findByNombreContaining(String nombre);
}
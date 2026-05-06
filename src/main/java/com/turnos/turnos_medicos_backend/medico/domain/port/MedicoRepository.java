package com.turnos.turnos_medicos_backend.medico.domain.port;

import com.turnos.turnos_medicos_backend.medico.domain.model.Medico;
import java.util.List;
import java.util.Optional;

public interface MedicoRepository {

    // Guarda un médico nuevo o actualiza uno existente
    Medico save(Medico medico);

    // Busca un médico por su ID (necesario para ver su disponibilidad y agenda)
    Optional<Medico> findById(Long id);

    // Devuelve la lista de todos los médicos
    List<Medico> findAll();

    // Devuelve los médicos filtrados por una especialidad específica
    List<Medico> findByEspecialidad(String especialidad);
}
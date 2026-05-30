package com.turnos.turnos_medicos_backend.secretaria.domain.port;

import com.turnos.turnos_medicos_backend.secretaria.domain.model.Recepcionista;
import java.util.List;
import java.util.Optional;

public interface RecepcionistaRepository {

    Optional<Recepcionista> findById(Long id);

    Recepcionista save(Recepcionista r);

    List<Recepcionista> findAll();
}

package com.turnos.turnos_medicos_backend.secretaria.infrastructure;

import com.turnos.turnos_medicos_backend.secretaria.domain.model.Recepcionista;
import com.turnos.turnos_medicos_backend.secretaria.domain.port.RecepcionistaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecepcionistaRepositoryImpl
        extends JpaRepository<Recepcionista, Long>, RecepcionistaRepository {
}

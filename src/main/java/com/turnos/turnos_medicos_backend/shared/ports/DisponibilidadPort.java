package com.turnos.turnos_medicos_backend.shared.ports;

import java.time.LocalDate;
import java.time.LocalTime;

public interface DisponibilidadPort {

    // Retorna true si el médico tiene ese slot en su agenda
    // y no está bloqueado por un BloqueoDia
    // Alex implementará esto en /shared/adapters/
    boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora);
}
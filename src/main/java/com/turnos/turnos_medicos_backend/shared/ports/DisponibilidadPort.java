package com.turnos.turnos_medicos_backend.shared.ports;

import java.time.LocalDate;
import java.time.LocalTime;

public interface DisponibilidadPort {

    boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora);

    boolean estaBloqueado(Long medicoId, LocalDate fecha);
}
package com.turnos.turnos_medicos_backend.shared.ports;

public interface IEventPublisher {
    void publicar(String evento, Object payload);
}
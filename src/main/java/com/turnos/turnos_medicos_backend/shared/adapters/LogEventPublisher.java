package com.turnos.turnos_medicos_backend.shared.adapters;

import com.turnos.turnos_medicos_backend.shared.ports.IEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogEventPublisher implements IEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogEventPublisher.class);

    @Override
    public void publicar(String evento, Object payload) {
        log.info("[EVENT] {} → {}", evento, payload);
    }
}
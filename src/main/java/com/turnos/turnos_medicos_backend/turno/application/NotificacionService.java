package com.turnos.turnos_medicos_backend.turno.application;

import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    public interface NotificacionStrategy {
        String canal();
        void notificar(String telefono, String email, String motivo);
    }

    @Service
    public static class WhatsAppLog implements NotificacionStrategy {
        private static final Logger log = LoggerFactory.getLogger(WhatsAppLog.class);

        @Override
        public String canal() { return "WHATSAPP"; }

        @Override
        public void notificar(String telefono, String email, String motivo) {
            log.info("[NOTIF][WhatsApp] → {} · Cita cancelada: {}", telefono, motivo);
        }
    }

    @Service
    public static class SmsLog implements NotificacionStrategy {
        private static final Logger log = LoggerFactory.getLogger(SmsLog.class);

        @Override
        public String canal() { return "SMS"; }

        @Override
        public void notificar(String telefono, String email, String motivo) {
            log.info("[NOTIF][SMS] → {} · Cita cancelada: {}", telefono, motivo);
        }
    }

    @Service
    public static class EmailLog implements NotificacionStrategy {
        private static final Logger log = LoggerFactory.getLogger(EmailLog.class);

        @Override
        public String canal() { return "EMAIL"; }

        @Override
        public void notificar(String telefono, String email, String motivo) {
            log.info("[NOTIF][Email] → {} · Cita cancelada: {}", email, motivo);
        }
    }

    private final Map<String, NotificacionStrategy> strategies;

    public NotificacionService(List<NotificacionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(s -> s.canal().toUpperCase(), s -> s));
    }

    public void notificar(Turno turno, List<String> canales, String motivo) {
        if (canales == null || canales.isEmpty()) return;
        String telefono = turno.getPaciente().getTelefono();
        String email = turno.getPaciente().getEmail();
        canales.forEach(canal -> {
            NotificacionStrategy strategy = strategies.get(canal.toUpperCase());
            if (strategy != null) {
                strategy.notificar(telefono, email, motivo);
            }
        });
    }
}

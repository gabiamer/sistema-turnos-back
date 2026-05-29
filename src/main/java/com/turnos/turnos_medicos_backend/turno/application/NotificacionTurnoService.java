package com.turnos.turnos_medicos_backend.turno.application;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionTurnoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTurnoService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${twilio.account-sid:}")
    private String twilioSid;

    @Value("${twilio.auth-token:}")
    private String twilioToken;

    @Value("${twilio.sms.from:}")
    private String smsFrom;

    @Value("${twilio.whatsapp.from:}")
    private String whatsappFrom;

    public NotificacionTurnoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    private void initTwilio() {
        if (!twilioSid.isBlank() && !twilioToken.isBlank()) {
            Twilio.init(twilioSid, twilioToken);
            log.info("[TWILIO] Inicializado correctamente.");
        } else {
            log.warn("[TWILIO] Credenciales no configuradas — SMS y WhatsApp desactivados.");
        }
    }

    public void notificarCancelacion(Turno turno, String motivo, List<String> canales) {
        String cuerpo = armarMensaje(turno, motivo);
        for (String canal : canales) {
            switch (canal.toUpperCase()) {
                case "EMAIL"    -> enviarEmail(turno, motivo);
                case "SMS"      -> enviarSms(turno, cuerpo);
                case "WHATSAPP" -> enviarWhatsApp(turno, cuerpo);
                default         -> log.warn("[NOTIF] Canal desconocido: {}", canal);
            }
        }
    }

    // ── Email ─────────────────────────────────────────────────────────────────

    private void enviarEmail(Turno turno, String motivo) {
        if (mailUsername.isBlank()) {
            log.warn("[NOTIF][EMAIL] MAIL_USERNAME no configurado — email no enviado.");
            return;
        }

        String nombre = turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido();
        String medico = "Dr/a. " + turno.getMedico().getNombre() + " " + turno.getMedico().getApellido();

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailUsername);
        msg.setTo(turno.getPaciente().getEmail());
        msg.setSubject("Cancelación de turno médico — " + turno.getFecha());
        msg.setText("""
                Estimado/a %s,

                Le informamos que su turno ha sido cancelado.

                  Médico:  %s (%s)
                  Fecha:   %s
                  Hora:    %s
                  Motivo:  %s

                Si tiene dudas, comuníquese con el consultorio.

                Sistema de Turnos Médicos
                """.formatted(nombre, medico, turno.getMedico().getEspecialidad(),
                turno.getFecha(), turno.getHora(), motivo));

        try {
            mailSender.send(msg);
            log.info("[NOTIF][EMAIL] Enviado a {} (turno {})", turno.getPaciente().getEmail(), turno.getId());
        } catch (Exception e) {
            log.error("[NOTIF][EMAIL] Error al enviar a {}: {}", turno.getPaciente().getEmail(), e.getMessage());
        }
    }

    // ── SMS ───────────────────────────────────────────────────────────────────

    private void enviarSms(Turno turno, String cuerpo) {
        if (twilioSid.isBlank() || smsFrom.isBlank()) {
            log.warn("[NOTIF][SMS] Twilio no configurado — SMS no enviado.");
            return;
        }

        String telefono = turno.getPaciente().getTelefono();
        if (telefono == null || telefono.isBlank()) {
            log.warn("[NOTIF][SMS] El paciente {} no tiene teléfono registrado.", turno.getPaciente().getId());
            return;
        }

        try {
            Message.creator(new PhoneNumber(telefono), new PhoneNumber(smsFrom), cuerpo).create();
            log.info("[NOTIF][SMS] Enviado a {} (turno {})", telefono, turno.getId());
        } catch (Exception e) {
            log.error("[NOTIF][SMS] Error al enviar a {}: {}", telefono, e.getMessage());
        }
    }

    // ── WhatsApp ──────────────────────────────────────────────────────────────

    private void enviarWhatsApp(Turno turno, String cuerpo) {
        if (twilioSid.isBlank() || whatsappFrom.isBlank()) {
            log.warn("[NOTIF][WHATSAPP] Twilio no configurado — WhatsApp no enviado.");
            return;
        }

        String telefono = turno.getPaciente().getTelefono();
        if (telefono == null || telefono.isBlank()) {
            log.warn("[NOTIF][WHATSAPP] El paciente {} no tiene teléfono registrado.", turno.getPaciente().getId());
            return;
        }

        // Twilio exige el prefijo "whatsapp:" en ambos extremos
        String waTo   = telefono.startsWith("whatsapp:") ? telefono : "whatsapp:" + telefono;
        String waFrom = whatsappFrom.startsWith("whatsapp:") ? whatsappFrom : "whatsapp:" + whatsappFrom;

        try {
            Message.creator(new PhoneNumber(waTo), new PhoneNumber(waFrom), cuerpo).create();
            log.info("[NOTIF][WHATSAPP] Enviado a {} (turno {})", telefono, turno.getId());
        } catch (Exception e) {
            log.error("[NOTIF][WHATSAPP] Error al enviar a {}: {}", telefono, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String armarMensaje(Turno turno, String motivo) {
        return "Turno cancelado: Dr/a. %s el %s a las %s. Motivo: %s".formatted(
                turno.getMedico().getApellido(),
                turno.getFecha(),
                turno.getHora(),
                motivo);
    }
}

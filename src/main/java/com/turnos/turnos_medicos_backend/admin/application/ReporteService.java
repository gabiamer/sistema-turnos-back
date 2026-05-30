package com.turnos.turnos_medicos_backend.admin.application;

import com.turnos.turnos_medicos_backend.admin.domain.model.*;
import com.turnos.turnos_medicos_backend.turno.domain.model.EstadoTurno;
import com.turnos.turnos_medicos_backend.turno.domain.model.Turno;
import com.turnos.turnos_medicos_backend.turno.domain.port.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class ReporteService {

    private static final int MAX_DIAS_RANGO = 90;

    private final TurnoRepository turnoRepository;

    public ReporteService(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    // ── KPIs del día ─────────────────────────────────────────────────────────

    public KpisDiaDTO kpisDelDia(LocalDate fecha) {
        List<Turno> turnos = turnoRepository.findByFecha(fecha);
        return new KpisDiaDTO(
                contarPorEstado(turnos, EstadoTurno.CONFIRMADO),
                contarPorEstado(turnos, EstadoTurno.CANCELADO),
                contarPorEstado(turnos, EstadoTurno.CONCLUIDA),
                contarAusentes(turnos)
        );
    }

    // ── Reporte de ocupación ──────────────────────────────────────────────────

    public List<OcupacionDTO> reporteOcupacion(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        List<Turno> turnos = turnoRepository.findByFechaBetween(inicio, fin);

        Map<Long, List<Turno>> porMedico = turnos.stream()
                .collect(Collectors.groupingBy(t -> t.getMedico().getId()));

        return porMedico.entrySet().stream().map(entry -> {
            List<Turno> del = entry.getValue();
            Turno primero = del.get(0);
            long ocupados = del.stream()
                    .filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO
                              || t.getEstado() == EstadoTurno.CONCLUIDA)
                    .count();
            long total = del.size();
            double pct = total > 0 ? (double) ocupados / total * 100.0 : 0.0;
            return new OcupacionDTO(
                    primero.getMedico().getId(),
                    primero.getMedico().getNombre() + " " + primero.getMedico().getApellido(),
                    primero.getMedico().getEspecialidad(),
                    total,
                    ocupados,
                    Math.round(pct * 10.0) / 10.0
            );
        }).toList();
    }

    // ── Reporte de ausentismo ─────────────────────────────────────────────────

    public List<AusentismoDTO> reporteAusentismo(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        List<Turno> turnos = turnoRepository.findByFechaBetween(inicio, fin);

        return turnos.stream()
                .filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO
                          && t.getFecha().isBefore(LocalDate.now()))
                .map(t -> new AusentismoDTO(
                        t.getId(),
                        t.getFecha(),
                        t.getMedico().getNombre() + " " + t.getMedico().getApellido(),
                        t.getMedico().getEspecialidad()
                ))
                .toList();
    }

    // ── Reporte por especialidad ──────────────────────────────────────────────

    public List<EspecialidadDTO> reporteEspecialidades(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        List<Turno> turnos = turnoRepository.findByFechaBetween(inicio, fin);

        Map<String, Long> agrupado = turnos.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getMedico().getEspecialidad(),
                        Collectors.counting()
                ));

        return agrupado.entrySet().stream()
                .map(e -> new EspecialidadDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(EspecialidadDTO::totalTurnos).reversed())
                .toList();
    }

    // ── Reporte de cancelaciones ──────────────────────────────────────────────

    public List<CancelacionDTO> reporteCancelaciones(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        List<Turno> turnos = turnoRepository.findByFechaBetween(inicio, fin);

        return turnos.stream()
                .filter(t -> t.getEstado() == EstadoTurno.CANCELADO)
                .map(t -> new CancelacionDTO(
                        t.getId(),
                        t.getFecha(),
                        t.getMedico().getEspecialidad(),
                        anonimizarPaciente(t.getPaciente().getCi()),
                        t.getMotivoCancelacion()
                ))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long contarPorEstado(List<Turno> turnos, EstadoTurno estado) {
        return turnos.stream().filter(t -> t.getEstado() == estado).count();
    }

    private long contarAusentes(List<Turno> turnos) {
        LocalDate hoy = LocalDate.now();
        return turnos.stream()
                .filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO
                          && t.getFecha().isBefore(hoy))
                .count();
    }

    private String anonimizarPaciente(String ci) {
        if (ci == null || ci.length() <= 3) return "***";
        return ci.substring(0, 2) + "***" + ci.substring(ci.length() - 1);
    }

    public void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new RangoInvalidoException("Las fechas inicio y fin son obligatorias");
        }
        if (fin.isBefore(inicio)) {
            throw new RangoInvalidoException("La fecha fin debe ser igual o posterior a inicio");
        }
        if (inicio.until(fin).getDays() > MAX_DIAS_RANGO) {
            throw new RangoInvalidoException("El rango no puede superar los " + MAX_DIAS_RANGO + " días");
        }
    }

    // ── Excepción de dominio ──────────────────────────────────────────────────

    public static class RangoInvalidoException extends RuntimeException {
        public RangoInvalidoException(String msg) { super(msg); }
    }
}
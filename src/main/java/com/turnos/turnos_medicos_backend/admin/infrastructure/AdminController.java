package com.turnos.turnos_medicos_backend.admin.infrastructure;

import com.turnos.turnos_medicos_backend.admin.application.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final ReporteService reporteService;

    public AdminController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // ── Guard de rol ─────────────────────────────────────────────────────────

    private boolean esAdministrativo(String rol) {
        return "ADMINISTRATIVO".equals(rol);
    }

    // ── GET /api/admin/kpis?fecha= ────────────────────────────────────────────

    @GetMapping("/kpis")
    public ResponseEntity<?> kpisDelDia(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (!esAdministrativo(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        return ResponseEntity.ok(reporteService.kpisDelDia(fechaConsulta));
    }

    // ── GET /api/admin/reportes/ocupacion?inicio=&fin= ────────────────────────

    @GetMapping("/reportes/ocupacion")
    public ResponseEntity<?> reporteOcupacion(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        if (!esAdministrativo(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            return ResponseEntity.ok(reporteService.reporteOcupacion(inicio, fin));
        } catch (ReporteService.RangoInvalidoException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/admin/reportes/ausentismo?inicio=&fin= ───────────────────────

    @GetMapping("/reportes/ausentismo")
    public ResponseEntity<?> reporteAusentismo(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        if (!esAdministrativo(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            return ResponseEntity.ok(reporteService.reporteAusentismo(inicio, fin));
        } catch (ReporteService.RangoInvalidoException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/admin/reportes/especialidades?inicio=&fin= ──────────────────

    @GetMapping("/reportes/especialidades")
    public ResponseEntity<?> reporteEspecialidades(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        if (!esAdministrativo(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            return ResponseEntity.ok(reporteService.reporteEspecialidades(inicio, fin));
        } catch (ReporteService.RangoInvalidoException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/admin/reportes/cancelaciones?inicio=&fin= ───────────────────

    @GetMapping("/reportes/cancelaciones")
    public ResponseEntity<?> reporteCancelaciones(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        if (!esAdministrativo(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            return ResponseEntity.ok(reporteService.reporteCancelaciones(inicio, fin));
        } catch (ReporteService.RangoInvalidoException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/admin/reportes/exportar?tipo=&inicio=&fin= → CSV ────────────

    @GetMapping("/reportes/exportar")
    public ResponseEntity<?> exportarCSV(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestParam String tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        if (!esAdministrativo(rol)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        }
        try {
            String csv = generarCSV(tipo, inicio, fin);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"reporte-" + tipo + "-" + inicio + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        } catch (ReporteService.RangoInvalidoException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (TipoReporteInvalidoException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── CSV builder ───────────────────────────────────────────────────────────

    private String generarCSV(String tipo, LocalDate inicio, LocalDate fin) {
        return switch (tipo) {
            case "ocupacion" -> {
                var rows = reporteService.reporteOcupacion(inicio, fin);
                StringBuilder sb = new StringBuilder("medico,especialidad,totalSlots,ocupados,porcentaje\n");
                rows.forEach(r -> sb.append(String.format("%s,%s,%d,%d,%.1f\n",
                        r.nombreMedico(), r.especialidad(), r.totalSlots(), r.ocupados(), r.porcentaje())));
                yield sb.toString();
            }
            case "ausentismo" -> {
                var rows = reporteService.reporteAusentismo(inicio, fin);
                StringBuilder sb = new StringBuilder("turnoId,fecha,medico,especialidad\n");
                rows.forEach(r -> sb.append(String.format("%d,%s,%s,%s\n",
                        r.turnoId(), r.fecha(), r.nombreMedico(), r.especialidad())));
                yield sb.toString();
            }
            case "especialidades" -> {
                var rows = reporteService.reporteEspecialidades(inicio, fin);
                StringBuilder sb = new StringBuilder("especialidad,totalTurnos\n");
                rows.forEach(r -> sb.append(String.format("%s,%d\n", r.especialidad(), r.totalTurnos())));
                yield sb.toString();
            }
            case "cancelaciones" -> {
                var rows = reporteService.reporteCancelaciones(inicio, fin);
                StringBuilder sb = new StringBuilder("turnoId,fecha,especialidad,paciente,motivo\n");
                rows.forEach(r -> sb.append(String.format("%d,%s,%s,%s,\"%s\"\n",
                        r.turnoId(), r.fecha(), r.especialidad(),
                        r.pacienteAnonimizado(),
                        r.motivo() != null ? r.motivo().replace("\"", "\"\"") : "")));
                yield sb.toString();
            }
            default -> throw new TipoReporteInvalidoException("Tipo de reporte desconocido: " + tipo);
        };
    }

    // ── Excepción local ───────────────────────────────────────────────────────

    static class TipoReporteInvalidoException extends RuntimeException {
        TipoReporteInvalidoException(String msg) { super(msg); }
    }
}
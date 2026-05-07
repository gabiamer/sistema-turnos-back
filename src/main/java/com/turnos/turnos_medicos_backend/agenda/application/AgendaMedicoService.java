package com.turnos.turnos_medicos_backend.agenda.application;

import com.turnos.turnos_medicos_backend.agenda.domain.model.AgendaMedico;

import org.springframework.stereotype.Service;

@Service
public class AgendaMedicoService {
    
    // luego inyectaremos el puerto de la Agenda
    
    public void guardarConfig(AgendaMedico  agenda) {
        // Lógica base
        System.out.println("Guardando configuración de agenda...");
    }
}
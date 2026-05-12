package Model;

import enums.EstadoIncidente;

import java.time.LocalDateTime;

public class HistorialEstado {
    private int id;
    private int incidenteId;
    private EstadoIncidente estadoAnterior;
    private EstadoIncidente estadoNuevo;
    private int usuarioId;
    private LocalDateTime fechaCambio;

    public HistorialEstado(int id, int incidenteId, EstadoIncidente estadoAnterior, EstadoIncidente estadoNuevo, int usuarioId, LocalDateTime fechaCambio) {
        this.id = id;
        this.incidenteId = incidenteId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuarioId = usuarioId;
        this.fechaCambio = fechaCambio;
    }

    public int getId() {
        return id;
    }

    public int getIncidenteId() {
        return incidenteId;
    }

    public EstadoIncidente getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoIncidente getEstadoNuevo() {
        return estadoNuevo;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIncidenteId(int incidenteId) {
        this.incidenteId = incidenteId;
    }

    public void setEstadoAnterior(EstadoIncidente estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public void setEstadoNuevo(EstadoIncidente estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }
}
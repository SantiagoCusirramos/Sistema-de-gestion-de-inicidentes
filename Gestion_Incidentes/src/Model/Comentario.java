package Model;

import java.time.LocalDateTime;

public class Comentario {
    private int id;
    private int incidenteId;
    private int usuarioId;
    private String mensaje;
    private LocalDateTime fecha;

    public Comentario(int id, int incidenteId, int usuarioId, String mensaje, LocalDateTime fecha) {
        this.id = id;
        this.incidenteId = incidenteId;
        this.usuarioId = usuarioId;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public int getIncidenteId() {
        return incidenteId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIncidenteId(int incidenteId) {
        this.incidenteId = incidenteId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
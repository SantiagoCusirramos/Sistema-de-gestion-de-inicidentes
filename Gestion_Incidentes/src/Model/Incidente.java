package Model;

import enums.EstadoIncidente;
import enums.Prioridad;

import java.time.LocalDateTime;

public class Incidente {
    private int id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private int usuarioId;
    private Integer tecnicoId;
    private EstadoIncidente estado;
    private Prioridad prioridad;

    public Incidente() {
    }

    public Incidente(int id, String titulo, String descripcion, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion, int usuarioId, Integer tecnicoId, EstadoIncidente estado, Prioridad prioridad) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
        this.usuarioId = usuarioId;
        this.tecnicoId = tecnicoId;
        this.estado = estado;
        this.prioridad = prioridad;
    }

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public Integer getTecnicoId() {
        return tecnicoId;
    }

    public EstadoIncidente getEstado() {
        return estado;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void setTecnicoId(Integer tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public void setEstado(EstadoIncidente estado) {
        this.estado = estado;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    @Override
    public String toString() {
        return "Incidente{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaActualizacion=" + fechaActualizacion +
                ", usuarioId=" + usuarioId +
                ", tecnicoId=" + tecnicoId +
                ", estado=" + estado +
                ", prioridad=" + prioridad +
                '}';
    }
}
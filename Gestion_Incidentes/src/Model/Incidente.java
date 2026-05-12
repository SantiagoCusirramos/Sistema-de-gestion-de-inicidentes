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

    // getters, setters, constructores
}
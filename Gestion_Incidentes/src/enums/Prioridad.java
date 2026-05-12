package enums;

public enum Prioridad {
    BAJA(1, "Baja prioridad"),
    MEDIA(2, "Prioridad media"),
    ALTA(3, "Alta prioridad"),
    CRITICA(4, "Crítica - acción inmediata");

    private int nivel;
    private String descripcion;

    Prioridad(int nivel, String descripcion) {
        this.nivel = nivel;
        this.descripcion = descripcion;
    }

    public int getNivel() { return nivel; }
    public String getDescripcion() { return descripcion; }
}
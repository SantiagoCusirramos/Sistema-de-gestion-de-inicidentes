package DAOs;

import Model.Incidente;
import enums.EstadoIncidente;

import java.util.List;

public interface IncidenteDAO {
    int crear(Incidente incidente);
    boolean actualizar(Incidente incidente);
    Incidente buscar(int id);
    List<Incidente> listarTodos();
    List<Incidente> listarPorUsuario(int usuarioId);
    List<Incidente> listarPorTecnico(int tecnicoId);
    List<Incidente> listarPorEstado(EstadoIncidente estado);
    boolean eliminar(int id);
}
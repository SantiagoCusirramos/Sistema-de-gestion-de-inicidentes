package DAOs;

import Model.Comentario;

import java.util.List;

public interface ComentarioDAO {
    int crear(Comentario comentario);
    List<Comentario> listarPorIncidente(int incidenteId);
    boolean eliminar(int id);
}
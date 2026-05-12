package Service;

import DAOs.ComentarioDAO;
import DAOs.IncidenteDAO;
import Model.Comentario;
import Model.Incidente;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;

import java.time.LocalDateTime;
import java.util.List;

public class IncidenteService {
    private IncidenteDAO incidenteDAO;
    private ComentarioDAO comentarioDAO;

    public Incidente crearIncidente(String titulo, String descripcion, int usuarioId, Prioridad prioridad) {
        Incidente inc = new Incidente();
        inc.setTitulo(titulo);
        inc.setDescripcion(descripcion);
        inc.setUsuarioId(usuarioId);
        inc.setPrioridad(prioridad);
        inc.setEstado(EstadoIncidente.PENDIENTE);
        inc.setFechaCreacion(LocalDateTime.now());

        int id = incidenteDAO.crear(inc);
        return incidenteDAO.buscar(id);
    }

    public boolean asignarTecnico(int incidenteId, int tecnicoId) {
        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) return false;

        inc.setTecnicoId(tecnicoId);
        inc.setEstado(EstadoIncidente.EN_PROCESO);
        return incidenteDAO.actualizar(inc);
    }

    public boolean resolverIncidente(int incidenteId, String solucion, int tecnicoId) {
        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null || inc.getTecnicoId() != tecnicoId) return false;

        Comentario comentario = new Comentario();
        comentario.setIncidenteId(incidenteId);
        comentario.setUsuarioId(tecnicoId);
        comentario.setMensaje("SOLUCIÓN: " + solucion);
        comentario.setFecha(LocalDateTime.now());
        comentarioDAO.crear(comentario);

        inc.setEstado(EstadoIncidente.RESUELTO);
        inc.setFechaActualizacion(LocalDateTime.now());
        return incidenteDAO.actualizar(inc);
    }

    public boolean cerrarIncidente(int incidenteId, int usuarioId) {
        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null || inc.getUsuarioId() != usuarioId) return false;

        inc.setEstado(EstadoIncidente.CERRADO);
        return incidenteDAO.actualizar(inc);
    }

    public List<Incidente> getIncidentesPorUsuario(int usuarioId, RolUsuario rol) {
        if (rol == RolUsuario.ADMIN) {
            return incidenteDAO.listarTodos();
        } else if (rol == RolUsuario.TECNICO) {
            return incidenteDAO.listarPorTecnico(usuarioId);
        } else {
            return incidenteDAO.listarPorUsuario(usuarioId);
        }
    }
}
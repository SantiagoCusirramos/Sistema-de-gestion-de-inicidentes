package Service;

import DAOs.ComentarioDAO;
import DAOs.IncidenteDAO;
import Model.Comentario;
import Model.Incidente;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IncidenteService {
    private IncidenteDAO incidenteDAO;
    private ComentarioDAO comentarioDAO;

    public IncidenteService() {
        this.incidenteDAO = new DAOs.Implementacion.IncidenteDAOImpl();
        this.comentarioDAO = new DAOs.Implementacion.ComentarioDAOImpl();
    }

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
        if (inc == null || inc.getTecnicoId() == null || inc.getTecnicoId() != tecnicoId) return false;

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

    public Incidente getIncidentePorId(int id) {
        return incidenteDAO.buscar(id);
    }

    public boolean agregarComentario(int incidenteId, int usuarioId, String mensaje) {
        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) return false;

        Comentario comentario = new Comentario();
        comentario.setIncidenteId(incidenteId);
        comentario.setUsuarioId(usuarioId);
        comentario.setMensaje(mensaje);
        comentario.setFecha(LocalDateTime.now());
        return comentarioDAO.crear(comentario) > 0;
    }

    public List<String> getComentarios(int incidenteId) {
        List<Comentario> comentarios = comentarioDAO.listarPorIncidente(incidenteId);
        List<String> resultado = new ArrayList<>();
        for (Comentario c : comentarios) {
            resultado.add("[" + c.getFecha() + "] Usuario " + c.getUsuarioId() + ": " + c.getMensaje());
        }
        return resultado;
    }

    public boolean cambiarEstado(int incidenteId, EstadoIncidente nuevoEstado, int usuarioId) {
        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) return false;

        inc.setEstado(nuevoEstado);
        inc.setFechaActualizacion(LocalDateTime.now());
        if (nuevoEstado == EstadoIncidente.RESUELTO) {
            Comentario comentario = new Comentario();
            comentario.setIncidenteId(incidenteId);
            comentario.setUsuarioId(usuarioId);
            comentario.setMensaje("Estado cambiado a: " + nuevoEstado);
            comentario.setFecha(LocalDateTime.now());
            comentarioDAO.crear(comentario);
        }
        return incidenteDAO.actualizar(inc);
    }
}
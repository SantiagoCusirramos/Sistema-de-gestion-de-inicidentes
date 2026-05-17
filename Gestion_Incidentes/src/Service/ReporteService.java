package Service;

import DAOs.IncidenteDAO;
import Model.Incidente;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Correcciones aplicadas:
 *  [BUG-017] Todos los métodos ahora aceptan usuarioId + rol para filtrar
 *             los datos según el contexto del usuario:
 *             - ADMIN   → ve estadísticas globales (todos los incidentes)
 *             - TECNICO → ve solo sus incidentes asignados
 *             - USUARIO → no debería acceder a reportes (la View ya lo bloquea,
 *                         pero el Service tampoco devuelve datos ajenos)
 */
public class ReporteService {

    private IncidenteDAO incidenteDAO;

    public ReporteService() {
        this.incidenteDAO = new DAOs.Implementacion.IncidenteDAOImpl();
    }

    // -------------------------------------------------------------------------
    // Métodos con filtro por rol [BUG-017]
    // -------------------------------------------------------------------------

    /**
     * Devuelve el total de incidentes en un estado dado, filtrado por rol.
     */
    public int getTotalIncidentesPorEstado(EstadoIncidente estado,
                                            int usuarioId, RolUsuario rol) {
        return (int) getIncidentesFiltrados(usuarioId, rol)
                     .stream()
                     .filter(i -> i.getEstado() == estado)
                     .count();
    }

    /**
     * Devuelve el conteo de incidentes por prioridad, filtrado por rol.
     */
    public Map<Prioridad, Integer> getIncidentesPorPrioridad(int usuarioId, RolUsuario rol) {
        Map<Prioridad, Integer> stats = new HashMap<>();
        for (Prioridad p : Prioridad.values()) {
            stats.put(p, 0);
        }
        for (Incidente inc : getIncidentesFiltrados(usuarioId, rol)) {
            stats.merge(inc.getPrioridad(), 1, Integer::sum);
        }
        return stats;
    }

    /**
     * Devuelve los incidentes más recientes, filtrado por rol.
     */
    public List<Incidente> getIncidentesRecientes(int limite, int usuarioId, RolUsuario rol) {
        List<Incidente> filtrados = getIncidentesFiltrados(usuarioId, rol);
        filtrados.sort((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()));
        return filtrados.stream().limit(limite).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Métodos legacy sin rol (mantienen compatibilidad con llamadas de ADMIN)
    // Internamente delegan a los métodos con rol usando ADMIN como contexto.
    // -------------------------------------------------------------------------

    /** @deprecated Usar {@link #getTotalIncidentesPorEstado(EstadoIncidente, int, RolUsuario)} */
    @Deprecated
    public int getTotalIncidentesPorEstado(EstadoIncidente estado) {
        return incidenteDAO.listarPorEstado(estado).size();
    }

    /** @deprecated Usar {@link #getIncidentesPorPrioridad(int, RolUsuario)} */
    @Deprecated
    public Map<Prioridad, Integer> getIncidentesPorPrioridad() {
        Map<Prioridad, Integer> stats = new HashMap<>();
        for (Prioridad p : Prioridad.values()) {
            stats.put(p, 0);
        }
        for (Incidente inc : incidenteDAO.listarTodos()) {
            stats.merge(inc.getPrioridad(), 1, Integer::sum);
        }
        return stats;
    }

    /** @deprecated Usar {@link #getIncidentesRecientes(int, int, RolUsuario)} */
    @Deprecated
    public List<Incidente> getIncidentesRecientes(int limite) {
        List<Incidente> todos = incidenteDAO.listarTodos();
        todos.sort((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()));
        return todos.stream().limit(limite).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helper interno
    // -------------------------------------------------------------------------

    /**
     * Devuelve la lista de incidentes visible para el usuario según su rol.
     *  - ADMIN   → todos los incidentes del sistema
     *  - TECNICO → solo los incidentes asignados a él
     *  - USUARIO → solo los incidentes creados por él
     */
    private List<Incidente> getIncidentesFiltrados(int usuarioId, RolUsuario rol) {
        if (rol == RolUsuario.ADMIN) {
            return incidenteDAO.listarTodos();
        } else if (rol == RolUsuario.TECNICO) {
            return incidenteDAO.listarPorTecnico(usuarioId);
        } else {
            return incidenteDAO.listarPorUsuario(usuarioId);
        }
    }
}

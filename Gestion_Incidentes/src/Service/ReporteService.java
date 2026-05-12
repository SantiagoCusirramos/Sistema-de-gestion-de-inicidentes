package Service;

import DAOs.IncidenteDAO;
import Model.Incidente;
import enums.EstadoIncidente;
import enums.Prioridad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReporteService {
    private IncidenteDAO incidenteDAO;

    public int getTotalIncidentesPorEstado(EstadoIncidente estado) {
        return incidenteDAO.listarPorEstado(estado).size();
    }

    public Map<Prioridad, Integer> getIncidentesPorPrioridad() {
        Map<Prioridad, Integer> stats = new HashMap<>();
        for (Prioridad p : Prioridad.values()) {
            stats.put(p, 0);
        }

        for (Incidente inc : incidenteDAO.listarTodos()) {
            stats.put(inc.getPrioridad(), stats.get(inc.getPrioridad()) + 1);
        }
        return stats;
    }

    public List<Incidente> getIncidentesRecientes(int limite) {
        List<Incidente> todos = incidenteDAO.listarTodos();
        todos.sort((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()));
        return todos.stream().limit(limite).collect(Collectors.toList());
    }
}
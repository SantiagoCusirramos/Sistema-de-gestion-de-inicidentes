package Controller;

import Exceptions.IncidenteException;
import Exceptions.PermisoDenegadoException;
import Model.Incidente;
import Model.Usuario;
import Service.IncidenteService;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;

import java.util.List;

public class IncidenteController {

    private IncidenteService incidenteService;
    private Usuario usuarioActual;

    public IncidenteController(Usuario usuarioActual) {
        this.incidenteService = new IncidenteService();
        this.usuarioActual = usuarioActual;
    }

    public void setUsuarioActual(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    public Incidente crearIncidente(String titulo, String descripcion, Prioridad prioridad)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesion para crear un incidente");
        }

        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IncidenteException("El titulo es obligatorio");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IncidenteException("La descripcion es obligatoria");
        }

        return incidenteService.crearIncidente(titulo.trim(), descripcion.trim(), usuarioActual.getId(), prioridad);
    }

    public boolean asignarTecnico(int incidenteId, int tecnicoId)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null || usuarioActual.getRol() != RolUsuario.ADMIN) {
            throw new PermisoDenegadoException("Solo los administradores pueden asignar tecnicos");
        }

        Incidente inc = incidenteService.getIncidentePorId(incidenteId);
        if (inc == null) {
            throw new IncidenteException("El incidente con ID " + incidenteId + " no existe");
        }

        return incidenteService.asignarTecnico(incidenteId, tecnicoId);
    }

    public boolean resolverIncidente(int incidenteId, String solucion)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null || (usuarioActual.getRol() != RolUsuario.TECNICO && usuarioActual.getRol() != RolUsuario.ADMIN)) {
            throw new PermisoDenegadoException("Solo tecnicos o administradores pueden resolver incidentes");
        }

        if (solucion == null || solucion.trim().isEmpty()) {
            throw new IncidenteException("Debe proporcionar una solucion");
        }

        return incidenteService.resolverIncidente(incidenteId, solucion, usuarioActual.getId());
    }

    public boolean cerrarIncidente(int incidenteId)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesion para cerrar un incidente");
        }

        if (usuarioActual.getRol() == RolUsuario.USUARIO) {
            Incidente inc = incidenteService.getIncidentePorId(incidenteId);
            if (inc == null) {
                throw new IncidenteException("El incidente con ID " + incidenteId + " no existe");
            }
            if (inc.getUsuarioId() != usuarioActual.getId()) {
                throw new PermisoDenegadoException("Solo el creador del incidente puede cerrarlo");
            }
        }

        return incidenteService.cerrarIncidente(incidenteId, usuarioActual.getId());
    }

    public List<Incidente> listarIncidentes() {
        if (usuarioActual == null) return List.of();
        return incidenteService.getIncidentesPorUsuario(usuarioActual.getId(), usuarioActual.getRol());
    }

    public Incidente buscarIncidente(int id) {
        return incidenteService.getIncidentePorId(id);
    }

    public boolean agregarComentario(int incidenteId, String mensaje)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesion para comentar");
        }
        if (mensaje == null || mensaje.trim().isEmpty()) {
            throw new IncidenteException("El comentario no puede estar vacio");
        }

        return incidenteService.agregarComentario(incidenteId, usuarioActual.getId(), mensaje.trim());
    }

    public List<String> listarComentarios(int incidenteId) {
        return incidenteService.getComentarios(incidenteId);
    }

    public boolean cambiarEstado(int incidenteId, EstadoIncidente nuevoEstado)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesion para cambiar el estado");
        }
        if (nuevoEstado == null) {
            throw new IncidenteException("El estado no es valido");
        }

        return incidenteService.cambiarEstado(incidenteId, nuevoEstado, usuarioActual.getId());
    }

    public int getTotalIncidentes() {
        return listarIncidentes().size();
    }

    public long getIncidentesPorEstado(EstadoIncidente estado) {
        return listarIncidentes().stream().filter(i -> i.getEstado() == estado).count();
    }
}

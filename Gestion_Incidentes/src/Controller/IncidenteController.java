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

/**
 * Correcciones aplicadas:
 *  [SEC-006] cerrarIncidente propaga el rol del usuario al Service para una autorización correcta.
 *  [SEC-005] listarComentarios propaga usuarioId y rol al Service para control de acceso.
 *  [SEC-012] Validaciones de longitud máxima antes de llegar al Service.
 */
public class IncidenteController {

    // Límites de longitud [SEC-012]
    private static final int MAX_TITULO      = 200;
    private static final int MAX_DESCRIPCION = 5000;
    private static final int MAX_COMENTARIO  = 2000;
    private static final int MAX_SOLUCION    = 3000;

    private IncidenteService incidenteService;
    private Usuario usuarioActual;

    public IncidenteController(Usuario usuarioActual) {
        this.incidenteService = new IncidenteService();
        this.usuarioActual    = usuarioActual;
    }

    public void setUsuarioActual(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    public Incidente crearIncidente(String titulo, String descripcion, Prioridad prioridad)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesión para crear un incidente");
        }

        // [SEC-012] Validaciones de longitud
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IncidenteException("El título es obligatorio");
        }
        if (titulo.trim().length() > MAX_TITULO) {
            throw new IncidenteException(
                "El título no puede superar " + MAX_TITULO + " caracteres");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IncidenteException("La descripción es obligatoria");
        }
        if (descripcion.trim().length() > MAX_DESCRIPCION) {
            throw new IncidenteException(
                "La descripción no puede superar " + MAX_DESCRIPCION + " caracteres");
        }

        return incidenteService.crearIncidente(
            titulo.trim(), descripcion.trim(), usuarioActual.getId(), prioridad);
    }

    public boolean asignarTecnico(int incidenteId, int tecnicoId)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null || usuarioActual.getRol() != RolUsuario.ADMIN) {
            throw new PermisoDenegadoException("Solo los administradores pueden asignar técnicos");
        }

        Incidente inc = incidenteService.getIncidentePorId(incidenteId);
        if (inc == null) {
            throw new IncidenteException("El incidente con ID " + incidenteId + " no existe");
        }

        return incidenteService.asignarTecnico(incidenteId, tecnicoId);
    }

    public boolean resolverIncidente(int incidenteId, String solucion)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null ||
            (usuarioActual.getRol() != RolUsuario.TECNICO &&
             usuarioActual.getRol() != RolUsuario.ADMIN)) {
            throw new PermisoDenegadoException(
                "Solo técnicos o administradores pueden resolver incidentes");
        }

        if (solucion == null || solucion.trim().isEmpty()) {
            throw new IncidenteException("Debe proporcionar una solución");
        }
        // [SEC-012]
        if (solucion.trim().length() > MAX_SOLUCION) {
            throw new IncidenteException(
                "La solución no puede superar " + MAX_SOLUCION + " caracteres");
        }

        return incidenteService.resolverIncidente(
            incidenteId, solucion.trim(), usuarioActual.getId());
    }

    /**
     * [SEC-006] Se propaga el rol al Service para que la lógica de autorización
     *            sea correcta: ADMIN puede cerrar cualquier incidente.
     */
    public boolean cerrarIncidente(int incidenteId)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesión para cerrar un incidente");
        }
        if (usuarioActual.getRol() == RolUsuario.USUARIO) {
            throw new PermisoDenegadoException(
                "Los usuarios no pueden cerrar incidentes. Solo técnicos o administradores.");
        }

        // [SEC-006] El rol se pasa para que el Service lo use en la verificación
        return incidenteService.cerrarIncidente(
            incidenteId, usuarioActual.getId(), usuarioActual.getRol());
    }

    public List<Incidente> listarIncidentes() {
        if (usuarioActual == null) return List.of();
        return incidenteService.getIncidentesPorUsuario(
            usuarioActual.getId(), usuarioActual.getRol());
    }

    public Incidente buscarIncidente(int id) {
        return incidenteService.getIncidentePorId(id);
    }

    public boolean agregarComentario(int incidenteId, String mensaje)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesión para comentar");
        }
        if (mensaje == null || mensaje.trim().isEmpty()) {
            throw new IncidenteException("El comentario no puede estar vacío");
        }
        // [SEC-012]
        if (mensaje.trim().length() > MAX_COMENTARIO) {
            throw new IncidenteException(
                "El comentario no puede superar " + MAX_COMENTARIO + " caracteres");
        }

        return incidenteService.agregarComentario(
            incidenteId, usuarioActual.getId(), mensaje.trim());
    }

    /**
     * [SEC-005] Se propagan usuarioId y rol al Service para control de acceso.
     */
    public List<String> listarComentarios(int incidenteId)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesión para ver comentarios");
        }

        return incidenteService.getComentarios(
            incidenteId, usuarioActual.getId(), usuarioActual.getRol());
    }

    public boolean cambiarEstado(int incidenteId, EstadoIncidente nuevoEstado)
            throws IncidenteException, PermisoDenegadoException {

        if (usuarioActual == null) {
            throw new PermisoDenegadoException("Debe iniciar sesión para cambiar el estado");
        }
        if (nuevoEstado == null) {
            throw new IncidenteException("El estado no es válido");
        }

        return incidenteService.cambiarEstado(
            incidenteId, nuevoEstado, usuarioActual.getId());
    }

    public int getTotalIncidentes() {
        return listarIncidentes().size();
    }

    public long getIncidentesPorEstado(EstadoIncidente estado) {
        return listarIncidentes().stream()
               .filter(i -> i.getEstado() == estado)
               .count();
    }
}

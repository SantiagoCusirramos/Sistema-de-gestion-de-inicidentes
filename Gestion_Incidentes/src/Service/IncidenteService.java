package Service;

import DAOs.ComentarioDAO;
import DAOs.IncidenteDAO;
import Exceptions.IncidenteException;
import Exceptions.PermisoDenegadoException;
import Model.Comentario;
import Model.Incidente;
import Model.Usuario;
import DAOs.UsuarioDAO;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Correcciones aplicadas:
 *  [SEC-005] IDOR: agregarComentario y getComentarios verifican autorización del usuario.
 *  [SEC-006] cerrarIncidente recibe el rol del usuario para autorizar correctamente a ADMIN/TECNICO.
 *  [SEC-007] Comparación de tecnicoId con Objects.equals() para evitar bug de autoboxing con Integer.
 *  [SEC-012] Validaciones de longitud máxima en entradas de texto.
 */
public class IncidenteService {

    // Límites de longitud [SEC-012]
    static final int MAX_TITULO     = 200;
    static final int MAX_DESCRIPCION = 5000;
    static final int MAX_COMENTARIO  = 2000;
    static final int MAX_SOLUCION    = 3000;

    private IncidenteDAO incidenteDAO;
    private ComentarioDAO comentarioDAO;
    private UsuarioDAO usuarioDAO;

    public IncidenteService() {
        this.incidenteDAO  = new DAOs.Implementacion.IncidenteDAOImpl();
        this.comentarioDAO = new DAOs.Implementacion.ComentarioDAOImpl();
        this.usuarioDAO    = new DAOs.Implementacion.UsuarioDAOImpl();
    }

    public Incidente crearIncidente(String titulo, String descripcion,
                                    int usuarioId, Prioridad prioridad)
            throws IncidenteException {

        // [SEC-012] Validación de longitud
        if (titulo != null && titulo.length() > MAX_TITULO) {
            throw new IncidenteException(
                "El título no puede superar " + MAX_TITULO + " caracteres");
        }
        if (descripcion != null && descripcion.length() > MAX_DESCRIPCION) {
            throw new IncidenteException(
                "La descripción no puede superar " + MAX_DESCRIPCION + " caracteres");
        }

        Incidente inc = new Incidente();
        inc.setTitulo(titulo);
        inc.setDescripcion(descripcion);
        inc.setUsuarioId(usuarioId);
        inc.setPrioridad(prioridad);
        inc.setEstado(EstadoIncidente.PENDIENTE);
        inc.setFechaCreacion(LocalDateTime.now());

        int id = incidenteDAO.crear(inc);
        if (id == -1) {
            throw new IncidenteException("No se pudo guardar el incidente en la base de datos.");
        }
        return incidenteDAO.buscar(id);
    }

    public boolean asignarTecnico(int incidenteId, int tecnicoId) throws IncidenteException {
        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) {
            throw new IncidenteException("Incidente no encontrado");
        }

        inc.setTecnicoId(tecnicoId);
        inc.setEstado(EstadoIncidente.EN_PROCESO);
        return incidenteDAO.actualizar(inc);
    }

    /**
     * [SEC-007] Usa Objects.equals() para comparar Integer con int sin riesgo de autoboxing.
     */
    public boolean resolverIncidente(int incidenteId, String solucion, int tecnicoId)
            throws IncidenteException, PermisoDenegadoException {

        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) {
            throw new IncidenteException("Incidente no encontrado");
        }

        // Objects.equals maneja el caso Integer vs int correctamente [SEC-007]
        if (!Objects.equals(inc.getTecnicoId(), tecnicoId)) {
            throw new PermisoDenegadoException(
                "Solo el técnico asignado puede resolver este incidente");
        }

        // [SEC-012] Validar longitud de solución
        if (solucion != null && solucion.length() > MAX_SOLUCION) {
            throw new IncidenteException(
                "La solución no puede superar " + MAX_SOLUCION + " caracteres");
        }

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

    /**
     * [SEC-006] Recibe el rol del usuario para autorizar correctamente.
     *   - ADMIN: puede cerrar cualquier incidente.
     *   - TECNICO: solo puede cerrar el incidente que tiene asignado.
     *   El método previo solo comparaba usuarioId == creador, lo que impedía
     *   a los admins cerrar incidentes ajenos.
     */
    public boolean cerrarIncidente(int incidenteId, int usuarioId, RolUsuario rol)
            throws IncidenteException, PermisoDenegadoException {

        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) {
            throw new IncidenteException("Incidente no encontrado");
        }

        boolean esAdmin            = rol == RolUsuario.ADMIN;
        boolean esTecnicoAsignado  = rol == RolUsuario.TECNICO &&
                                     Objects.equals(inc.getTecnicoId(), usuarioId);

        if (!esAdmin && !esTecnicoAsignado) {
            throw new PermisoDenegadoException(
                "No está autorizado para cerrar este incidente");
        }

        inc.setEstado(EstadoIncidente.CERRADO);
        inc.setFechaActualizacion(LocalDateTime.now());
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

    /**
     * [SEC-005] IDOR corregido: verifica que el usuario tenga acceso al incidente.
     *   - ADMIN y TECNICO asignado: acceso completo.
     *   - USUARIO: solo puede comentar en sus propios incidentes.
     */
    public boolean agregarComentario(int incidenteId, int usuarioId, String mensaje)
            throws IncidenteException, PermisoDenegadoException {

        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) {
            throw new IncidenteException("Incidente no encontrado");
        }

        // [SEC-012] Validar longitud
        if (mensaje != null && mensaje.length() > MAX_COMENTARIO) {
            throw new IncidenteException(
                "El comentario no puede superar " + MAX_COMENTARIO + " caracteres");
        }

        // [SEC-005] Verificar autorización
        Usuario usuario = usuarioDAO.buscar(usuarioId);
        if (usuario == null) {
            throw new PermisoDenegadoException("Usuario no encontrado");
        }

        boolean autorizado = usuario.getRol() == RolUsuario.ADMIN
            || usuario.getRol() == RolUsuario.TECNICO
            || inc.getUsuarioId() == usuarioId; // USUARIO solo en sus propios incidentes

        if (!autorizado) {
            throw new PermisoDenegadoException(
                "No tiene permiso para comentar en este incidente");
        }

        Comentario comentario = new Comentario();
        comentario.setIncidenteId(incidenteId);
        comentario.setUsuarioId(usuarioId);
        comentario.setMensaje(mensaje);
        comentario.setFecha(LocalDateTime.now());
        return comentarioDAO.crear(comentario) > 0;
    }

    /**
     * [SEC-005] IDOR corregido: solo devuelve comentarios si el usuario tiene acceso
     *            al incidente correspondiente.
     */
    public List<String> getComentarios(int incidenteId, int usuarioId, RolUsuario rol)
            throws IncidenteException, PermisoDenegadoException {

        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) {
            throw new IncidenteException("Incidente no encontrado");
        }

        // [SEC-005] Verificar que el usuario tiene acceso al incidente
        boolean autorizado = rol == RolUsuario.ADMIN
            || rol == RolUsuario.TECNICO
            || inc.getUsuarioId() == usuarioId;

        if (!autorizado) {
            throw new PermisoDenegadoException(
                "No tiene permiso para ver los comentarios de este incidente");
        }

        List<Comentario> comentarios = comentarioDAO.listarPorIncidente(incidenteId);
        List<String> resultado = new ArrayList<>();
        for (Comentario c : comentarios) {
            resultado.add("[" + c.getFecha() + "] Usuario " +
                          c.getUsuarioId() + ": " + c.getMensaje());
        }
        return resultado;
    }

    public boolean cambiarEstado(int incidenteId, EstadoIncidente nuevoEstado, int usuarioId)
            throws IncidenteException {

        Incidente inc = incidenteDAO.buscar(incidenteId);
        if (inc == null) {
            throw new IncidenteException("Incidente no encontrado");
        }

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

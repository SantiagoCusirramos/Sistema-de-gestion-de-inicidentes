package Service;

import DAOs.Implementacion.UsuarioDAOImpl;
import DAOs.UsuarioDAO;
import Exceptions.UsuarioException;
import Model.Usuario;
import Utils.Validador;
import enums.RolUsuario;

import java.util.List;

public class UsuarioService {

    private UsuarioDAO usuarioDAO;
    private Validador validador;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAOImpl();
        this.validador = new Validador();
    }

    public Usuario registrarUsuario(String nombre, String email, String password, RolUsuario rol)
            throws UsuarioException {

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new UsuarioException("El nombre es obligatorio");
        }

        if (!validador.validarEmail(email)) {
            throw new UsuarioException("Email inválido");
        }

        if (!validador.validarPassword(password)) {
            throw new UsuarioException("La contraseña debe tener al menos 6 caracteres");
        }

        if (usuarioDAO.buscarPorEmail(email) != null) {
            throw new UsuarioException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setEmail(email.toLowerCase().trim());
        usuario.setPassword(encriptarPassword(password));
        usuario.setRol(rol);
        usuario.setFechaRegistro(java.time.LocalDateTime.now());

        int id = usuarioDAO.crear(usuario);
        return usuarioDAO.buscar(id);
    }

    public Usuario login(String email, String password) throws UsuarioException {
        if (email == null || password == null) {
            throw new UsuarioException("Email y contraseña son obligatorios");
        }

        Usuario usuario = usuarioDAO.buscarPorEmail(email.toLowerCase().trim());

        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }

        if (!verificarPassword(password, usuario.getPassword())) {
            throw new UsuarioException("Contraseña incorrecta");
        }

        return usuario;
    }

    public Usuario actualizarUsuario(int id, String nombre, String email, RolUsuario rol)
            throws UsuarioException {

        Usuario usuario = usuarioDAO.buscar(id);
        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }

        if (!usuario.getEmail().equals(email)) {
            if (!validador.validarEmail(email)) {
                throw new UsuarioException("Email inválido");
            }
            if (usuarioDAO.buscarPorEmail(email) != null) {
                throw new UsuarioException("El email ya está en uso");
            }
            usuario.setEmail(email.toLowerCase().trim());
        }

        if (nombre != null && !nombre.trim().isEmpty()) {
            usuario.setNombre(nombre.trim());
        }

        usuario.setRol(rol);

        usuarioDAO.actualizar(usuario);
        return usuarioDAO.buscar(id);
    }

    public boolean cambiarPassword(int id, String passwordActual, String passwordNueva)
            throws UsuarioException {

        Usuario usuario = usuarioDAO.buscar(id);
        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }

        if (!verificarPassword(passwordActual, usuario.getPassword())) {
            throw new UsuarioException("Contraseña actual incorrecta");
        }

        if (!validador.validarPassword(passwordNueva)) {
            throw new UsuarioException("La nueva contraseña debe tener al menos 6 caracteres");
        }

        usuario.setPassword(encriptarPassword(passwordNueva));
        return usuarioDAO.actualizar(usuario);
    }

    public Usuario obtenerUsuario(int id) throws UsuarioException {
        Usuario usuario = usuarioDAO.buscar(id);
        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }
        return usuario;
    }

    public List<Usuario> listarTodosUsuarios() {
        return usuarioDAO.listarTodos();

    }

    public List<Usuario> listarUsuariosPorRol(RolUsuario rol) {
        return usuarioDAO.listarPorRol(rol);
    }

    public List<Usuario> listarTecnicos() {
        return usuarioDAO.listarPorRol(RolUsuario.TECNICO);
    }

    public boolean eliminarUsuario(int id) throws UsuarioException {
        Usuario usuario = usuarioDAO.buscar(id);
        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }

        if (usuario.getRol() == RolUsuario.ADMIN) {
            long totalAdmins = usuarioDAO.listarPorRol(RolUsuario.ADMIN).size();
            if (totalAdmins <= 1) {
                throw new UsuarioException("No se puede eliminar el único administrador del sistema");
            }
        }

        return usuarioDAO.eliminar(id);
    }

    public boolean tienePermiso(Usuario usuario, String accion) {
        if (usuario == null) return false;

        switch (usuario.getRol()) {
            case ADMIN:
                return true;
            case TECNICO:
                return accion.equals("VER_INCIDENTES") ||
                        accion.equals("MODIFICAR_INCIDENTES") ||
                        accion.equals("COMENTAR");
            case USUARIO:
                return accion.equals("CREAR_INCIDENTE") ||
                        accion.equals("VER_MIS_INCIDENTES") ||
                        accion.equals("COMENTAR_MIS_INCIDENTES");
            default:
                return false;
        }
    }

    private String encriptarPassword(String password) {
        return java.util.Base64.getEncoder().encodeToString(password.getBytes());
    }

    private boolean verificarPassword(String password, String passwordEncriptado) {
        String encoded = java.util.Base64.getEncoder().encodeToString(password.getBytes());
        return encoded.equals(passwordEncriptado);
    }
}
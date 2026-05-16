package Service;

import DAOs.Implementacion.UsuarioDAOImpl;
import DAOs.UsuarioDAO;
import Exceptions.UsuarioException;
import Model.Usuario;
import Utils.Validador;
import enums.RolUsuario;

import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UsuarioService {

    private UsuarioDAO usuarioDAO;
    private Validador validador;

    // Costo de BCrypt (10-12 es razonable)
    private static final int BCRYPT_ROUNDS = 12;

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

        if (usuarioDAO.buscarPorEmail(email.toLowerCase().trim()) != null) {
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
        if (email == null || email.trim().isEmpty()) {
            throw new UsuarioException("Email y contraseña son obligatorios");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new UsuarioException("Email y contraseña son obligatorios");
        }

        String emailNormalizado = email.toLowerCase().trim();
        Usuario usuario = usuarioDAO.buscarPorEmail(emailNormalizado);

        if (usuario == null) {
            throw new UsuarioException("Credenciales incorrectas");
        }

        String passwordAlmacenada = usuario.getPassword();

        if (!verificarPassword(password, passwordAlmacenada)) {
            throw new UsuarioException("Credenciales incorrectas");
        }

        // Migración automática: si la contraseña estaba en texto plano,
        // se reemplaza por un hash BCrypt al primer login exitoso.
        if (esTextoPlano(passwordAlmacenada)) {
            usuario.setPassword(encriptarPassword(password));
            usuarioDAO.actualizar(usuario);
        }

        return usuario;
    }

    public Usuario actualizarUsuario(int id, String nombre, String email, RolUsuario rol)
            throws UsuarioException {

        Usuario usuario = usuarioDAO.buscar(id);
        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }

        String emailNormalizado = email != null ? email.toLowerCase().trim() : "";

        if (!usuario.getEmail().equals(emailNormalizado)) {
            if (!validador.validarEmail(emailNormalizado)) {
                throw new UsuarioException("Email inválido");
            }
            if (usuarioDAO.buscarPorEmail(emailNormalizado) != null) {
                throw new UsuarioException("El email ya está en uso");
            }
            usuario.setEmail(emailNormalizado);
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

    // -------------------------------------------------------------------------
    // Métodos privados de manejo de contraseñas
    // -------------------------------------------------------------------------

    /**
     * Genera un hash BCrypt de la contraseña.
     */
    private String encriptarPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifica la contraseña soportando dos formatos:
     *  1. Hash BCrypt (contraseñas nuevas, empiezan con "$2a$" o "$2b$")
     *  2. Texto plano (contraseñas legacy cargadas directamente en la BD)
     *
     * En producción, una vez que todos los usuarios hayan iniciado sesión
     * al menos una vez, las contraseñas legacy ya no existirán.
     */
    private boolean verificarPassword(String passwordIngresada, String passwordAlmacenada) {
        if (passwordAlmacenada == null || passwordAlmacenada.isEmpty()) {
            return false;
        }

        if (esBCrypt(passwordAlmacenada)) {
            // Contraseña ya hasheada con BCrypt
            try {
                return BCrypt.checkpw(passwordIngresada, passwordAlmacenada);
            } catch (Exception e) {
                return false;
            }
        } else {
            // Contraseña legacy en texto plano (datos de seed/BD)
            return passwordAlmacenada.equals(passwordIngresada);
        }
    }

    /**
     * Detecta si el valor almacenado es un hash BCrypt válido.
     */
    private boolean esBCrypt(String valor) {
        return valor != null && (valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$"));
    }

    /**
     * Detecta si el valor almacenado es texto plano (no BCrypt).
     */
    private boolean esTextoPlano(String valor) {
        return !esBCrypt(valor);
    }
}

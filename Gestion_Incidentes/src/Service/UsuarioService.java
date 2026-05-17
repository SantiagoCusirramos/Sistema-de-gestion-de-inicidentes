package Service;

import DAOs.Implementacion.UsuarioDAOImpl;
import DAOs.UsuarioDAO;
import Exceptions.UsuarioException;
import Model.Usuario;
import Utils.Validador;
import enums.RolUsuario;

import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

/**
 * Correcciones aplicadas:
 *  [SEC-009] Solo BCrypt. No se acepta texto plano.
 *  [SEC-010] Protección contra fuerza bruta: bloqueo temporal tras intentos fallidos.
 *  [BUG-010] cambiarPassword ahora rechaza una nueva contraseña igual a la actual.
 */
public class UsuarioService {

    private UsuarioDAO usuarioDAO;
    private Validador validador;

    private static final int BCRYPT_ROUNDS     = 12;
    private static final int MAX_INTENTOS      = 5;
    private static final long BLOQUEO_SEGUNDOS = 300L; // 5 minutos

    // Límites de longitud [SEC-012]
    static final int MAX_NOMBRE   = 100;
    static final int MAX_EMAIL    = 100;
    static final int MAX_PASSWORD = 128;

    public UsuarioService() {
        this.usuarioDAO = new UsuarioDAOImpl();
        this.validador  = new Validador();
    }

    // -------------------------------------------------------------------------
    // Registro
    // -------------------------------------------------------------------------

    public Usuario registrarUsuario(String nombre, String email, String password)
            throws UsuarioException {
        return registrarUsuarioConRol(nombre, email, password, RolUsuario.USUARIO);
    }

    public Usuario registrarUsuarioConRol(String nombre, String email,
                                           String password, RolUsuario rol)
            throws UsuarioException {

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new UsuarioException("El nombre es obligatorio");
        }
        if (nombre.trim().length() > MAX_NOMBRE) {
            throw new UsuarioException("El nombre no puede superar " + MAX_NOMBRE + " caracteres");
        }
        if (!validador.validarEmail(email)) {
            throw new UsuarioException("Email inválido");
        }
        if (email.length() > MAX_EMAIL) {
            throw new UsuarioException("El email no puede superar " + MAX_EMAIL + " caracteres");
        }
        if (!validador.validarPassword(password)) {
            throw new UsuarioException("La contraseña debe tener al menos 6 caracteres");
        }
        if (password.length() > MAX_PASSWORD) {
            throw new UsuarioException("La contraseña no puede superar " + MAX_PASSWORD + " caracteres");
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
        if (id == -1) {
            throw new UsuarioException("No se pudo crear el usuario. Verifique la conexión a la base de datos.");
        }
        return usuarioDAO.buscar(id);
    }

    // -------------------------------------------------------------------------
    // Login con protección contra fuerza bruta [SEC-010]
    // -------------------------------------------------------------------------

    public Usuario login(String email, String password) throws UsuarioException {
        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            throw new UsuarioException("Email y contraseña son obligatorios");
        }

        String emailNormalizado = email.toLowerCase().trim();
        Usuario usuario = usuarioDAO.buscarPorEmail(emailNormalizado);

        if (usuario == null) {
            BCrypt.checkpw(password, "$2a$12$invalidhashfortimingnormalization00000000000000000000000");
            throw new UsuarioException("Credenciales incorrectas");
        }

        verificarBloqueo(usuario);

        boolean credencialesValidas = verificarPasswordBcrypt(password, usuario.getPassword());

        if (!credencialesValidas) {
            registrarIntentoFallido(usuario);
            throw new UsuarioException("Credenciales incorrectas");
        }

        resetearIntentosFallidos(usuario);
        return usuario;
    }

    // -------------------------------------------------------------------------
    // Gestión de intentos fallidos [SEC-010]
    // -------------------------------------------------------------------------

    private void verificarBloqueo(Usuario usuario) throws UsuarioException {
        java.time.LocalDateTime bloqueadoHasta = usuarioDAO.getBloqueadoHasta(usuario.getId());
        if (bloqueadoHasta != null &&
            bloqueadoHasta.isAfter(java.time.LocalDateTime.now())) {

            long segundosRestantes = java.time.Duration.between(
                java.time.LocalDateTime.now(), bloqueadoHasta).getSeconds();
            throw new UsuarioException(
                "Cuenta bloqueada temporalmente por demasiados intentos fallidos. " +
                "Intente de nuevo en " + Math.max(1, segundosRestantes / 60) + " minuto(s).");
        }
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentosActuales = usuarioDAO.getIntentosFallidos(usuario.getId()) + 1;
        if (intentosActuales >= MAX_INTENTOS) {
            java.time.LocalDateTime bloqueadoHasta =
                java.time.LocalDateTime.now().plusSeconds(BLOQUEO_SEGUNDOS);
            usuarioDAO.setBloqueadoHasta(usuario.getId(), bloqueadoHasta);
            usuarioDAO.setIntentosFallidos(usuario.getId(), 0);
        } else {
            usuarioDAO.setIntentosFallidos(usuario.getId(), intentosActuales);
        }
    }

    private void resetearIntentosFallidos(Usuario usuario) {
        usuarioDAO.setIntentosFallidos(usuario.getId(), 0);
        usuarioDAO.setBloqueadoHasta(usuario.getId(), null);
    }

    // -------------------------------------------------------------------------
    // Gestión de usuarios
    // -------------------------------------------------------------------------

    public Usuario actualizarUsuario(int id, String nombre, String email, RolUsuario rol)
            throws UsuarioException {

        Usuario usuario = usuarioDAO.buscar(id);
        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }

        String emailNormalizado = (email != null) ? email.toLowerCase().trim() : "";

        if (!usuario.getEmail().equals(emailNormalizado)) {
            if (!validador.validarEmail(emailNormalizado)) {
                throw new UsuarioException("Email inválido");
            }
            if (emailNormalizado.length() > MAX_EMAIL) {
                throw new UsuarioException("El email no puede superar " + MAX_EMAIL + " caracteres");
            }
            if (usuarioDAO.buscarPorEmail(emailNormalizado) != null) {
                throw new UsuarioException("El email ya está en uso");
            }
            usuario.setEmail(emailNormalizado);
        }

        if (nombre != null && !nombre.trim().isEmpty()) {
            if (nombre.trim().length() > MAX_NOMBRE) {
                throw new UsuarioException("El nombre no puede superar " + MAX_NOMBRE + " caracteres");
            }
            usuario.setNombre(nombre.trim());
        }

        usuario.setRol(rol);
        usuarioDAO.actualizar(usuario);
        return usuarioDAO.buscar(id);
    }

    /**
     * [BUG-010] Se verifica que la nueva contraseña sea diferente a la actual.
     *            BCrypt no permite comparar hashes directamente, por lo que se
     *            usa checkpw contra el hash almacenado para detectar igualdad.
     */
    public boolean cambiarPassword(int id, String passwordActual, String passwordNueva)
            throws UsuarioException {

        Usuario usuario = usuarioDAO.buscar(id);
        if (usuario == null) {
            throw new UsuarioException("Usuario no encontrado");
        }

        // [SEC-009] Solo BCrypt
        if (!verificarPasswordBcrypt(passwordActual, usuario.getPassword())) {
            throw new UsuarioException("Contraseña actual incorrecta");
        }

        if (!validador.validarPassword(passwordNueva)) {
            throw new UsuarioException("La nueva contraseña debe tener al menos 6 caracteres");
        }
        if (passwordNueva.length() > MAX_PASSWORD) {
            throw new UsuarioException("La contraseña no puede superar " + MAX_PASSWORD + " caracteres");
        }

        // [BUG-010] Rechazar si la nueva contraseña es igual a la actual
        if (verificarPasswordBcrypt(passwordNueva, usuario.getPassword())) {
            throw new UsuarioException("La nueva contraseña debe ser diferente a la contraseña actual");
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
            return usuarioDAO.eliminarAdminSeguro(id);
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
    // Métodos privados de contraseñas
    // -------------------------------------------------------------------------

    private String encriptarPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    private boolean verificarPasswordBcrypt(String passwordIngresada, String hashAlmacenado) {
        if (hashAlmacenado == null || hashAlmacenado.isBlank()) return false;
        if (!esBCrypt(hashAlmacenado)) {
            System.err.println("[SEGURIDAD] Hash de contraseña en formato no reconocido para un usuario. " +
                               "Se requiere migración manual.");
            return false;
        }
        try {
            return BCrypt.checkpw(passwordIngresada, hashAlmacenado);
        } catch (Exception e) {
            System.err.println("[SEGURIDAD] Error al verificar BCrypt hash: " + e.getMessage());
            return false;
        }
    }

    private boolean esBCrypt(String valor) {
        return valor != null &&
               (valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$"));
    }
}

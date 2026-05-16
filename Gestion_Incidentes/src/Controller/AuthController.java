package Controller;

import Exceptions.UsuarioException;
import Model.Usuario;
import Service.UsuarioService;
import enums.RolUsuario;

/**
 * Correcciones aplicadas:
 *  [SEC-004] El registro público SIEMPRE asigna rol USUARIO.
 *            Solo un ADMIN autenticado puede crear cuentas con rol elevado.
 *            El parámetro "rol" fue eliminado del método de registro público.
 */
public class AuthController {

    private UsuarioService usuarioService;
    private Usuario usuarioActual;
    private String ultimoError;

    public AuthController() {
        this.usuarioService = new UsuarioService();
        this.usuarioActual  = null;
        this.ultimoError    = "";
    }

    public String getUltimoError() {
        return ultimoError;
    }

    /**
     * Registro público: siempre crea un usuario con rol USUARIO.
     * [SEC-004] No se acepta parámetro de rol desde el exterior.
     */
    public boolean registrar(String nombre, String email, String password) {
        try {
            // [SEC-004] Rol fijo: USUARIO. Sin posibilidad de escalar a TECNICO o ADMIN.
            Usuario nuevoUsuario = usuarioService.registrarUsuario(nombre, email, password);
            if (nuevoUsuario == null) {
                ultimoError = "No se pudo crear el usuario. Verifique la conexión a la base de datos.";
                return false;
            }
            return true;
        } catch (UsuarioException e) {
            ultimoError = e.getMessage();
            return false;
        }
    }

    /**
     * Registro con rol específico: solo puede ser invocado por un ADMIN autenticado.
     * [SEC-004] Si no hay sesión activa de ADMIN, rechaza la operación.
     *
     * @param rol  "tecnico" o "admin" (cualquier otro valor asigna USUARIO)
     */
    public boolean registrarConRol(String nombre, String email, String password, String rol) {
        if (usuarioActual == null || usuarioActual.getRol() != RolUsuario.ADMIN) {
            ultimoError = "Solo un administrador autenticado puede asignar roles elevados.";
            return false;
        }

        RolUsuario rolUsuario;
        switch (rol == null ? "" : rol.toLowerCase().trim()) {
            case "tecnico":
                rolUsuario = RolUsuario.TECNICO;
                break;
            case "admin":
                rolUsuario = RolUsuario.ADMIN;
                break;
            default:
                rolUsuario = RolUsuario.USUARIO;
        }

        try {
            Usuario nuevoUsuario = usuarioService.registrarUsuarioConRol(
                nombre, email, password, rolUsuario);
            if (nuevoUsuario == null) {
                ultimoError = "No se pudo crear el usuario. Verifique la conexión a la base de datos.";
                return false;
            }
            return true;
        } catch (UsuarioException e) {
            ultimoError = e.getMessage();
            return false;
        }
    }

    public boolean login(String email, String password) {
        try {
            usuarioActual = usuarioService.login(email, password);
            ultimoError   = "";
            return true;
        } catch (UsuarioException e) {
            ultimoError = e.getMessage();
            return false;
        }
    }

    public void logout() {
        usuarioActual = null;
    }

    public boolean isLoggedIn() {
        return usuarioActual != null;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean isAdmin() {
        return usuarioActual != null && usuarioActual.getRol() == RolUsuario.ADMIN;
    }

    public boolean isTecnico() {
        return usuarioActual != null && usuarioActual.getRol() == RolUsuario.TECNICO;
    }

    public boolean isUsuario() {
        return usuarioActual != null && usuarioActual.getRol() == RolUsuario.USUARIO;
    }

    public boolean cambiarPassword(String passwordActual, String passwordNueva) {
        if (usuarioActual == null) return false;
        try {
            return usuarioService.cambiarPassword(
                usuarioActual.getId(), passwordActual, passwordNueva);
        } catch (UsuarioException e) {
            ultimoError = e.getMessage();
            return false;
        }
    }

    public String getInfoUsuarioActual() {
        if (usuarioActual == null) return "No hay sesión activa";
        return String.format("Usuario: %s (%s) - Email: %s",
                usuarioActual.getNombre(),
                usuarioActual.getRol(),
                usuarioActual.getEmail());
    }
}

package Controller;


import Exceptions.UsuarioException;
import Model.Usuario;
import Service.UsuarioService;
import enums.RolUsuario;

public class AuthController {

    private UsuarioService usuarioService;
    private Usuario usuarioActual;
    private String ultimoError;

    public AuthController() {
        this.usuarioService = new UsuarioService();
        this.usuarioActual = null;
        this.ultimoError = "";
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public boolean registrar(String nombre, String email, String password, String rol) {
        try {
            RolUsuario rolUsuario;
            switch (rol.toLowerCase()) {
                case "tecnico":
                    rolUsuario = RolUsuario.TECNICO;
                    break;
                case "admin":
                    if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.ADMIN) {
                        rolUsuario = RolUsuario.ADMIN;
                    } else {
                        rolUsuario = RolUsuario.USUARIO;
                    }
                    break;
                default:
                    rolUsuario = RolUsuario.USUARIO;
            }

            Usuario nuevoUsuario = usuarioService.registrarUsuario(nombre, email, password, rolUsuario);
            if (nuevoUsuario == null) {
                ultimoError = "No se pudo crear el usuario. Verifique la conexion a la base de datos.";
            }
            return nuevoUsuario != null;
        } catch (UsuarioException e) {
            ultimoError = e.getMessage();
            System.err.println("Error al registrar: " + e.getMessage());
            return false;
        }
    }

    public boolean login(String email, String password) {
        try {
            usuarioActual = usuarioService.login(email, password);
            ultimoError = "";
            return true;
        } catch (UsuarioException e) {
            ultimoError = e.getMessage();
            System.err.println("Error de login: " + e.getMessage());
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
            return usuarioService.cambiarPassword(usuarioActual.getId(), passwordActual, passwordNueva);
        } catch (UsuarioException e) {
            System.err.println("Error al cambiar contraseña: " + e.getMessage());
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
package DAOs;

import Model.Usuario;
import enums.RolUsuario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Métodos añadidos:
 *  [SEC-010] getIntentosFallidos, setIntentosFallidos, getBloqueadoHasta, setBloqueadoHasta
 *             — persisten el estado de bloqueo por fuerza bruta en la BD.
 *  [SEC-011] eliminarAdminSeguro — eliminación atómica con SELECT FOR UPDATE para evitar
 *             race condition al eliminar el último administrador.
 */
public interface UsuarioDAO {
    int crear(Usuario usuario);
    Usuario buscar(int id);
    Usuario buscarPorEmail(String email);
    List<Usuario> listarTodos();
    List<Usuario> listarPorRol(RolUsuario rol);
    boolean actualizar(Usuario usuario);
    boolean eliminar(int id);

    // --- Protección contra fuerza bruta [SEC-010] ---
    int getIntentosFallidos(int usuarioId);
    void setIntentosFallidos(int usuarioId, int intentos);
    LocalDateTime getBloqueadoHasta(int usuarioId);
    void setBloqueadoHasta(int usuarioId, LocalDateTime hasta);

    // --- Eliminación segura del último admin [SEC-011] ---
    boolean eliminarAdminSeguro(int id) throws Exceptions.UsuarioException;
}

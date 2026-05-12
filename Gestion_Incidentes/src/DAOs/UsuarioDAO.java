package DAOs;

import Model.Usuario;
import enums.RolUsuario;

import java.util.List;

public interface UsuarioDAO {
    int crear(Usuario usuario);
    Usuario buscar(int id);
    Usuario buscarPorEmail(String email);
    List<Usuario> listarTodos();
    List<Usuario> listarPorRol(RolUsuario rol);
    boolean actualizar(Usuario usuario);
    boolean eliminar(int id);
}

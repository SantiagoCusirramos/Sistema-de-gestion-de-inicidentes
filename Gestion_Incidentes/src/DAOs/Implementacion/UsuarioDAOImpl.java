package DAOs.Implementacion;

import DAOs.UsuarioDAO;
import Exceptions.UsuarioException;
import Model.Usuario;
import Utils.ConexionBD;
import enums.RolUsuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Correcciones aplicadas:
 *  [SEC-008] printStackTrace() reemplazado por mensajes de error internos sin exponer stack.
 *  [SEC-010] Nuevos métodos para gestión de intentos fallidos y bloqueo temporal.
 *  [SEC-011] eliminarAdminSeguro usa transacción con SELECT FOR UPDATE para evitar TOCTOU.
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public int crear(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre, email, password, rol, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, usuario.getNombre());
            pstmt.setString(2, usuario.getEmail());
            pstmt.setString(3, usuario.getPassword());
            pstmt.setString(4, usuario.getRol().name());
            pstmt.setTimestamp(5, Timestamp.valueOf(usuario.getFechaRegistro()));

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            // [SEC-008] No se expone stack trace al exterior
            System.err.println("[UsuarioDAOImpl] Error al crear usuario: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public Usuario buscar(int id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al buscar usuario por ID: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT * FROM usuarios WHERE email = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al buscar usuario por email: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY nombre";

        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) usuarios.add(mapResultSetToUsuario(rs));
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al listar usuarios: " + e.getMessage());
        }
        return usuarios;
    }

    @Override
    public List<Usuario> listarPorRol(RolUsuario rol) {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE rol = ? ORDER BY nombre";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rol.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) usuarios.add(mapResultSetToUsuario(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al listar usuarios por rol: " + e.getMessage());
        }
        return usuarios;
    }

    @Override
    public boolean actualizar(Usuario usuario) {
        String sql = "UPDATE usuarios SET nombre = ?, email = ?, password = ?, rol = ? WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getNombre());
            pstmt.setString(2, usuario.getEmail());
            pstmt.setString(3, usuario.getPassword());
            pstmt.setString(4, usuario.getRol().name());
            pstmt.setInt(5, usuario.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Protección contra fuerza bruta [SEC-010]
    // -------------------------------------------------------------------------

    @Override
    public int getIntentosFallidos(int usuarioId) {
        String sql = "SELECT intentos_fallidos FROM usuarios WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al obtener intentos fallidos: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void setIntentosFallidos(int usuarioId, int intentos) {
        String sql = "UPDATE usuarios SET intentos_fallidos = ? WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, intentos);
            pstmt.setInt(2, usuarioId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al actualizar intentos fallidos: " + e.getMessage());
        }
    }

    @Override
    public LocalDateTime getBloqueadoHasta(int usuarioId) {
        String sql = "SELECT bloqueado_hasta FROM usuarios WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.toLocalDateTime() : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al obtener bloqueado_hasta: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setBloqueadoHasta(int usuarioId, LocalDateTime hasta) {
        String sql = "UPDATE usuarios SET bloqueado_hasta = ? WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (hasta != null) {
                pstmt.setTimestamp(1, Timestamp.valueOf(hasta));
            } else {
                pstmt.setNull(1, Types.TIMESTAMP);
            }
            pstmt.setInt(2, usuarioId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error al actualizar bloqueado_hasta: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Eliminación atómica del admin [SEC-011]
    // -------------------------------------------------------------------------

    /**
     * Elimina un administrador solo si existen al menos dos.
     * Usa transacción con SELECT FOR UPDATE para evitar race condition (TOCTOU).
     */
    @Override
    public boolean eliminarAdminSeguro(int id) throws UsuarioException {
        String countSql  = "SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' FOR UPDATE";
        String deleteSql = "DELETE FROM usuarios WHERE id = ?";

        Connection conn = null;
        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            // Bloquear y contar admins dentro de la transacción
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {

                if (rs.next() && rs.getInt(1) <= 1) {
                    conn.rollback();
                    throw new UsuarioException(
                        "No se puede eliminar el único administrador del sistema");
                }
            }

            // Proceder con la eliminación
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, id);
                int rows = pstmt.executeUpdate();
                conn.commit();
                return rows > 0;
            }

        } catch (UsuarioException e) {
            throw e; // re-lanzar excepción de negocio
        } catch (SQLException e) {
            System.err.println("[UsuarioDAOImpl] Error en eliminarAdminSeguro: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close(); // devuelve al pool
                } catch (SQLException ex) { /* ignorar */ }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Mapeo
    // -------------------------------------------------------------------------

    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setEmail(rs.getString("email"));
        usuario.setPassword(rs.getString("password"));
        usuario.setRol(RolUsuario.valueOf(rs.getString("rol")));
        usuario.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
        return usuario;
    }
}

package DAOs.Implementacion;

import DAOs.ComentarioDAO;
import Model.Comentario;
import Utils.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Correcciones aplicadas:
 *  [SEC-008] e.printStackTrace() reemplazado por mensajes de log internos.
 *            ResultSet cerrado con try-with-resources.
 */
public class ComentarioDAOImpl implements ComentarioDAO {

    @Override
    public int crear(Comentario comentario) {
        String sql = "INSERT INTO comentarios (incidente_id, usuario_id, mensaje, es_interno, fecha) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, comentario.getIncidenteId());
            pstmt.setInt(2, comentario.getUsuarioId());
            pstmt.setString(3, comentario.getMensaje());
            pstmt.setBoolean(4, false);
            pstmt.setTimestamp(5, Timestamp.valueOf(comentario.getFecha()));

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            // [SEC-008] Sin stack trace expuesto al exterior
            System.err.println("[ComentarioDAOImpl] Error al crear comentario: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public List<Comentario> listarPorIncidente(int incidenteId) {
        List<Comentario> comentarios = new ArrayList<>();
        String sql = "SELECT * FROM comentarios WHERE incidente_id = ? ORDER BY fecha ASC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, incidenteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) comentarios.add(mapResultSetToComentario(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ComentarioDAOImpl] Error al listar comentarios: " + e.getMessage());
        }
        return comentarios;
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM comentarios WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ComentarioDAOImpl] Error al eliminar comentario: " + e.getMessage());
            return false;
        }
    }

    private Comentario mapResultSetToComentario(ResultSet rs) throws SQLException {
        Comentario comentario = new Comentario();
        comentario.setId(rs.getInt("id"));
        comentario.setIncidenteId(rs.getInt("incidente_id"));
        comentario.setUsuarioId(rs.getInt("usuario_id"));
        comentario.setMensaje(rs.getString("mensaje"));
        comentario.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        return comentario;
    }
}

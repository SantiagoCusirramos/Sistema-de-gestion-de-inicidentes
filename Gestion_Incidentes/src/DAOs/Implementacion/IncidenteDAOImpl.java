package DAOs.Implementacion;

import DAOs.IncidenteDAO;
import Model.Incidente;
import Utils.ConexionBD;
import enums.EstadoIncidente;
import enums.Prioridad;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IncidenteDAOImpl implements IncidenteDAO {

    @Override
    public int crear(Incidente incidente) {
        String sql = "INSERT INTO incidentes (titulo, descripcion, fecha_creacion, usuario_id, tecnico_id, estado, prioridad) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, incidente.getTitulo());
            pstmt.setString(2, incidente.getDescripcion());
            pstmt.setTimestamp(3, Timestamp.valueOf(incidente.getFechaCreacion()));
            pstmt.setInt(4, incidente.getUsuarioId());
            if (incidente.getTecnicoId() != null) {
                pstmt.setInt(5, incidente.getTecnicoId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            pstmt.setString(6, incidente.getEstado().name());
            pstmt.setString(7, incidente.getPrioridad().name());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean actualizar(Incidente incidente) {
        String sql = "UPDATE incidentes SET titulo = ?, descripcion = ?, fecha_actualizacion = ?, tecnico_id = ?, estado = ?, prioridad = ? WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, incidente.getTitulo());
            pstmt.setString(2, incidente.getDescripcion());
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            if (incidente.getTecnicoId() != null) {
                pstmt.setInt(4, incidente.getTecnicoId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setString(5, incidente.getEstado().name());
            pstmt.setString(6, incidente.getPrioridad().name());
            pstmt.setInt(7, incidente.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Incidente buscar(int id) {
        String sql = "SELECT * FROM incidentes WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToIncidente(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Incidente> listarTodos() {
        List<Incidente> incidentes = new ArrayList<>();
        String sql = "SELECT * FROM incidentes ORDER BY fecha_creacion DESC";

        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                incidentes.add(mapResultSetToIncidente(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidentes;
    }

    @Override
    public List<Incidente> listarPorUsuario(int usuarioId) {
        List<Incidente> incidentes = new ArrayList<>();
        String sql = "SELECT * FROM incidentes WHERE usuario_id = ? ORDER BY fecha_creacion DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                incidentes.add(mapResultSetToIncidente(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidentes;
    }

    @Override
    public List<Incidente> listarPorTecnico(int tecnicoId) {
        List<Incidente> incidentes = new ArrayList<>();
        String sql = "SELECT * FROM incidentes WHERE tecnico_id = ? ORDER BY fecha_creacion DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tecnicoId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                incidentes.add(mapResultSetToIncidente(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidentes;
    }

    @Override
    public List<Incidente> listarPorEstado(EstadoIncidente estado) {
        List<Incidente> incidentes = new ArrayList<>();
        String sql = "SELECT * FROM incidentes WHERE estado = ? ORDER BY fecha_creacion DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, estado.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                incidentes.add(mapResultSetToIncidente(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidentes;
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM incidentes WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Incidente mapResultSetToIncidente(ResultSet rs) throws SQLException {
        Incidente inc = new Incidente();
        inc.setId(rs.getInt("id"));
        inc.setTitulo(rs.getString("titulo"));
        inc.setDescripcion(rs.getString("descripcion"));
        inc.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
        inc.setUsuarioId(rs.getInt("usuario_id"));
        int tecnicoId = rs.getInt("tecnico_id");
        inc.setTecnicoId(rs.wasNull() ? null : tecnicoId);
        inc.setEstado(EstadoIncidente.valueOf(rs.getString("estado")));
        inc.setPrioridad(Prioridad.valueOf(rs.getString("prioridad")));
        Timestamp fechaActualizacion = rs.getTimestamp("fecha_actualizacion");
        if (fechaActualizacion != null) {
            inc.setFechaActualizacion(fechaActualizacion.toLocalDateTime());
        }
        return inc;
    }
}

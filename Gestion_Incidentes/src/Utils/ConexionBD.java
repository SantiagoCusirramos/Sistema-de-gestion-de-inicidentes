package Utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {

    private static final HikariDataSource dataSource;

    static {
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUrl.isBlank()) {
            throw new ExceptionInInitializerError(
                "Variable de entorno DB_URL no configurada. " +
                "Ejemplo: DB_URL=jdbc:mysql://localhost:3306/sistema_incidentes?serverTimezone=UTC");
        }
        if (dbUser == null || dbUser.isBlank()) {
            throw new ExceptionInInitializerError(
                "Variable de entorno DB_USER no configurada.");
        }
        if (dbPass == null) {
            throw new ExceptionInInitializerError(
                "Variable de entorno DB_PASS no configurada.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
    }

    private ConexionBD() {}

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Pool de conexiones cerrado.");
        }
    }

    public static void inicializarBaseDatos() {
        String crearTablaUsuarios = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INT PRIMARY KEY AUTO_INCREMENT,
                nombre VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                rol ENUM('ADMIN', 'TECNICO', 'USUARIO') NOT NULL,
                telefono VARCHAR(20),
                activo BOOLEAN DEFAULT TRUE,
                fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                ultimo_login TIMESTAMP NULL,
                intentos_fallidos INT NOT NULL DEFAULT 0,
                bloqueado_hasta TIMESTAMP NULL,
                INDEX idx_email (email),
                INDEX idx_rol (rol)
            )
            """;

        String crearTablaIncidentes = """
            CREATE TABLE IF NOT EXISTS incidentes (
                id INT PRIMARY KEY AUTO_INCREMENT,
                titulo VARCHAR(200) NOT NULL,
                descripcion TEXT NOT NULL,
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                fecha_actualizacion TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
                fecha_resolucion TIMESTAMP NULL,
                usuario_id INT NOT NULL,
                tecnico_id INT NULL,
                estado ENUM('PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO') NOT NULL DEFAULT 'PENDIENTE',
                prioridad ENUM('BAJA', 'MEDIA', 'ALTA', 'CRITICA') NOT NULL DEFAULT 'MEDIA',
                tiempo_resolucion_horas INT,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY (tecnico_id) REFERENCES usuarios(id) ON DELETE SET NULL,
                INDEX idx_estado (estado),
                INDEX idx_usuario (usuario_id),
                INDEX idx_tecnico (tecnico_id),
                INDEX idx_prioridad (prioridad)
            )
            """;

        String crearTablaComentarios = """
            CREATE TABLE IF NOT EXISTS comentarios (
                id INT PRIMARY KEY AUTO_INCREMENT,
                incidente_id INT NOT NULL,
                usuario_id INT NOT NULL,
                mensaje TEXT NOT NULL,
                es_interno BOOLEAN DEFAULT FALSE,
                fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                INDEX idx_incidente (incidente_id)
            )
            """;

        String crearTablaHistorial = """
            CREATE TABLE IF NOT EXISTS historial_estados (
                id INT PRIMARY KEY AUTO_INCREMENT,
                incidente_id INT NOT NULL,
                estado_anterior ENUM('PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO'),
                estado_nuevo ENUM('PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO') NOT NULL,
                usuario_id INT NOT NULL,
                comentario TEXT,
                fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
                INDEX idx_incidente_fecha (incidente_id, fecha_cambio)
            )
            """;

        String crearTablaNotificaciones = """
            CREATE TABLE IF NOT EXISTS notificaciones (
                id INT PRIMARY KEY AUTO_INCREMENT,
                usuario_id INT NOT NULL,
                incidente_id INT,
                titulo VARCHAR(200) NOT NULL,
                mensaje TEXT NOT NULL,
                leido BOOLEAN DEFAULT FALSE,
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                fecha_lectura TIMESTAMP NULL,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE SET NULL,
                INDEX idx_usuario_leido (usuario_id, leido)
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(crearTablaUsuarios);
            stmt.execute(crearTablaIncidentes);
            stmt.execute(crearTablaComentarios);
            stmt.execute(crearTablaHistorial);
            stmt.execute(crearTablaNotificaciones);

            insertarAdminInicial(conn);

            System.out.println("Base de datos inicializada correctamente.");

        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar la base de datos.", e);
        }
    }

    private static void insertarAdminInicial(Connection conn) throws SQLException {
        String adminEmail = "admin@sistema.in";

        String checkSql = "SELECT COUNT(*) FROM usuarios WHERE email = ?";
        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setString(1, adminEmail);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Admin inicial ya existe. Saltando creación.");
                    return;
                }
            }
        }

        String adminPass = System.getenv("ADMIN_INITIAL_PASSWORD");
        if (adminPass == null || adminPass.isBlank()) {
            throw new IllegalStateException(
                "Variable de entorno ADMIN_INITIAL_PASSWORD no configurada. " +
                "Ejemplo: export ADMIN_INITIAL_PASSWORD=\"MiContraseña$egura2024\"");
        }

        String hashBcrypt = BCrypt.hashpw(adminPass, BCrypt.gensalt(12));

        String insertSql = """
            INSERT INTO usuarios (nombre, email, password, rol, telefono)
            VALUES (?, ?, ?, 'ADMIN', ?)
            """;
        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setString(1, "Administrador");
            insert.setString(2, adminEmail);
            insert.setString(3, hashBcrypt);
            insert.setString(4, "123456789");
            insert.executeUpdate();
            System.out.println("Admin inicial creado correctamente.");
        }
    }
}

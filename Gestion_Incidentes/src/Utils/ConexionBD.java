package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {

    // Configuración de la base de datos
    private static final String URL = "jdbc:mysql://localhost:3306/sistema_incidentes";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static Connection connection = null;

    // Constructor privado para patrón Singleton
    private ConexionBD() {}

    // Obtener conexión (Singleton)
    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName(DRIVER);
                connection = DriverManager.getConnection(URL, USUARIO, PASSWORD);
                System.out.println("✅ Conexión a base de datos establecida");
            } catch (ClassNotFoundException e) {
                System.err.println("❌ Error: Driver MySQL no encontrado");
                e.printStackTrace();
            } catch (SQLException e) {
                System.err.println("❌ Error al conectar a la base de datos");
                e.printStackTrace();
            }
        }
        return connection;
    }

    // Cerrar conexión
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("🔒 Conexión a base de datos cerrada");
            } catch (SQLException e) {
                System.err.println("❌ Error al cerrar la conexión");
                e.printStackTrace();
            }
        }
    }

    // Verificar si la conexión está activa
    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // Método para crear las tablas automáticamente (útil para primera ejecución)
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

        String insertarAdmin = """
            INSERT INTO usuarios (nombre, email, password, rol, telefono) 
            SELECT 'Administrador', 'admin@sistema.com', 'YWRtaW4xMjM=', 'ADMIN', '123456789'
            WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'admin@sistema.com')
            """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(crearTablaUsuarios);
            stmt.execute(crearTablaIncidentes);
            stmt.execute(crearTablaComentarios);
            stmt.execute(crearTablaHistorial);
            stmt.execute(crearTablaNotificaciones);
            stmt.execute(insertarAdmin);
            System.out.println("✅ Base de datos inicializada correctamente");
        } catch (SQLException e) {
            System.err.println("❌ Error al inicializar la base de datos");
            e.printStackTrace();
        }
    }

    // Método para probar la conexión
    public static void testConexion() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Conexión exitosa a: " + URL);
                System.out.println("📊 Base de datos: " + conn.getCatalog());
                System.out.println("🔧 Versión MySQL: " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en prueba de conexión");
            e.printStackTrace();
        }
    }
}
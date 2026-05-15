-- =====================================================
-- SISTEMA DE GESTIÓN DE INCIDENTES
-- Script completo de creación de tablas e inserts iniciales
-- =====================================================

USE sistema_incidentes;

-- =====================================================
-- 1. TABLA: usuarios
-- =====================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'TECNICO', 'USUARIO') NOT NULL DEFAULT 'USUARIO',
    telefono VARCHAR(20),
    activo BOOLEAN DEFAULT TRUE,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. TABLA: incidentes
-- =====================================================
CREATE TABLE IF NOT EXISTS incidentes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    usuario_id INT NOT NULL,
    tecnico_id INT NULL,
    estado ENUM('PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO') NOT NULL DEFAULT 'PENDIENTE',
    prioridad ENUM('BAJA', 'MEDIA', 'ALTA', 'CRITICA') NOT NULL DEFAULT 'MEDIA',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (tecnico_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- =====================================================
-- 3. TABLA: comentarios
-- =====================================================
CREATE TABLE IF NOT EXISTS comentarios (
    id INT PRIMARY KEY AUTO_INCREMENT,
    incidente_id INT NOT NULL,
    usuario_id INT NOT NULL,
    mensaje TEXT NOT NULL,
    es_interno BOOLEAN DEFAULT FALSE,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- =====================================================
-- 4. TABLA: historial_estados
-- =====================================================
CREATE TABLE IF NOT EXISTS historial_estados (
    id INT PRIMARY KEY AUTO_INCREMENT,
    incidente_id INT NOT NULL,
    estado_anterior ENUM('PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO'),
    estado_nuevo ENUM('PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'CERRADO') NOT NULL,
    usuario_id INT NOT NULL,
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- =====================================================
-- 5. TABLA: notificaciones
-- =====================================================
CREATE TABLE IF NOT EXISTS notificaciones (
    id INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id INT NOT NULL,
    mensaje TEXT NOT NULL,
    leida BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- =====================================================
-- 6. INSERTS INICIALES (DATOS POR DEFECTO)
-- =====================================================

-- Usuario administrador (contraseña: admin123 en Base64)
INSERT INTO usuarios (nombre, email, password, rol, activo) VALUES 
('Administrador del Sistema', 'admin@sistema.com', 'YWRtaW4xMjM=', 'ADMIN', TRUE);

-- Técnicos de ejemplo (contraseña: tecnico123 en Base64)
INSERT INTO usuarios (nombre, email, password, rol, activo) VALUES 
('Carlos Técnico', 'carlos.tecnico@sistema.com', 'dGVjbmljbzEyMw==', 'TECNICO', TRUE),
('María Técnica', 'maria.tecnica@sistema.com', 'dGVjbmljbzEyMw==', 'TECNICO', TRUE);

-- Usuarios normales de ejemplo (contraseña: usuario123 en Base64)
INSERT INTO usuarios (nombre, email, password, rol, activo, telefono) VALUES 
('Juan Pérez', 'juan.perez@ejemplo.com', 'dXN1YXJpbzEyMw==', 'USUARIO', TRUE, '555-1234'),
('Ana Gómez', 'ana.gomez@ejemplo.com', 'dXN1YXJpbzEyMw==', 'USUARIO', TRUE, '555-5678');

-- =====================================================
-- 7. VERIFICACIÓN DE DATOS INSERTADOS
-- =====================================================
SELECT '=== USUARIOS CREADOS ===' as '';
SELECT id, nombre, email, rol FROM usuarios;

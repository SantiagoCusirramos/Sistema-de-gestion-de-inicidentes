#!/usr/bin/env bash
# ============================================================
#  INSTALADOR - Sistema de Gestión de Incidentes
#  Distribución: Manjaro Linux (Arch-based)
#  Requisitos: Java 21+, Maven 3.9+, MySQL 8+
# ============================================================

set -euo pipefail

# ── Colores ──────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

info()    { echo -e "${CYAN}[INFO]${RESET} $*"; }
ok()      { echo -e "${GREEN}[OK]${RESET}   $*"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET} $*"; }
error()   { echo -e "${RED}[ERROR]${RESET} $*"; exit 1; }
titulo()  { echo -e "\n${BOLD}${CYAN}══════════════════════════════════════${RESET}"; \
            echo -e "${BOLD}${CYAN}  $*${RESET}"; \
            echo -e "${BOLD}${CYAN}══════════════════════════════════════${RESET}"; }

# ── Variables ─────────────────────────────────────────────────
PROYECTO_DIR="Gestion_Incidentes"          # Carpeta del proyecto (ajustar si difiere)
DB_NAME="sistema_incidentes"
DB_USER="root"
DB_PASS=""                                  # Vacío para desarrollo local

# ============================================================
# 0. Verificar que NO se ejecuta como root
# ============================================================
titulo "Verificando entorno"

if [[ $EUID -eq 0 ]]; then
    error "No ejecutes este script como root. Usa tu usuario normal; se pedirá sudo cuando sea necesario."
fi

# ============================================================
# 1. Actualizar repositorios
# ============================================================
titulo "1/6  Actualizando repositorios de Pacman"
info "Ejecutando: sudo pacman -Sy"
sudo pacman -Sy --noconfirm
ok "Repositorios actualizados."

# ============================================================
# 2. Instalar dependencias del sistema
# ============================================================
titulo "2/6  Instalando dependencias (Java, Maven, MySQL)"

instalar_si_falta() {
    local pkg="$1"
    if pacman -Qi "$pkg" &>/dev/null; then
        ok "$pkg ya está instalado."
    else
        info "Instalando $pkg ..."
        sudo pacman -S --noconfirm "$pkg"
        ok "$pkg instalado."
    fi
}

instalar_si_falta "jdk21-openjdk"
instalar_si_falta "maven"
instalar_si_falta "mysql"

# Establecer JAVA_HOME si no está configurado
if [[ -z "${JAVA_HOME:-}" ]]; then
    JAVA_HOME_CANDIDATO=$(dirname "$(dirname "$(readlink -f "$(which java)")")")
    export JAVA_HOME="$JAVA_HOME_CANDIDATO"
    info "JAVA_HOME establecido automáticamente: $JAVA_HOME"
fi

# Verificar versiones
java_version=$(java -version 2>&1 | head -n1)
mvn_version=$(mvn --version 2>&1 | head -n1)
info "Java:  $java_version"
info "Maven: $mvn_version"

# ============================================================
# 3. Inicializar y arrancar MariaDB
# ============================================================
titulo "3/6  Configurando MariaDB"

# Manjaro instala MariaDB; usar mariadb-install-db en vez de --initialize-insecure
if [[ ! -d /var/lib/mysql/mysql ]]; then
    info "Inicializando directorio de datos de MariaDB..."
    sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
    ok "Datos de MariaDB inicializados."
else
    ok "Directorio de datos de MariaDB ya existe."
fi

# El servicio se llama mariadb (no mysqld) en Arch/Manjaro
info "Habilitando e iniciando mariadb..."
sudo systemctl enable mariadb --now

# Esperar a que MariaDB esté listo
info "Esperando a que MariaDB acepte conexiones..."
for i in {1..20}; do
    if sudo mariadb-admin ping --silent 2>/dev/null; then
        ok "MariaDB está activo."
        break
    fi
    sleep 1
    if [[ $i -eq 20 ]]; then
        error "MariaDB no respondió después de 20 segundos. Revisa: sudo systemctl status mariadb"
    fi
done

# ============================================================
# 4. Configurar MariaDB para desarrollo (sin contraseña para root)
# ============================================================
titulo "4/6  Configurando usuario root de MariaDB"

info "Ajustando autenticación de root (sin contraseña, solo para desarrollo)..."
sudo mariadb -u root <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '';
FLUSH PRIVILEGES;
SQL
ok "Autenticación de root configurada."

# ============================================================
# 5. Crear base de datos y tablas
# ============================================================
titulo "5/6  Creando base de datos y tablas"

info "Creando base de datos '$DB_NAME'..."
mariadb -u "$DB_USER" <<SQL
CREATE DATABASE IF NOT EXISTS $DB_NAME
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
SQL
ok "Base de datos '$DB_NAME' lista."

info "Creando tablas e insertando datos iniciales..."
mariadb -u "$DB_USER" "$DB_NAME" <<'SQL'
-- =====================================================
-- SISTEMA DE GESTIÓN DE INCIDENTES
-- Script completo de creación de tablas e inserts
-- =====================================================

-- 1. TABLA: usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    nombre          VARCHAR(100)  NOT NULL,
    email           VARCHAR(100)  NOT NULL UNIQUE,
    password        VARCHAR(255)  NOT NULL,
    rol             ENUM('ADMIN','TECNICO','USUARIO') NOT NULL DEFAULT 'USUARIO',
    telefono        VARCHAR(20),
    activo          BOOLEAN       DEFAULT TRUE,
    fecha_registro  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- 2. TABLA: incidentes
CREATE TABLE IF NOT EXISTS incidentes (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    titulo              VARCHAR(200) NOT NULL,
    descripcion         TEXT         NOT NULL,
    usuario_id          INT          NOT NULL,
    tecnico_id          INT          NULL,
    estado              ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO') NOT NULL DEFAULT 'PENDIENTE',
    prioridad           ENUM('BAJA','MEDIA','ALTA','CRITICA')               NOT NULL DEFAULT 'MEDIA',
    fecha_creacion      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (tecnico_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- 3. TABLA: comentarios
CREATE TABLE IF NOT EXISTS comentarios (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    incidente_id INT  NOT NULL,
    usuario_id   INT  NOT NULL,
    mensaje      TEXT NOT NULL,
    es_interno   BOOLEAN   DEFAULT FALSE,
    fecha        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id)   REFERENCES usuarios(id)   ON DELETE CASCADE
);

-- 4. TABLA: historial_estados
CREATE TABLE IF NOT EXISTS historial_estados (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    incidente_id   INT NOT NULL,
    estado_anterior ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO'),
    estado_nuevo    ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO') NOT NULL,
    usuario_id      INT NOT NULL,
    fecha_cambio    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id)   REFERENCES usuarios(id)   ON DELETE CASCADE
);

-- 5. TABLA: notificaciones
CREATE TABLE IF NOT EXISTS notificaciones (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id     INT  NOT NULL,
    mensaje        TEXT NOT NULL,
    leida          BOOLEAN   DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 6. DATOS INICIALES (se omiten si ya existen)
INSERT IGNORE INTO usuarios (nombre, email, password, rol, activo) VALUES
    ('Administrador del Sistema', 'admin@sistema.com',              'admin123',      'ADMIN',   TRUE),
    ('Carlos Técnico',            'carlos.tecnico@sistema.com',     'carlostecnico', 'TECNICO', TRUE),
    ('María Técnica',             'maria.tecnica@sistema.com',      'mariatecnica',  'TECNICO', TRUE);

INSERT IGNORE INTO usuarios (nombre, email, password, rol, activo, telefono) VALUES
    ('Juan Pérez', 'juan.perez@ejemplo.com', 'juanperez', 'USUARIO', TRUE, '555-1234'),
    ('Ana Gómez',  'ana.gomez@ejemplo.com',  'anagomez',  'USUARIO', TRUE, '555-5678');
SQL

ok "Tablas y datos iniciales creados correctamente."

# Verificar usuarios
echo ""
info "Usuarios en la base de datos:"
mariadb -u "$DB_USER" "$DB_NAME" -e "SELECT id, nombre, email, rol FROM usuarios;" 2>/dev/null

# ============================================================
# 6. Compilar y ejecutar el proyecto
# ============================================================
titulo "6/6  Compilando y ejecutando el proyecto"

if [[ ! -d "$PROYECTO_DIR" ]]; then
    warn "No se encontró la carpeta '$PROYECTO_DIR' en el directorio actual."
    warn "Asegúrate de ejecutar este script desde el directorio que contiene '$PROYECTO_DIR'."
    warn "O cambia la variable PROYECTO_DIR al inicio del script."
    echo ""
    read -rp "¿Deseas compilar ahora? (s/n): " resp
    if [[ "$resp" != "s" && "$resp" != "S" ]]; then
        info "Saltando compilación. Cuando estés listo, ejecuta:"
        echo "    cd $PROYECTO_DIR && mvn clean javafx:run"
        exit 0
    fi
fi

cd "$PROYECTO_DIR"
info "Compilando con Maven (puede tardar en la primera ejecución)..."
mvn clean javafx:run

# ============================================================
# Resumen final
# ============================================================
titulo "✅  Instalación completada"

echo -e "${BOLD}Credenciales por defecto:${RESET}"
echo -e "  ${GREEN}ADMIN${RESET}   → admin@sistema.com          / admin123"
echo -e "  ${CYAN}TECNICO${RESET} → carlos.tecnico@sistema.com  / tecnico123"
echo -e "  ${CYAN}TECNICO${RESET} → maria.tecnica@sistema.com   / tecnico123"
echo -e "  ${YELLOW}USUARIO${RESET} → juan.perez@ejemplo.com      / usuario123"
echo -e "  ${YELLOW}USUARIO${RESET} → ana.gomez@ejemplo.com       / usuario123"
echo ""
echo -e "${BOLD}Para ejecutar el proyecto en el futuro:${RESET}"
echo -e "  sudo systemctl start mariadb"
echo -e "  cd $PROYECTO_DIR && mvn clean javafx:run"

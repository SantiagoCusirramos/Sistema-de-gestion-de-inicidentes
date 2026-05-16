#!/usr/bin/env bash
set -euo pipefail

R='\033[0;31m' Y='\033[1;33m' G='\033[0;32m' C='\033[0;36m' B='\033[1m' X='\033[0m'

info()  { echo -e "${C}[INFO]${X}  $*"; }
ok()    { echo -e "${G}[OK]${X}    $*"; }
warn()  { echo -e "${Y}[WARN]${X}  $*"; }
err()   { echo -e "${R}[ERROR]${X} $*"; exit 1; }
hdr()   { echo -e "\n${B}── $* ──${X}"; }

PROYECTO_DIR="Gestion_Incidentes"
DB_NAME="sistema_incidentes"

[[ $EUID -eq 0 ]] && err "No ejecutar como root."

hdr "Variables de entorno"
[[ -z "${DB_URL:-}"   ]] && err "DB_URL no definida.  Ejemplo: export DB_URL=\"jdbc:mysql://localhost:3306/sistema_incidentes?serverTimezone=UTC\""
[[ -z "${DB_USER:-}"  ]] && err "DB_USER no definida. Ejemplo: export DB_USER=\"root\""
[[ -z "${DB_PASS+x}" ]] && err "DB_PASS no definida. Ejemplo: export DB_PASS=\"\""
[[ -z "${ADMIN_INITIAL_PASSWORD:-}" ]] && err "ADMIN_INITIAL_PASSWORD no definida. Ejemplo: export ADMIN_INITIAL_PASSWORD=\"admin123\""
ok "DB_URL  = $DB_URL"
ok "DB_USER = $DB_USER"
ok "DB_PASS = ${DB_PASS:-(vacío)}"
ok "ADMIN_INITIAL_PASSWORD = (configurada)"

hdr "Sistema"
sudo apt update &>/dev/null && ok "Repos actualizados"

hdr "Dependencias"
pkg_ok() {
    dpkg -l | grep -q "^ii  $1 " &>/dev/null \
        && ok "$1 presente" \
        || { info "Instalando $1..."; sudo apt install -y "$1" &>/dev/null && ok "$1 instalado"; }
}
pkg_ok "openjdk-21-jdk"
pkg_ok "maven"
pkg_ok "mysql-server"

[[ -z "${JAVA_HOME:-}" ]] && \
    export JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(which java)")")")

hdr "MySQL"
if [[ ! -d /var/lib/mysql/mysql ]]; then
    info "Inicializando datos..."
    sudo mysqld --initialize-insecure --user=mysql &>/dev/null || true
    ok "Datos inicializados"
else
    ok "Datos presentes"
fi

sudo systemctl enable mysql --now &>/dev/null

info "Esperando MySQL..."
for i in {1..20}; do
    sudo mysqladmin ping --silent 2>/dev/null && break
    sleep 1
    [[ $i -eq 20 ]] && err "MySQL no responde. Revisa: systemctl status mysql"
done
ok "MySQL activo"

sudo mysql -u root &>/dev/null <<'SQL'
ALTER USER 'root'@'localhost' IDENTIFIED BY '';
FLUSH PRIVILEGES;
SQL
ok "Auth root OK"

hdr "Base de datos"

DB_EXISTS=$(mysql -u "$DB_USER" -sse \
    "SELECT COUNT(*) FROM information_schema.SCHEMATA
     WHERE SCHEMA_NAME='$DB_NAME';" 2>/dev/null || echo 0)

if [[ "$DB_EXISTS" -eq 1 ]]; then
    TABLES_OK=$(mysql -u "$DB_USER" -sse \
        "SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA='$DB_NAME'
         AND TABLE_NAME IN
         ('usuarios','incidentes','comentarios','historial_estados','notificaciones');" \
        2>/dev/null || echo 0)
else
    TABLES_OK=0
fi

if [[ "$DB_EXISTS" -eq 1 && "$TABLES_OK" -eq 5 ]]; then
    info "BD ya inicializada. Saltando bootstrap."
    info "Aplicando migraciones..."
    mysql -u "$DB_USER" "$DB_NAME" &>/dev/null <<'SQL'
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS ultimo_login      TIMESTAMP NULL         AFTER fecha_registro,
    ADD COLUMN IF NOT EXISTS intentos_fallidos INT NOT NULL DEFAULT 0 AFTER ultimo_login,
    ADD COLUMN IF NOT EXISTS bloqueado_hasta   TIMESTAMP NULL         AFTER intentos_fallidos;
CREATE INDEX IF NOT EXISTS idx_email ON usuarios (email);
CREATE INDEX IF NOT EXISTS idx_rol   ON usuarios (rol);
SQL
    ok "Migraciones OK"

else
    if [[ "$DB_EXISTS" -eq 0 ]]; then
        info "Creando base de datos..."
        mysql -u "$DB_USER" &>/dev/null <<SQL
CREATE DATABASE IF NOT EXISTS $DB_NAME
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL
        ok "BD creada"
    else
        warn "BD existe pero tablas incompletas ($TABLES_OK/5). Recreando..."
    fi

    info "Creando tablas..."
    mysql -u "$DB_USER" "$DB_NAME" &>/dev/null <<'SQL'
CREATE TABLE IF NOT EXISTS usuarios (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    nombre              VARCHAR(100) NOT NULL,
    email               VARCHAR(100) UNIQUE NOT NULL,
    password            VARCHAR(255) NOT NULL,
    rol                 ENUM('ADMIN','TECNICO','USUARIO') NOT NULL,
    telefono            VARCHAR(20),
    activo              BOOLEAN DEFAULT TRUE,
    fecha_registro      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_login        TIMESTAMP NULL,
    intentos_fallidos   INT NOT NULL DEFAULT 0,
    bloqueado_hasta     TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_rol   (rol)
);
CREATE TABLE IF NOT EXISTS incidentes (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    titulo              VARCHAR(200) NOT NULL,
    descripcion         TEXT NOT NULL,
    usuario_id          INT NOT NULL,
    tecnico_id          INT NULL,
    estado              ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO') NOT NULL DEFAULT 'PENDIENTE',
    prioridad           ENUM('BAJA','MEDIA','ALTA','CRITICA') NOT NULL DEFAULT 'MEDIA',
    fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (tecnico_id) REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS comentarios (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    incidente_id INT NOT NULL,
    usuario_id   INT NOT NULL,
    mensaje      TEXT NOT NULL,
    es_interno   BOOLEAN DEFAULT FALSE,
    fecha        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id)   REFERENCES usuarios(id)   ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS historial_estados (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    incidente_id    INT NOT NULL,
    estado_anterior ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO'),
    estado_nuevo    ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO') NOT NULL,
    usuario_id      INT NOT NULL,
    fecha_cambio    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id)   REFERENCES usuarios(id)   ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS notificaciones (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id     INT NOT NULL,
    mensaje        TEXT NOT NULL,
    leida          BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
SQL
    ok "Tablas creadas"
fi

hdr "Build"

if [[ ! -d "$PROYECTO_DIR" ]]; then
    warn "Carpeta '$PROYECTO_DIR' no encontrada."
    read -rp "  ¿Compilar de todas formas? (s/n): " resp
    [[ "$resp" != "s" && "$resp" != "S" ]] && {
        info "Ejecutar manualmente: cd $PROYECTO_DIR && mvn clean javafx:run"
        exit 0
    }
fi

cd "$PROYECTO_DIR"
info "Compilando..."
mvn clean javafx:run

echo -e "\n${G}${B}[OK] Listo${X}"
info "mysql  →  sudo systemctl start mysql"
info "app    →  cd $PROYECTO_DIR && mvn clean javafx:run"
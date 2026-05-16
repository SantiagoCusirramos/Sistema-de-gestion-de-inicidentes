@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

:: ============================================================
::  INSTALADOR - Sistema de Gestión de Incidentes
::  Sistema Operativo: Windows 10 / 11
::  Requisitos: Java 21+, Maven 3.9+, MySQL 8+
::  Autor: Instalador automático
:: ============================================================

:: ── Colores via ANSI (requiere Windows 10 1511+) ────────────
:: Activar soporte ANSI en la consola
reg add HKCU\Console /v VirtualTerminalLevel /t REG_DWORD /d 1 /f >nul 2>&1

set "ESC="
set "RED=%ESC%[91m"
set "GREEN=%ESC%[92m"
set "YELLOW=%ESC%[93m"
set "CYAN=%ESC%[96m"
set "BOLD=%ESC%[1m"
set "RESET=%ESC%[0m"

:: ── Variables de configuración ──────────────────────────────
set "PROYECTO_DIR=Gestion_Incidentes"
set "DB_NAME=sistema_incidentes"
set "DB_USER=root"
set "DB_PASS="
set "MYSQL_PORT=3306"

:: URLs de descarga (fallback si winget no está disponible)
set "JAVA_URL=https://download.java.net/java/GA/jdk21.0.5/9124ee2d524d4ec3de30e4748b3a83da/9/GPL/openjdk-21.0.5_windows-x64_bin.zip"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
set "MYSQL_URL=https://dev.mysql.com/get/Downloads/MySQLInstaller/mysql-installer-community-8.0.40.0.msi"

:: Directorios de instalación manual (si winget falla)
set "TOOLS_DIR=%USERPROFILE%\devtools"
set "JAVA_INSTALL_DIR=%TOOLS_DIR%\java21"
set "MAVEN_INSTALL_DIR=%TOOLS_DIR%\maven"

:: ── Contadores de estado ────────────────────────────────────
set "JAVA_OK=0"
set "MAVEN_OK=0"
set "MYSQL_OK=0"
set "WINGET_OK=0"

:: ============================================================
::  INICIO
:: ============================================================
cls
echo.
echo %CYAN%╔══════════════════════════════════════════════════════════╗%RESET%
echo %CYAN%║     INSTALADOR - Sistema de Gestión de Incidentes        ║%RESET%
echo %CYAN%║     Windows 10 / 11  ^|  Java 21  ^|  Maven  ^|  MySQL 8   ║%RESET%
echo %CYAN%╚══════════════════════════════════════════════════════════╝%RESET%
echo.
echo  Este script verificará e instalará automáticamente todos los
echo  componentes necesarios para ejecutar el sistema.
echo.
pause

:: ============================================================
:: PASO 0: Verificar permisos de Administrador
:: ============================================================
call :titulo "PASO 0/6  Verificando permisos de administrador"

net session >nul 2>&1
if %errorLevel% neq 0 (
    echo %YELLOW%[AVISO]%RESET% Este script no se está ejecutando como Administrador.
    echo.
    echo         Algunas instalaciones pueden requerir permisos elevados.
    echo         Se recomienda cerrar y volver a ejecutar haciendo clic derecho
    echo         en el .bat y seleccionando "Ejecutar como administrador".
    echo.
    set /p CONTINUAR="  ¿Deseas continuar de todas formas? (s/n): "
    if /i "!CONTINUAR!" neq "s" (
        echo %RED%[ABORTADO]%RESET% Instalación cancelada por el usuario.
        goto :fin_error
    )
    echo %YELLOW%[AVISO]%RESET% Continuando sin privilegios de administrador...
) else (
    echo %GREEN%[OK]%RESET%   Ejecutando con permisos de Administrador.
)

:: ============================================================
:: PASO 1: Verificar winget
:: ============================================================
call :titulo "PASO 1/6  Verificando herramientas del sistema"

echo %CYAN%[INFO]%RESET% Comprobando winget (Windows Package Manager)...
winget --version >nul 2>&1
if %errorLevel% equ 0 (
    for /f "tokens=*" %%v in ('winget --version 2^>nul') do set "WINGET_VER=%%v"
    echo %GREEN%[OK]%RESET%   winget disponible: !WINGET_VER!
    set "WINGET_OK=1"
) else (
    echo %YELLOW%[AVISO]%RESET% winget no está disponible en este sistema.
    echo         Se usará descarga directa como método alternativo.
    echo         Para instalar winget: https://aka.ms/getwinget
)

:: Verificar PowerShell (necesario para descargas)
echo %CYAN%[INFO]%RESET% Comprobando PowerShell...
powershell -Command "Write-Host 'OK'" >nul 2>&1
if %errorLevel% equ 0 (
    for /f "tokens=*" %%v in ('powershell -Command "$PSVersionTable.PSVersion.Major"') do set "PS_VER=%%v"
    echo %GREEN%[OK]%RESET%   PowerShell !PS_VER! disponible.
) else (
    echo %RED%[ERROR]%RESET% PowerShell no está disponible. Es necesario para continuar.
    goto :fin_error
)

:: Verificar curl
echo %CYAN%[INFO]%RESET% Comprobando curl...
curl --version >nul 2>&1
if %errorLevel% equ 0 (
    echo %GREEN%[OK]%RESET%   curl disponible.
) else (
    echo %YELLOW%[AVISO]%RESET% curl no detectado. Se usará PowerShell para descargas.
)

:: ============================================================
:: PASO 2: Verificar / Instalar Java 21+
:: ============================================================
call :titulo "PASO 2/6  Verificando Java 21+"

call :verificar_java
if "!JAVA_OK!"=="1" goto :java_listo

:: Java no encontrado o versión insuficiente — intentar instalar
echo %CYAN%[INFO]%RESET% Intentando instalar Java 21 con winget...

if "!WINGET_OK!"=="1" (
    winget install --id Microsoft.OpenJDK.21 --accept-source-agreements --accept-package-agreements
    if !errorLevel! equ 0 (
        echo %GREEN%[OK]%RESET%   Java 21 instalado con winget.
        :: Recargar PATH
        call :recargar_path
        call :verificar_java
    ) else (
        echo %YELLOW%[AVISO]%RESET% winget falló. Intentando con Eclipse Adoptium...
        winget install --id EclipseAdoptium.Temurin.21.JDK --accept-source-agreements --accept-package-agreements
        if !errorLevel! equ 0 (
            echo %GREEN%[OK]%RESET%   Temurin JDK 21 instalado.
            call :recargar_path
            call :verificar_java
        )
    )
)

if "!JAVA_OK!"=="0" (
    echo %YELLOW%[AVISO]%RESET% Instalación automática no disponible. Descargando OpenJDK 21...
    call :descargar_java
    call :verificar_java
)

if "!JAVA_OK!"=="0" (
    echo %RED%[ERROR]%RESET% No se pudo instalar Java 21. Por favor instálalo manualmente:
    echo         https://adoptium.net/es/temurin/releases/?version=21
    goto :fin_error
)

:java_listo
echo %GREEN%[OK]%RESET%   Java listo: !JAVA_VERSION!
echo %GREEN%[OK]%RESET%   JAVA_HOME: "!JAVA_HOME!"

:: ============================================================
:: PASO 3: Verificar / Instalar Maven
:: ============================================================
call :titulo "PASO 3/6  Verificando Apache Maven 3.9+"

call :verificar_maven
if "!MAVEN_OK!"=="1" goto :maven_listo

echo %CYAN%[INFO]%RESET% Intentando instalar Maven con winget...

if "!WINGET_OK!"=="1" (
    winget install --id Apache.Maven --accept-source-agreements --accept-package-agreements
    if !errorLevel! equ 0 (
        echo %GREEN%[OK]%RESET%   Maven instalado con winget.
        call :recargar_path
        call :verificar_maven
    )
)

if "!MAVEN_OK!"=="0" (
    echo %YELLOW%[AVISO]%RESET% Instalando Maven manualmente en %MAVEN_INSTALL_DIR%...
    call :descargar_maven
    call :verificar_maven
)

if "!MAVEN_OK!"=="0" (
    echo %RED%[ERROR]%RESET% No se pudo instalar Maven. Instálalo manualmente:
    echo         https://maven.apache.org/download.cgi
    goto :fin_error
)

:maven_listo
echo %GREEN%[OK]%RESET%   Maven listo: !MAVEN_VERSION!

:: ============================================================
:: PASO 4: Verificar / Instalar MySQL 8+
:: ============================================================
call :titulo "PASO 4/6  Verificando MySQL 8+"

call :verificar_mysql
if "!MYSQL_OK!"=="1" goto :mysql_listo

echo %CYAN%[INFO]%RESET% Intentando instalar MySQL con winget...

if "!WINGET_OK!"=="1" (
    winget install --id Oracle.MySQL --accept-source-agreements --accept-package-agreements
    if !errorLevel! equ 0 (
        echo %GREEN%[OK]%RESET%   MySQL instalado con winget.
        call :recargar_path
        :: Esperar a que MySQL inicie su servicio
        echo %CYAN%[INFO]%RESET% Esperando inicio del servicio MySQL...
        timeout /t 10 /nobreak >nul
        call :verificar_mysql
    ) else (
        echo %YELLOW%[AVISO]%RESET% winget no pudo instalar MySQL.
    )
)

if "!MYSQL_OK!"=="0" (
    echo %YELLOW%[AVISO]%RESET% Abriendo descarga del instalador de MySQL...
    start "" "https://dev.mysql.com/downloads/installer/"
    echo.
    echo  %YELLOW%ACCIÓN REQUERIDA:%RESET%
    echo  ┌─────────────────────────────────────────────────────┐
    echo  │  1. Descarga "MySQL Installer" desde la página web  │
    echo  │  2. Ejecuta el instalador y elige "Developer Default"│
    echo  │  3. Configura root SIN contraseña (desarrollo local) │
    echo  │  4. Completa la instalación                         │
    echo  │  5. Vuelve aquí y presiona cualquier tecla          │
    echo  └─────────────────────────────────────────────────────┘
    echo.
    pause
    call :recargar_path
    call :verificar_mysql
)

if "!MYSQL_OK!"=="0" (
    echo %RED%[ERROR]%RESET% MySQL no está disponible. Instálalo manualmente y vuelve a ejecutar.
    goto :fin_error
)

:mysql_listo
echo %GREEN%[OK]%RESET%   MySQL listo: !MYSQL_VERSION!

:: ============================================================
:: PASO 5: Crear base de datos y tablas
:: ============================================================
call :titulo "PASO 5/6  Configurando base de datos"

echo %CYAN%[INFO]%RESET% Verificando conexión a MySQL...

:: Intentar conectarse con y sin contraseña
mysql -u root -e "SELECT 1;" >nul 2>&1
if %errorLevel% equ 0 (
    set "MYSQL_AUTH=-u root"
    echo %GREEN%[OK]%RESET%   Conectado a MySQL sin contraseña.
) else (
    mysql -u root -p"" -e "SELECT 1;" >nul 2>&1
    if !errorLevel! equ 0 (
        set "MYSQL_AUTH=-u root -p"""
        echo %GREEN%[OK]%RESET%   Conectado a MySQL con contraseña vacía.
    ) else (
        echo %YELLOW%[AVISO]%RESET% No se puede conectar sin contraseña.
        set /p ROOT_PASS="  Ingresa la contraseña de root de MySQL (Enter si no tiene): "
        set "MYSQL_AUTH=-u root -p!ROOT_PASS!"
        mysql !MYSQL_AUTH! -e "SELECT 1;" >nul 2>&1
        if !errorLevel! neq 0 (
            echo %RED%[ERROR]%RESET% No se pudo conectar a MySQL. Verifica que el servicio esté activo.
            echo         Intenta: net start MySQL80
            goto :fin_error
        )
        echo %GREEN%[OK]%RESET%   Conexión a MySQL exitosa.
    )
)

:: Crear base de datos
echo %CYAN%[INFO]%RESET% Creando base de datos '%DB_NAME%'...
mysql !MYSQL_AUTH! -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %errorLevel% equ 0 (
    echo %GREEN%[OK]%RESET%   Base de datos '%DB_NAME%' lista.
) else (
    echo %RED%[ERROR]%RESET% No se pudo crear la base de datos.
    goto :fin_error
)

:: Crear archivo SQL temporal
echo %CYAN%[INFO]%RESET% Generando script SQL de tablas...
set "SQL_TEMP=%TEMP%\incidentes_setup.sql"

(
echo -- =====================================================
echo -- SISTEMA DE GESTION DE INCIDENTES
echo -- Script de creacion de tablas e inserts
echo -- =====================================================
echo.
echo -- 1. TABLA: usuarios
echo CREATE TABLE IF NOT EXISTS usuarios ^(
echo     id              INT PRIMARY KEY AUTO_INCREMENT,
echo     nombre          VARCHAR^(100^)  NOT NULL,
echo     email           VARCHAR^(100^)  NOT NULL UNIQUE,
echo     password        VARCHAR^(255^)  NOT NULL,
echo     rol             ENUM^('ADMIN','TECNICO','USUARIO'^) NOT NULL DEFAULT 'USUARIO',
echo     telefono        VARCHAR^(20^),
echo     activo          BOOLEAN       DEFAULT TRUE,
echo     fecha_registro  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
echo ^);
echo.
echo -- 2. TABLA: incidentes
echo CREATE TABLE IF NOT EXISTS incidentes ^(
echo     id                  INT PRIMARY KEY AUTO_INCREMENT,
echo     titulo              VARCHAR^(200^) NOT NULL,
echo     descripcion         TEXT         NOT NULL,
echo     usuario_id          INT          NOT NULL,
echo     tecnico_id          INT          NULL,
echo     estado              ENUM^('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO'^) NOT NULL DEFAULT 'PENDIENTE',
echo     prioridad           ENUM^('BAJA','MEDIA','ALTA','CRITICA'^)               NOT NULL DEFAULT 'MEDIA',
echo     fecha_creacion      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
echo     fecha_actualizacion TIMESTAMP    NULL,
echo     FOREIGN KEY ^(usuario_id^) REFERENCES usuarios^(id^) ON DELETE CASCADE,
echo     FOREIGN KEY ^(tecnico_id^) REFERENCES usuarios^(id^) ON DELETE SET NULL
echo ^);
echo.
echo -- 3. TABLA: comentarios
echo CREATE TABLE IF NOT EXISTS comentarios ^(
echo     id           INT PRIMARY KEY AUTO_INCREMENT,
echo     incidente_id INT  NOT NULL,
echo     usuario_id   INT  NOT NULL,
echo     mensaje      TEXT NOT NULL,
echo     es_interno   BOOLEAN   DEFAULT FALSE,
echo     fecha        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
echo     FOREIGN KEY ^(incidente_id^) REFERENCES incidentes^(id^) ON DELETE CASCADE,
echo     FOREIGN KEY ^(usuario_id^)   REFERENCES usuarios^(id^)   ON DELETE CASCADE
echo ^);
echo.
echo -- 4. TABLA: historial_estados
echo CREATE TABLE IF NOT EXISTS historial_estados ^(
echo     id              INT PRIMARY KEY AUTO_INCREMENT,
echo     incidente_id    INT NOT NULL,
echo     estado_anterior ENUM^('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO'^),
echo     estado_nuevo    ENUM^('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO'^) NOT NULL,
echo     usuario_id      INT NOT NULL,
echo     fecha_cambio    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
echo     FOREIGN KEY ^(incidente_id^) REFERENCES incidentes^(id^) ON DELETE CASCADE,
echo     FOREIGN KEY ^(usuario_id^)   REFERENCES usuarios^(id^)   ON DELETE CASCADE
echo ^);
echo.
echo -- 5. TABLA: notificaciones
echo CREATE TABLE IF NOT EXISTS notificaciones ^(
echo     id             INT PRIMARY KEY AUTO_INCREMENT,
echo     usuario_id     INT  NOT NULL,
echo     mensaje        TEXT NOT NULL,
echo     leida          BOOLEAN   DEFAULT FALSE,
echo     fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
echo     FOREIGN KEY ^(usuario_id^) REFERENCES usuarios^(id^) ON DELETE CASCADE
echo ^);
echo.
echo -- 6. DATOS INICIALES
echo INSERT IGNORE INTO usuarios ^(nombre, email, password, rol, activo^) VALUES
echo     ^('Administrador del Sistema', 'admin@sistema.com',          'admin123',      'ADMIN',   TRUE^),
echo     ^('Carlos Tecnico',            'carlos.tecnico@sistema.com', 'carlostecnico', 'TECNICO', TRUE^),
echo     ^('Maria Tecnica',             'maria.tecnica@sistema.com',  'mariatecnica',  'TECNICO', TRUE^);
echo.
echo INSERT IGNORE INTO usuarios ^(nombre, email, password, rol, activo, telefono^) VALUES
echo     ^('Juan Perez', 'juan.perez@ejemplo.com', 'juanperez', 'USUARIO', TRUE, '555-1234'^),
echo     ^('Ana Gomez',  'ana.gomez@ejemplo.com',  'anagomez',  'USUARIO', TRUE, '555-5678'^);
) > "%SQL_TEMP%"

echo %CYAN%[INFO]%RESET% Ejecutando script SQL...
mysql !MYSQL_AUTH! %DB_NAME% < "%SQL_TEMP%"
if %errorLevel% equ 0 (
    echo %GREEN%[OK]%RESET%   Tablas y datos iniciales creados correctamente.
) else (
    echo %RED%[ERROR]%RESET% Error al ejecutar el script SQL.
    del "%SQL_TEMP%" >nul 2>&1
    goto :fin_error
)
del "%SQL_TEMP%" >nul 2>&1

:: Mostrar usuarios creados
echo.
echo %CYAN%[INFO]%RESET% Usuarios registrados en la base de datos:
echo.
mysql !MYSQL_AUTH! %DB_NAME% -e "SELECT id, nombre, email, rol FROM usuarios;" 2>nul
echo.

:: ============================================================
:: PASO 6: Compilar y ejecutar el proyecto
:: ============================================================
call :titulo "PASO 6/6  Compilando y ejecutando el proyecto"

if not exist "%PROYECTO_DIR%" (
    echo %YELLOW%[AVISO]%RESET% No se encontró la carpeta '%PROYECTO_DIR%' en el directorio actual.
    echo         Directorio actual: %CD%
    echo.
    echo         Opciones:
    echo           1. Asegúrate de ejecutar este .bat desde la carpeta que
    echo              contiene '%PROYECTO_DIR%'
    echo           2. Edita la variable PROYECTO_DIR al inicio del script
    echo.
    set /p COMPILAR="  ¿Deseas intentar compilar de todas formas? (s/n): "
    if /i "!COMPILAR!" neq "s" (
        echo %CYAN%[INFO]%RESET% Instalación de dependencias completada.
        echo         Cuando estés listo, ejecuta:
        echo           cd %PROYECTO_DIR%
        echo           mvn clean javafx:run
        goto :fin_ok
    )
)

if exist "%PROYECTO_DIR%" cd "%PROYECTO_DIR%"

echo %CYAN%[INFO]%RESET% Iniciando compilación con Maven...
echo         ^(La primera ejecución puede tardar varios minutos^)
echo.
mvn clean javafx:run

if %errorLevel% equ 0 (
    echo %GREEN%[OK]%RESET%   Proyecto compilado y ejecutado correctamente.
) else (
    echo %RED%[ERROR]%RESET% Error durante la compilación. Revisa los mensajes de Maven arriba.
    goto :fin_error
)

goto :fin_ok

:: ============================================================
:: SUBRUTINAS
:: ============================================================

:verificar_java
    set "JAVA_OK=0"
    set "JAVA_VERSION="
    set "JAVA_HOME="

    :: Buscar java en PATH
    where java >nul 2>&1
    if %errorLevel% neq 0 (
        :: Buscar en ubicaciones comunes de Windows
        call :buscar_java_en_disco
        if "!JAVA_OK!"=="0" exit /b
    )

    :: Obtener versión
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set "RAW_VER=%%v"
    )
    :: Limpiar comillas
    set "RAW_VER=!RAW_VER:"=!"

    :: Extraer número mayor de versión (Java 21.x.x -> 21)
    for /f "delims=." %%m in ("!RAW_VER!") do set "MAJOR=%%m"

    :: Java 9+ usa versión directa (21), Java 8 usa 1.8
    if "!MAJOR!"=="1" (
        for /f "tokens=2 delims=." %%m in ("!RAW_VER!") do set "MAJOR=%%m"
    )

    if !MAJOR! geq 21 (
        set "JAVA_OK=1"
        set "JAVA_VERSION=!RAW_VER!"
        :: Detectar JAVA_HOME si no está configurado
        if not defined JAVA_HOME (
            for /f "delims=" %%p in ('where java 2^>nul') do (
                set "JAVA_BIN=%%p"
                goto :java_home_encontrado
            )
            :java_home_encontrado
            for %%d in ("!JAVA_BIN!\..\..") do set "JAVA_HOME=%%~fd"
        )
    ) else (
        echo %YELLOW%[AVISO]%RESET% Java encontrado pero versión insuficiente: !RAW_VER! ^(se requiere 21+^)
    )
exit /b

:buscar_java_en_disco
    set "RUTAS_JAVA=C:\Program Files\Java;C:\Program Files\Eclipse Adoptium;C:\Program Files\Microsoft;C:\Program Files\OpenJDK;%USERPROFILE%\devtools\java21"
    for %%r in (%RUTAS_JAVA%) do (
        if exist "%%r" (
            for /d %%d in ("%%r\jdk*") do (
                if exist "%%d\bin\java.exe" (
                    set "JAVA_HOME=%%d"
                    set "PATH=%%d\bin;!PATH!"
                    exit /b
                )
            )
            for /d %%d in ("%%r\jre*") do (
                if exist "%%d\bin\java.exe" (
                    set "JAVA_HOME=%%d"
                    set "PATH=%%d\bin;!PATH!"
                    exit /b
                )
            )
        )
    )
exit /b

:verificar_maven
    set "MAVEN_OK=0"
    set "MAVEN_VERSION="

    where mvn >nul 2>&1
    if %errorLevel% neq 0 (
        :: Buscar en ubicación de instalación manual
        if exist "%MAVEN_INSTALL_DIR%\bin\mvn.cmd" (
            set "PATH=%MAVEN_INSTALL_DIR%\bin;!PATH!"
        ) else (
            exit /b
        )
    )

    for /f "tokens=3" %%v in ('mvn --version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        set "MAVEN_VERSION=%%v"
        goto :mvn_ver_ok
    )
    :mvn_ver_ok

    :: Extraer major.minor (3.9 -> 3 y 9)
    for /f "tokens=1,2 delims=." %%a in ("!MAVEN_VERSION!") do (
        set "MVN_MAJOR=%%a"
        set "MVN_MINOR=%%b"
    )
    if !MVN_MAJOR! geq 3 (
        if !MVN_MINOR! geq 9 (
            set "MAVEN_OK=1"
        ) else (
            if !MVN_MAJOR! gtr 3 set "MAVEN_OK=1"
        )
    )
    if "!MAVEN_OK!"=="0" (
        echo %YELLOW%[AVISO]%RESET% Maven encontrado pero versión insuficiente: !MAVEN_VERSION! ^(se requiere 3.9+^)
    )
exit /b

:verificar_mysql
    set "MYSQL_OK=0"
    set "MYSQL_VERSION="

    where mysql >nul 2>&1
    if %errorLevel% neq 0 (
        :: Buscar en ubicaciones comunes de MySQL en Windows
        set "RUTAS_MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin;C:\Program Files\MySQL\MySQL Server 8.1\bin;C:\Program Files\MySQL\MySQL Server 8.2\bin;C:\Program Files\MySQL\MySQL Server 8.3\bin;C:\Program Files\MySQL\MySQL Server 8.4\bin"
        for %%r in (%RUTAS_MYSQL%) do (
            if exist "%%r\mysql.exe" (
                set "PATH=%%r;!PATH!"
                goto :mysql_path_ok
            )
        )
        exit /b
        :mysql_path_ok
    )

    for /f "tokens=2" %%v in ('mysql --version 2^>^&1 ^| findstr /i "Distrib"') do (
        set "MYSQL_RAW=%%v"
        goto :mysql_ver_ok
    )
    :mysql_ver_ok
    set "MYSQL_VERSION=!MYSQL_RAW:,=!"

    for /f "delims=." %%m in ("!MYSQL_VERSION!") do set "MYSQL_MAJOR=%%m"
    if !MYSQL_MAJOR! geq 8 (
        set "MYSQL_OK=1"
    ) else (
        echo %YELLOW%[AVISO]%RESET% MySQL encontrado pero versión insuficiente: !MYSQL_VERSION! ^(se requiere 8+^)
    )
exit /b

:descargar_java
    echo %CYAN%[INFO]%RESET% Descargando OpenJDK 21 en '%JAVA_INSTALL_DIR%'...
    if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
    set "JAVA_ZIP=%TEMP%\openjdk21.zip"

    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%JAVA_URL%' -OutFile '%JAVA_ZIP%' -UseBasicParsing }" 2>nul
    if %errorLevel% equ 0 (
        echo %CYAN%[INFO]%RESET% Extrayendo OpenJDK 21...
        powershell -Command "Expand-Archive -Path '%JAVA_ZIP%' -DestinationPath '%TOOLS_DIR%\java_temp' -Force" 2>nul
        :: Mover la carpeta extraída al destino final
        for /d %%d in ("%TOOLS_DIR%\java_temp\jdk*") do (
            if exist "%%d" (
                move "%%d" "%JAVA_INSTALL_DIR%" >nul 2>&1
            )
        )
        rd /s /q "%TOOLS_DIR%\java_temp" >nul 2>&1
        del "%JAVA_ZIP%" >nul 2>&1

        if exist "%JAVA_INSTALL_DIR%\bin\java.exe" (
            set "JAVA_HOME=%JAVA_INSTALL_DIR%"
            set "PATH=%JAVA_INSTALL_DIR%\bin;!PATH!"
            echo %GREEN%[OK]%RESET%   OpenJDK 21 instalado en %JAVA_INSTALL_DIR%
            :: Agregar al PATH del usuario permanentemente
            setx JAVA_HOME "%JAVA_INSTALL_DIR%" >nul 2>&1
            setx PATH "%JAVA_INSTALL_DIR%\bin;%PATH%" >nul 2>&1
        )
    ) else (
        echo %YELLOW%[AVISO]%RESET% Descarga automática falló. Abriendo página de descarga...
        start "" "https://adoptium.net/es/temurin/releases/?version=21&os=windows&arch=x64&package=jdk"
        echo.
        echo  %YELLOW%ACCIÓN REQUERIDA:%RESET%
        echo  Descarga e instala Temurin JDK 21, luego presiona cualquier tecla.
        pause
    )
exit /b

:descargar_maven
    echo %CYAN%[INFO]%RESET% Descargando Apache Maven 3.9 en '%MAVEN_INSTALL_DIR%'...
    if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
    set "MVN_ZIP=%TEMP%\maven.zip"

    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MVN_ZIP%' -UseBasicParsing }" 2>nul
    if %errorLevel% equ 0 (
        echo %CYAN%[INFO]%RESET% Extrayendo Maven...
        powershell -Command "Expand-Archive -Path '%MVN_ZIP%' -DestinationPath '%TOOLS_DIR%\mvn_temp' -Force" 2>nul
        for /d %%d in ("%TOOLS_DIR%\mvn_temp\apache-maven*") do (
            if exist "%%d" move "%%d" "%MAVEN_INSTALL_DIR%" >nul 2>&1
        )
        rd /s /q "%TOOLS_DIR%\mvn_temp" >nul 2>&1
        del "%MVN_ZIP%" >nul 2>&1

        if exist "%MAVEN_INSTALL_DIR%\bin\mvn.cmd" (
            set "PATH=%MAVEN_INSTALL_DIR%\bin;!PATH!"
            echo %GREEN%[OK]%RESET%   Maven instalado en %MAVEN_INSTALL_DIR%
            setx PATH "%MAVEN_INSTALL_DIR%\bin;%PATH%" >nul 2>&1
        )
    ) else (
        echo %YELLOW%[AVISO]%RESET% Descarga automática de Maven falló.
        echo         Descárgalo manualmente desde: https://maven.apache.org/download.cgi
    )
exit /b

:recargar_path
    :: Recargar variables de entorno sin reiniciar la consola
    for /f "tokens=2*" %%a in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v PATH 2^>nul') do set "SYS_PATH=%%b"
    for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v PATH 2^>nul') do set "USR_PATH=%%b"
    if defined SYS_PATH set "PATH=!SYS_PATH!"
    if defined USR_PATH set "PATH=!PATH!;!USR_PATH!"
exit /b

:titulo
    echo.
    echo %BOLD%%CYAN%══════════════════════════════════════════════════════════%RESET%
    echo %BOLD%%CYAN%  %~1%RESET%
    echo %BOLD%%CYAN%══════════════════════════════════════════════════════════%RESET%
    echo.
exit /b

:: ============================================================
:: FIN - OK
:: ============================================================
:fin_ok
echo.
echo %BOLD%%GREEN%╔══════════════════════════════════════════════════════════╗%RESET%
echo %BOLD%%GREEN%║              INSTALACIÓN COMPLETADA                      ║%RESET%
echo %BOLD%%GREEN%╚══════════════════════════════════════════════════════════╝%RESET%
echo.
echo %BOLD%Credenciales por defecto:%RESET%
echo.
echo   %GREEN%ADMIN%RESET%   →  admin@sistema.com           /  admin123
echo   %CYAN%TECNICO%RESET% →  carlos.tecnico@sistema.com  /  carlostecnico
echo   %CYAN%TECNICO%RESET% →  maria.tecnica@sistema.com   /  mariatecnica
echo   %YELLOW%USUARIO%RESET% →  juan.perez@ejemplo.com       /  juanperez
echo   %YELLOW%USUARIO%RESET% →  ana.gomez@ejemplo.com        /  anagomez
echo.
echo %BOLD%Para ejecutar el proyecto en el futuro:%RESET%
echo   cd %PROYECTO_DIR%
echo   mvn clean javafx:run
echo.
echo %BOLD%Herramientas instaladas en:%RESET%
echo   Java:  %JAVA_HOME%
if exist "%MAVEN_INSTALL_DIR%" echo   Maven: %MAVEN_INSTALL_DIR%
echo.
pause
endlocal
exit /b 0

:: ============================================================
:: FIN - ERROR
:: ============================================================
:fin_error
echo.
echo %RED%╔══════════════════════════════════════════════════════════╗%RESET%
echo %RED%║          INSTALACIÓN FINALIZADA CON ERRORES              ║%RESET%
echo %RED%╚══════════════════════════════════════════════════════════╝%RESET%
echo.
echo  Revisa los mensajes de error anteriores e intenta de nuevo.
echo  Si el problema persiste, instala manualmente:
echo.
echo    Java 21:  https://adoptium.net/es/temurin/releases/?version=21
echo    Maven:    https://maven.apache.org/download.cgi
echo    MySQL 8:  https://dev.mysql.com/downloads/installer/
echo.
pause
endlocal
exit /b 1

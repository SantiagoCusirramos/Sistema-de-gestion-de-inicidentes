# Manual de Usuario - Sistema de Gestion de Incidentes

## 1. Requisitos

- **Java** JDK 21 o superior (recomendado JDK 25)
- **MySQL** 8.0 o superior (o XAMPP con MySQL)
- **Maven** 3.9+ (para compilar y ejecutar)

---

## 2. Como ejecutar el programa

### 2.1 Iniciar MySQL

Con XAMPP en Windows:
```
C:\xampp\mysql\bin\mysqld.exe --console
```

Con Linux:

```
sudo systemctl start mysql

sudo systemctl status mysql

sudo mysql -u root
```

### 2.2 Crear la base de datos

Windows:
```
C:\xampp\mysql\bin\mysql.exe -u root -e "CREATE DATABASE IF NOT EXISTS sistema_incidentes;"
```

Linux:

En Linux, MySQL suele requerir autenticación. Para que el programa funcione, debes asegurarte de que la contraseña coincida con la configurada en `Utils/ConexionBD.java`.

**Opción A: Dejar MySQL sin contraseña (recomendado para desarrollo)**

```
sudo mysql -u root
```

Dentro de MySql agregar el siguiente rool

```
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
FLUSH PRIVILEGES;
EXIT;
```


### 2.3 Crear las tablas e inserts iniciales (PASO OBLIGATORIO PARA QUE FUNCIONE)

Para ejecutar este comando, saber exactamente donde esta el script

Windows:
```
C:\xampp\mysql\bin\mysql.exe -u root sistema_incidentes < script_inicial.sql
```

Linux:
```
sudo mysql -u root sistema_incidentes < script_inicial.sql
```

O en caso de querer ir por la segura, ejecutar la siguiente instruccion dentro de tu terminal SQL:

```
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
('Administrador del Sistema', 'admin@sistema.com', 'admin123', 'ADMIN', TRUE);

-- Técnicos de ejemplo (contraseña: tecnico123 en Base64)
INSERT INTO usuarios (nombre, email, password, rol, activo) VALUES 
('Carlos Técnico', 'carlos.tecnico@sistema.com', 'carlostecnico', 'TECNICO', TRUE),
('María Técnica', 'maria.tecnica@sistema.com', 'mariatecnica', 'TECNICO', TRUE);

-- Usuarios normales de ejemplo (contraseña: usuario123 en Base64)
INSERT INTO usuarios (nombre, email, password, rol, activo, telefono) VALUES 
('Juan Pérez', 'juan.perez@ejemplo.com', 'juanperez', 'USUARIO', TRUE, '555-1234'),
('Ana Gómez', 'ana.gomez@ejemplo.com', 'anagomez', 'USUARIO', TRUE, '555-5678');


-- =====================================================
-- 7. VERIFICACIÓN DE DATOS INSERTADOS
-- =====================================================
SELECT '=== USUARIOS CREADOS ===' as '';
SELECT id, nombre, email, rol FROM usuarios;

```



### 2.4 Compilar y ejecutar

Windows:
```powershell
cd Gestion_Incidentes
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
C:\tools\apache-maven-3.9.8\bin\mvn.cmd clean javafx:run
```

> En la primera ejecucion, el programa crea automaticamente las 5 tablas e inserta el usuario administrador por defecto, en caso esto falle, se ha colocado la solucion en el paso 2.3 

Linux:

```
cd Gestion_Incidentes
mvn clean javafx:run
```

---


## 3. Credenciales por defecto

| Rol | Email | Contrasena |
|---|---|---|
| **ADMIN** | `admin@sistema.com` | `admin123` |
| **TECNICO** | `carlos.tecnico@sistema.com` | `tecnico123` |
| **TECNICO** | `maria.tecnica@sistema.com` | `tecnico123` |
| **USUARIO** | `juan.perez@ejemplo.com` | `usuario123` |
| **USUARIO** | `ana.gomez@ejemplo.com` | `usuario123` |
---


## 4. Roles y permisos

| Accion | ADMIN | TECNICO | USUARIO |
|---|---|---|---|
| Crear incidente | ✓ | ✓ | ✓ |
| Ver todos los incidentes | ✓ | Solo los suyos | Solo los suyos |
| Asignar tecnico | ✓ | ✗ | ✗ |
| Resolver incidente | ✓ | ✓ | ✗ |
| Cambiar estado | ✓ | ✓ | ✗ |
| Cerrar incidente | ✓ | Solo los suyos | Solo los suyos |
| Agregar comentarios | ✓ | ✓ | ✓ |
| Ver reportes | ✓ | ✓ | Solo sus stats |

---

## 5. Funcionalidades paso a paso

### 5.1 Pantalla de login

Al iniciar aparecen dos pestanas:

- **Iniciar Sesion**: Ingrese email y contrasena, luego haga clic en "Iniciar Sesion".
- **Registrarse**: Complete nombre, email, contrasena (min. 6 caracteres) y confirme. Los nuevos usuarios siempre se crean con rol USUARIO.

### 5.2 Panel principal (Dashboard)

Una vez autenticado, se abre el panel con pestanas:

- **Incidentes**: Gestion completa de incidentes.
- **Reportes**: Estadisticas y graficos.
- **Usuarios** (solo ADMIN): Administracion de usuarios.

En la barra superior se muestra su nombre, rol y un boton para cerrar sesion.

### 5.3 Gestion de incidentes

La pestana "Incidentes" contiene una tabla con todos los incidentes visibles segun su rol y una barra de herramientas con botones:

1. **Nuevo Incidente**: Abre un dialogo para ingresar titulo, descripcion y prioridad (Baja/Media/Alta/Critica).
2. **Ver Detalle**: Muestra la informacion completa del incidente seleccionado, incluyendo comentarios.
3. **Asignar Tecnico** (solo ADMIN): Permite elegir un tecnico de la lista para que atienda el incidente. Al asignarlo, el estado cambia automaticamente a "EN_PROCESO".
4. **Resolver** (TECNICO/ADMIN): Solicita una descripcion de la solucion. El estado pasa a "RESUELTO" y se registra la solucion como comentario.
5. **Cambiar Estado** (TECNICO/ADMIN): Permite cambiar manualmente entre Pendiente/En Proceso/Resuelto/Cerrado.
6. **Cerrar**: El creador del incidente o un ADMIN pueden cerrarlo definitivamente.
7. **Agregar Comentario**: Anade un comentario al incidente seleccionado.
8. **Actualizar**: Recarga la tabla de incidentes.

### 5.4 Reportes y estadisticas

La pestana "Reportes" muestra:

- **Tarjetas de resumen**: Total, Pendientes, En Proceso, Resueltos, Cerrados (con codigo de colores).
- **Grafico de pastel**: Distribucion de incidentes por prioridad.
- **Grafico de barras**: Incidentes agrupados por estado.
- **Tabla de recientes**: Los ultimos 15 incidentes creados.

---

## 6. Arquitectura del programa

```
Main (JavaFX Application)
  |
  +-- LoginView (ventana de autenticacion)
  |     +-- AuthController
  |           +-- UsuarioService
  |                 +-- UsuarioDAOImpl --> Base de datos
  |
  +-- DashboardView (panel principal con pestanas)
        |
        +-- IncidenteView (pestana de incidentes)
        |     +-- IncidenteController
        |           +-- IncidenteService
        |           |     +-- IncidenteDAOImpl --> Base de datos
        |           |     +-- ComentarioDAOImpl --> Base de datos
        |           +-- UsuarioService
        |
        +-- ReporteView (pestana de reportes)
              +-- IncidenteController
              +-- ReporteService
                    +-- IncidenteDAOImpl --> Base de datos
```

**Capas del software:**

- **View** (JavaFX): Interfaz grafica con ventanas, tablas, botones y graficos.
- **Controller**: Orquesta las operaciones, valida permisos y protege las reglas de negocio.
- **Service**: Logica de negocio (crear incidentes, autenticar, generar reportes).
- **DAO** (Data Access Object): Operaciones CRUD contra la base de datos via JDBC.
- **Model**: Objetos que representan las entidades (Usuario, Incidente, Comentario).
- **Exceptions**: Clases de error especificas para cada tipo de fallo.

---

## 7. Donde y como se almacenan los datos

### Base de datos: MySQL

El programa se conecta a MySQL en `localhost:3306` y almacena todo en la base de datos `sistema_incidentes`. La configuracion esta en `Utils/ConexionBD.java`:

```java
URL = "jdbc:mysql://localhost:3306/sistema_incidentes"
USUARIO = "root"
PASSWORD = ""
```

### Estructura de la base de datos (5 tablas)

**usuarios**
| Columna | Tipo | Descripcion |
|---|---|---|
| id | INT (PK, AUTO_INCREMENT) | Identificador unico |
| nombre | VARCHAR(100) | Nombre del usuario |
| email | VARCHAR(100) (UNIQUE) | Correo electronico |
| password | VARCHAR(255) | Contrasena codificada en Base64 |
| rol | ENUM(ADMIN,TECNICO,USUARIO) | Rol del usuario |
| telefono | VARCHAR(20) | Telefono (opcional) |
| activo | BOOLEAN | Si la cuenta esta activa |
| fecha_registro | TIMESTAMP | Fecha de creacion |

**incidentes**
| Columna | Tipo | Descripcion |
|---|---|---|
| id | INT (PK, AUTO_INCREMENT) | Identificador unico |
| titulo | VARCHAR(200) | Titulo del incidente |
| descripcion | TEXT | Descripcion detallada |
| usuario_id | INT (FK -> usuarios) | Creador del incidente |
| tecnico_id | INT (FK -> usuarios, NULL) | Tecnico asignado |
| estado | ENUM(PENDIENTE,EN_PROCESO,RESUELTO,CERRADO) | Estado actual |
| prioridad | ENUM(BAJA,MEDIA,ALTA,CRITICA) | Nivel de prioridad |
| fecha_creacion | TIMESTAMP | Cuando se creo |
| fecha_actualizacion | TIMESTAMP (NULL) | Ultima modificacion |

**comentarios**
| Columna | Tipo | Descripcion |
|---|---|---|
| id | INT (PK, AUTO_INCREMENT) | Identificador |
| incidente_id | INT (FK -> incidentes) | Incidente asociado |
| usuario_id | INT (FK -> usuarios) | Autor del comentario |
| mensaje | TEXT | Contenido del comentario |
| fecha | TIMESTAMP | Cuando se publico |

**historial_estados** - Registra cada cambio de estado en los incidentes.
**notificaciones** - Alertas pendientes para los usuarios (funcionalidad preparada para futuro).

### Como se conecta y almacena

1. **Conexion Singleton**: `ConexionBD.getConnection()` mantiene una unica conexion abierta durante toda la ejecucion, reutilizandola para todas las consultas.
2. **DAO con JDBC**: Cada operacion (crear, leer, actualizar, eliminar) se ejecuta mediante `PreparedStatement` de JDBC, lo que previene inyeccion SQL.
3. **Auto-creacion de tablas**: Al iniciar, `ConexionBD.inicializarBaseDatos()` ejecuta `CREATE TABLE IF NOT EXISTS` para las 5 tablas. Si ya existen, no las modifica.
4. **Contrasenas**: Se almacenan codificadas en Base64 (no es criptografia segura, es una simplificacion educativa).

---

## 8. Flujo de vida de un incidente

```
  [USUARIO]        [ADMIN]          [TECNICO]
     |                |                |
  1. Crea incidente   |                |
     | (PENDIENTE)    |                |
     |----+---------->|                |
     |                | 2. Asigna      |
     |                |    tecnico     |
     |                |    (EN_PROCESO) |
     |                |----+---------->|
     |                |                | 3. Resuelve
     |                |                |    (RESUELTO)
     |                |                |----+--->
     |                |                |
  4. Cierra           |                |
     (CERRADO)        |                |
     |<---------------+---------------|
     v
  [HISTORIAL REGISTRADO]
```

---

## 9. Posibles errores y soluciones

| Problema | Causa | Solucion |
|---|---|---|
| "Driver MySQL no encontrado" | Falta el JAR del conector MySQL | Maven lo descarga automaticamente |
| "Error al conectar a la base de datos" | MySQL no esta corriendo | Ejecutar `mysqld.exe` o iniciar el servicio |
| Pantalla en blanco al ejecutar | JavaFX no encuentra modulo grafico | Usar `mvn clean javafx:run` en vez de `java` directamente |
| Error "Unknown database 'sistema_incidentes'" | La BD no fue creada | Ejecutar `CREATE DATABASE sistema_incidentes` en MySQL |

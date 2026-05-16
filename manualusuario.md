# Manual de Usuario - Sistema de Gestión de Incidentes

## 1. Requisitos

- **Java** JDK 21 o superior (recomendado JDK 25)
- **MySQL** 8.0 o superior
- **Maven** 3.9+ (para compilar y ejecutar)

---

## 2. Cómo ejecutar el programa

### 2.1 Iniciar MySQL

```bash
sudo systemctl start mysql
sudo systemctl status mysql
```

### 2.2 Crear la base de datos

```bash
sudo mysql -u root -e "CREATE DATABASE IF NOT EXISTS sistema_incidentes;"
```

### 2.3 Configurar las variables de entorno

Para las variables de entorno se recomienda realizar lo siguiente en la consola, antes de ejecutar el programa. Asegúrese de haber creado la base de datos (paso 2.2):

```bash
export DB_URL="jdbc:mysql://localhost:3306/sistema_incidentes?serverTimezone=UTC"
export DB_USER="root"
export DB_PASS=""
export ADMIN_INITIAL_PASSWORD="Admin123@"
```

### 2.4 Primera ejecución - Script automatizado

Después de haber realizado lo anterior, bastará con ejecutar alguno de los scripts. Actualmente se cuenta con soporte para:

- **Arch Linux y sus variantes**: `manjaro.sh`
- **Ubuntu y sus variantes**: `ubunto.sh`

Recuerde darles los permisos correspondientes antes de ejecutarlos:

```bash
chmod +x ubunto.sh
./ubunto.sh
```

```bash
chmod +x manjaro.sh
./manjaro.sh
```

### 2.5 Compilar y ejecutar (ejecuciones posteriores)

Después de la primera ejecución con el script, si desea ejecutar nuevamente la aplicación, bastará con ubicarse en el proyecto y ejecutarlo con Maven:

```bash
cd Gestion_Incidentes   # En caso de estar en la carpeta raíz, nos movemos al directorio del proyecto
mvn clean javafx:run
```

> **Nota:** En la primera ejecución, el programa crea automáticamente las 5 tablas e inserta el usuario administrador por defecto, tomando la contraseña del administrador desde la variable de entorno `ADMIN_INITIAL_PASSWORD`. Se aplican ciertos criterios de seguridad.

---

## 3. Credenciales por defecto

Para saber cómo configurar las variables de entorno, revise el paso 2.3.

| Rol | Email | Contraseña |
|---|---|---|
| **ADMIN** | `admin@sistema.in` | `Valor de la variable de entorno ADMIN_INITIAL_PASSWORD` |

> La contraseña del administrador se define mediante la variable de entorno `ADMIN_INITIAL_PASSWORD`. En los ejemplos se usa `Admin123@`, pero usted puede elegir cualquier contraseña que cumpla con los criterios de seguridad.

---

## 4. Roles y permisos

| Acción | ADMIN | TECNICO | USUARIO |
|---|---|---|---|
| Crear incidente | ✓ | ✓ | ✓ |
| Ver todos los incidentes | ✓ | Solo los suyos | Solo los suyos |
| Asignar técnico | ✓ | ✗ | ✗ |
| Resolver incidente | ✓ | ✓ | ✗ |
| Cambiar estado | ✓ | ✓ | ✗ |
| Cerrar incidente | ✓ | Solo los suyos | Solo los suyos |
| Agregar comentarios | ✓ | ✓ | ✓ |
| Ver reportes | ✓ | ✓ | Solo sus estadísticas |
| Administrar usuarios | ✓ | ✗ | ✗ |

---

## 5. Funcionalidades paso a paso

### 5.1 Pantalla de login

Al iniciar aparecen dos pestañas:

- **Iniciar Sesión**: Ingrese email y contraseña, luego haga clic en "Iniciar Sesión".
- **Registrarse**: Complete nombre, email, contraseña (mín. 6 caracteres) y confirme. Los nuevos usuarios siempre se crean con rol USUARIO.

### 5.2 Panel principal (Dashboard)

Una vez autenticado, se abre el panel con pestañas:

- **Incidentes**: Gestión completa de incidentes.
- **Reportes**: Estadísticas y gráficos.
- **Usuarios** (solo ADMIN): Administración de usuarios.

En la barra superior se muestra su nombre, rol y un botón para cerrar sesión.

### 5.3 Gestión de incidentes

La pestaña "Incidentes" contiene una tabla con todos los incidentes visibles según su rol y una barra de herramientas con botones:

1. **Nuevo Incidente**: Abre un diálogo para ingresar título, descripción y prioridad (Baja/Media/Alta/Crítica).
2. **Ver Detalle**: Muestra la información completa del incidente seleccionado, incluyendo comentarios.
3. **Asignar Técnico** (solo ADMIN): Permite elegir un técnico de la lista para que atienda el incidente. Al asignarlo, el estado cambia automáticamente a "EN_PROCESO".
4. **Resolver** (TÉCNICO/ADMIN): Solicita una descripción de la solución. El estado pasa a "RESUELTO" y se registra la solución como comentario.
5. **Cambiar Estado** (TÉCNICO/ADMIN): Permite cambiar manualmente entre Pendiente/En Proceso/Resuelto/Cerrado.
6. **Cerrar**: El creador del incidente o un ADMIN pueden cerrarlo definitivamente.
7. **Agregar Comentario**: Añade un comentario al incidente seleccionado.
8. **Actualizar**: Recarga la tabla de incidentes.

### 5.4 Reportes y estadísticas

La pestaña "Reportes" muestra:

- **Tarjetas de resumen**: Total, Pendientes, En Proceso, Resueltos, Cerrados (con código de colores).
- **Gráfico de pastel**: Distribución de incidentes por prioridad.
- **Gráfico de barras**: Incidentes agrupados por estado.
- **Tabla de recientes**: Los últimos 15 incidentes creados.

---

## 6. Arquitectura del programa

```
Main (JavaFX Application)
  |
  +-- LoginView (ventana de autenticación)
  |     +-- AuthController
  |           +-- UsuarioService
  |                 +-- UsuarioDAOImpl --> Base de datos
  |
  +-- DashboardView (panel principal con pestañas)
        |
        +-- IncidenteView (pestaña de incidentes)
        |     +-- IncidenteController
        |           +-- IncidenteService
        |           |     +-- IncidenteDAOImpl --> Base de datos
        |           |     +-- ComentarioDAOImpl --> Base de datos
        |           +-- UsuarioService
        |
        +-- ReporteView (pestaña de reportes)
              +-- IncidenteController
              +-- ReporteService
                    +-- IncidenteDAOImpl --> Base de datos
```

**Capas del software:**

- **View** (JavaFX): Interfaz gráfica con ventanas, tablas, botones y gráficos.
- **Controller**: Orquesta las operaciones, valida permisos y protege las reglas de negocio.
- **Service**: Lógica de negocio (crear incidentes, autenticar, generar reportes).
- **DAO** (Data Access Object): Operaciones CRUD contra la base de datos vía JDBC.
- **Model**: Objetos que representan las entidades (Usuario, Incidente, Comentario).
- **Exceptions**: Clases de error específicas para cada tipo de fallo.

---

## 7. Estructura de la base de datos

El programa utiliza MySQL como motor de base de datos. La conexión se establece mediante `localhost:3306` y los datos se almacenan en la base de datos `sistema_incidentes`. La configuración se lee desde las variables de entorno definidas en el paso 2.3.

A continuación, se detalla la estructura completa de las **5 tablas** que componen la base de datos:

---

### 7.1 Tabla `usuarios`

Almacena la información de todos los usuarios del sistema.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Identificador único del usuario |
| `nombre` | VARCHAR(100) | NOT NULL | Nombre completo del usuario |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE | Correo electrónico (usado para login) |
| `password` | VARCHAR(255) | NOT NULL | Contraseña encriptada con BCrypt |
| `rol` | ENUM('ADMIN','TECNICO','USUARIO') | NOT NULL | Rol del usuario en el sistema |
| `telefono` | VARCHAR(20) | NULLABLE | Número de teléfono (opcional) |
| `activo` | BOOLEAN | DEFAULT TRUE | Indica si la cuenta está activa |
| `fecha_registro` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha y hora de creación de la cuenta |
| `ultimo_login` | TIMESTAMP | NULLABLE | Último inicio de sesión registrado |
| `intentos_fallidos` | INT | NOT NULL DEFAULT 0 | Contador de intentos fallidos de login |
| `bloqueado_hasta` | TIMESTAMP | NULLABLE | Fecha hasta la que la cuenta está bloqueada |

---

### 7.2 Tabla `incidentes`

Almacena los incidentes reportados por los usuarios.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Identificador único del incidente |
| `titulo` | VARCHAR(200) | NOT NULL | Título breve del incidente |
| `descripcion` | TEXT | NOT NULL | Descripción detallada del problema |
| `usuario_id` | INT | NOT NULL, FOREIGN KEY → usuarios(id) | Usuario que reportó el incidente |
| `tecnico_id` | INT | NULLABLE, FOREIGN KEY → usuarios(id) | Técnico asignado (si existe) |
| `estado` | ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO') | NOT NULL DEFAULT 'PENDIENTE' | Estado actual del incidente |
| `prioridad` | ENUM('BAJA','MEDIA','ALTA','CRITICA') | NOT NULL DEFAULT 'MEDIA' | Nivel de prioridad |
| `fecha_creacion` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha de creación del incidente |
| `fecha_actualizacion` | TIMESTAMP | NULLABLE | Última modificación del incidente |

---

### 7.3 Tabla `comentarios`

Almacena los comentarios asociados a cada incidente.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Identificador único del comentario |
| `incidente_id` | INT | NOT NULL, FOREIGN KEY → incidentes(id) | Incidente al que pertenece el comentario |
| `usuario_id` | INT | NOT NULL, FOREIGN KEY → usuarios(id) | Usuario que escribió el comentario |
| `mensaje` | TEXT | NOT NULL | Contenido del comentario |
| `es_interno` | BOOLEAN | DEFAULT FALSE | Indica si es un comentario interno (solo técnicos/ADMIN) |
| `fecha` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha y hora del comentario |

---

### 7.4 Tabla `historial_estados`

Registra cada cambio de estado que sufre un incidente, permitiendo la trazabilidad completa.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Identificador único del registro |
| `incidente_id` | INT | NOT NULL, FOREIGN KEY → incidentes(id) | Incidente afectado |
| `estado_anterior` | ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO') | NULLABLE | Estado previo al cambio |
| `estado_nuevo` | ENUM('PENDIENTE','EN_PROCESO','RESUELTO','CERRADO') | NOT NULL | Estado después del cambio |
| `usuario_id` | INT | NOT NULL, FOREIGN KEY → usuarios(id) | Usuario que realizó el cambio |
| `fecha_cambio` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Momento exacto del cambio |

---

### 7.5 Tabla `notificaciones`

Sistema de notificaciones pendientes para los usuarios (funcionalidad preparada para futuras versiones).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Identificador único de la notificación |
| `usuario_id` | INT | NOT NULL, FOREIGN KEY → usuarios(id) | Usuario destinatario |
| `mensaje` | TEXT | NOT NULL | Contenido de la notificación |
| `leida` | BOOLEAN | DEFAULT FALSE | Indica si el usuario ya la ha leído |
| `fecha_creacion` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha de generación de la notificación |

---

### 7.6 Resumen de relaciones entre tablas

```
usuarios (1) ──────< (N) incidentes      (un usuario crea muchos incidentes)
usuarios (1) ──────< (N) incidentes      (un técnico atiende muchos incidentes)
incidentes (1) ─────< (N) comentarios    (un incidente tiene muchos comentarios)
incidentes (1) ─────< (N) historial      (un incidente tiene muchos cambios de estado)
usuarios (1) ──────< (N) notificaciones  (un usuario recibe muchas notificaciones)
```

---

### 7.7 Seguridad de contraseñas

Las contraseñas de los usuarios **no se almacenan en texto plano**. El sistema utiliza **BCrypt**, un algoritmo de hash diseñado específicamente para contraseñas, que incluye un "salt" (valor añadido) y es computacionalmente costoso para dificultar ataques de fuerza bruta.

```java
// Ejemplo de encriptación con BCrypt
String passwordEncriptada = BCrypt.hashpw(password, BCrypt.gensalt());
```

> **Nota:** BCrypt es el estándar recomendado para almacenamiento de contraseñas en aplicaciones Java. A diferencia de Base64 (que es codificación reversible), BCrypt es un hash unidireccional y seguro.

---

## 8. Flujo de vida de un incidente

```
  [USUARIO]        [ADMIN]          [TECNICO]
     |                |                |
  1. Crea incidente   |                |
     | (PENDIENTE)    |                |
     |----+---------->|                |
     |                | 2. Asigna      |
     |                |    técnico     |
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

| Problema | Causa | Solución |
|---|---|---|
| `Variable de entorno ADMIN_INITIAL_PASSWORD no configurada` | Falta la variable de entorno | Ejecutar `export ADMIN_INITIAL_PASSWORD="Admin123@"` |
| `Access denied for user 'root'@'localhost'` | Contraseña de MySQL incorrecta | Ejecutar `sudo mysql` y luego `ALTER USER 'root'@'localhost' IDENTIFIED BY '';` |
| `Unknown database 'sistema_incidentes'` | La base de datos no fue creada | Ejecutar `sudo mysql -e "CREATE DATABASE sistema_incidentes;"` |
| `Connection refused` | MySQL no está corriendo | Ejecutar `sudo systemctl start mysql` |
| Pantalla en blanco al ejecutar | JavaFX no encuentra módulos gráficos | Usar `mvn clean javafx:run` en lugar de `java -jar` |
| `Driver not found` | Falta el conector MySQL | Maven descarga la dependencia automáticamente al compilar |
| `Tablas no encontradas` | La base de datos no está inicializada | Ejecutar el script `.sh` correspondiente o crear las tablas manualmente |

---

## 10. Comandos útiles

### Reiniciar todo desde cero

```bash
# 1. Borrar base de datos
sudo mysql -u root -e "DROP DATABASE IF EXISTS sistema_incidentes;"

# 2. Crear base de datos nueva
sudo mysql -u root -e "CREATE DATABASE sistema_incidentes;"

# 3. Ejecutar el script de inicialización
./ubunto.sh   # o ./manjaro.sh según su distribución
```

### Ver datos en la base de datos

```bash
# Entrar a MySQL
sudo mysql -u root

# Seleccionar la base de datos y ver usuarios
USE sistema_incidentes;
SELECT id, nombre, email, rol FROM usuarios;

# Ver incidentes
SELECT * FROM incidentes;

# Salir
EXIT;
```

### Detener MySQL

```bash
sudo systemctl stop mysql
```

### Ver logs del programa

Los logs se muestran directamente en la terminal donde se ejecutó `mvn javafx:run` o el script correspondiente.

---

## 11. Scripts de ayuda

El proyecto incluye los siguientes scripts para facilitar la ejecución en diferentes distribuciones de Linux:

| Script | Distribución | Función |
|--------|--------------|---------|
| `ubunto.sh` | Ubuntu y derivados (Debian, Pop!_OS, Linux Mint) | Instalación automática de dependencias, configuración de MySQL, creación de tablas y ejecución del programa |
| `manjaro.sh` | Arch Linux y derivados (Manjaro, EndeavourOS, Garuda) | Misma funcionalidad pero adaptada para pacman (en lugar de apt) |

### Cómo ejecutar los scripts

```bash
# Dar permisos de ejecución
chmod +x ubunto.sh
chmod +x manjaro.sh

# Ejecutar según su distribución
./ubunto.sh      # Para Ubuntu/Debian/Pop!_OS
./manjaro.sh     # Para Arch/Manjaro
```

### Qué hace cada script

1. **Actualiza los repositorios** del sistema.
2. **Instala las dependencias** necesarias (Java JDK 21, Maven, MySQL, Git).
3. **Inicializa y configura MySQL** (lo deja sin contraseña para desarrollo).
4. **Crea la base de datos** `sistema_incidentes` si no existe.
5. **Crea las tablas** necesarias (5 tablas).
6. **Configura las variables de entorno** necesarias para el programa.
7. **Compila el proyecto** con Maven.
8. **Ejecuta la aplicación** (pregunta antes de hacerlo).

> **Nota:** La primera ejecución del script puede tomar varios minutos mientras descarga e instala las dependencias. Las ejecuciones posteriores serán mucho más rápidas.
```

src/
│
├── model/
│   ├── Incidente.java            ✅
│   ├── Usuario.java              ✅
│   ├── Comentario.java           ✅
│   └── HistorialEstado.java      ✅
│
├── dao/
│   ├── IncidenteDAO.java         ✅
│   ├── UsuarioDAO.java           ✅
│   ├── ComentarioDAO.java        ✅
│   └── implementacion/
│       ├── UsuarioDAOImpl.java   ✅
│       ├── IncidenteDAOImpl.java ❗
│       └── ComentarioDAOImpl.java ❗
│
├── service/
│   ├── IncidenteService.java     ☀️
│   ├── UsuarioService.java       ✅
│   └── ReporteService.java       ☀️
│
├── controller/
│   ├── AuthController.java       ✅
│   └── IncidenteController.java  ❗
│
├── view/
│   ├── LoginView.java            ❗
│   ├── DashboardView.java        ❗
│   ├── IncidenteView.java        ❗
│   └── ReporteView.java          ❗
│
├── enums/
│   ├── EstadoIncidente.java      ✅
│   ├── Prioridad.java            ✅
│   └── RolUsuario.java           ✅
│
├── utils/
│   ├── ConexionBD.java           ✅
│   └── Validador.java            ✅
│
├── exceptions/
│   ├── UsuarioException.java     ✅
│   ├── IncidenteException.java   ❗
│   ├── AutenticacionException.java ❗
│   ├── ValidacionException.java  ❗
│   ├── BaseDeDatosException.java ❗
│   └── PermisoDenegadoException.java ❗
│
└── Main.java                     ☀️

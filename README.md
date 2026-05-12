models/
├── incident/
│   ├── Incident.java                 # Entidad central (ID, título, descripción, fechas)
│   ├── IncidentId.java               # Value object (evita primitives obsesion)
│   └── IncidentStatus.java           # Enum con estados básicos (opcional si usas State machine aparte)
│
├── core/
│   ├── Priority.java                 # Enum con niveles y sus SLA base
│   ├── Category.java                 # Jerarquía de categorías (padre-hijo)
│   └── Severity.java                 # Impacto + Urgencia = Prioridad (método Matrix)
│
├── state/
│   ├── IncidentState.java            # Interfaz o enum de estados
│   ├── StateTransition.java          # Transiciones permitidas (de -> para)
│   └── StateMachine.java             # Lógica de cambio de estado
│
├── actors/
│   ├── Reporter.java                 # Quien abre el incidente (cliente, usuario final)
│   ├── Technician.java               # Quien lo resuelve (con habilidades, grupo asignado)
│   └── Assignee.java                 # Interfaz común si es necesario
│
├── timeline/
│   ├── TimelineEvent.java            # Cada cambio: estado, prioridad, asignación, comentario
│   ├── EventType.java                # Enum: STATE_CHANGE, ASSIGNMENT, COMMENT, PRIORITY_CHANGE
│   └── IncidentHistory.java          # Lista de eventos (aggregate root)
│
├── sla/
│   ├── SlaPolicy.java                # Tiempo de respuesta/resolución por prioridad
│   ├── SlaViolation.java             # Cuando se incumple el SLA
│   └── EscalationRule.java           # Reglas: si pasa X tiempo sin resolver, escalar a nivel 2
│
└── communication/
    ├── Comment.java                  # Nota (con autor, timestamp, si es interna o pública)
    └── Attachment.java               # Archivo adjunto (nombre, ruta, tamaño, tipo MIME)
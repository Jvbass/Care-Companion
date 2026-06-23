# Diseño Técnico — Care Companion

**Proyecto:** Care Companion — App de recordatorios de medicación y citas
**Versión:** 0.1
**Fecha:** 2026-06-22
**Basado en:** `docs/SRS.md` v0.2
**Convención:** prosa en español; código, identificadores, SQL y nombres de API en inglés.

---

## 1. Visión general

```
┌─────────────────────────────────────────────┐
│  App Android (React Native + Expo)           │
│                                              │
│  UI  →  Domain  →  Repositories              │
│                      │                       │
│              expo-sqlite (local source)      │
│                      │                       │
│              Sync Engine  ──► expo-notifications
│                      │ (push/pull deltas)    │
└──────────────────────┼───────────────────────┘
                       │ HTTPS REST + JWT
                       ▼
┌─────────────────────────────────────────────┐
│  Backend (Spring Boot)                       │
│  Controller → Service → Repository (JPA)     │
│                      │                       │
│                   MySQL                      │
│            (ambos en Docker)                 │
└─────────────────────────────────────────────┘
```

Tres pilares que gobiernan todo el diseño:
1. **Offline-first:** la app escribe en SQLite local; el servidor es la fuente de
   verdad compartida.
2. **Notificaciones en el dispositivo:** los recordatorios los agenda y dispara el
   teléfono, no el servidor.
3. **Sincronización por deltas con última-escritura-gana:** simple, robusta para el
   MVP, con limitaciones conocidas (ver §5).

---

## 2. Arquitectura del backend (Spring Boot)

Capas clásicas, sin sobre-ingeniería para el MVP:

- **Controller** — endpoints REST, validación de entrada, mapeo DTO.
- **Service** — reglas de negocio: permisos por rol, lógica de sync, auditoría.
- **Repository** — Spring Data JPA sobre MySQL.
- **Security** — filtro JWT, resolución del usuario autenticado.

Módulos sugeridos: `auth`, `profile`, `sharing`, `medication`, `appointment`,
`note`, `sync`, `audit`.

Regla transversal de permisos: cada operación sobre datos de un perfil verifica la
membresía y el rol del usuario autenticado (`ProfileMembership`) **en el servicio**,
no en el controller. El cliente puede ocultar botones, pero la autorización real vive
en el backend.

---

## 3. Esquema de base de datos (MySQL)

Todas las tablas de datos de perfil comparten columnas de sincronización:

```sql
id            CHAR(36)   PRIMARY KEY,   -- UUID generado en el cliente
created_at    DATETIME(3) NOT NULL,
updated_at    DATETIME(3) NOT NULL,     -- hora de la última edición (cliente)
server_seq    BIGINT     NOT NULL,      -- secuencia monotónica del servidor (cursor de pull)
deleted       BOOLEAN    NOT NULL DEFAULT FALSE,
deleted_at    DATETIME(3) NULL
```

`server_seq` lo asigna el servidor en cada escritura (ver §5). Es la clave para que
el pull sea incremental y resistente al desfase de reloj entre dispositivos.

Tablas principales:

- **users**: `name`, `email` (UNIQUE), `birth_date`, `password_hash`.
- **treatment_profiles**: `title`, `owner_user_id` (FK users), `condition` NULL.
- **profile_memberships**: `profile_id` (FK), `user_id` (FK),
  `role` ENUM('OWNER','CAREGIVER','VIEWER'). UNIQUE(`profile_id`,`user_id`).
- **medications**: `profile_id`, `name`, `dose_amount`, `dose_unit`,
  `treatment_start_date`, `treatment_end_date` NULL, `notes`.
- **medication_schedules**: `medication_id`,
  `frequency_type` ENUM('FIXED_TIMES','EVERY_X_HOURS','DAYS_OF_WEEK','AS_NEEDED'),
  `times` JSON NULL, `interval_hours` INT NULL, `days_of_week` JSON NULL,
  `reminder_lead_minutes` INT NOT NULL DEFAULT 30.
- **dose_logs**: `schedule_id`, `scheduled_at`, `status`
  ENUM('TAKEN','SKIPPED','POSTPONED'), `logged_by_user_id`, `logged_at`, `note` NULL.
- **appointments**: `profile_id`, `title`, `datetime`, `location`,
  `purpose`, `reminder_lead_minutes` DEFAULT 30, `notes`.
- **notes**: `profile_id`, `type` ENUM('SYMPTOM','QUESTION'), `text`,
  `linked_appointment_id` NULL, `resolved` BOOLEAN DEFAULT FALSE.
- **share_codes**: `profile_id`, `code` (índice), `default_role`,
  `expires_at` (NOT NULL, = created_at + 15 min), `revoked` BOOLEAN DEFAULT FALSE.
- **audit_logs**: `profile_id`, `actor_user_id`, `action`
  ENUM('CREATE','UPDATE','DELETE'), `entity_type`, `entity_id`, `timestamp`,
  `summary` NULL. **Append-only**, no se sincroniza vía last-write-wins (ver §8).

Índice clave para el pull: `INDEX(server_seq)` y, donde aplique,
`INDEX(profile_id, server_seq)`.

---

## 4. Esquema local (SQLite / expo-sqlite)

Espejo casi exacto de MySQL: mismas tablas, mismas columnas de sync. Diferencias:

- Tipos adaptados (`TEXT` para UUID/JSON/fechas ISO-8601, `INTEGER` para booleanos).
- Una tabla de control `sync_state(last_server_seq INTEGER, last_synced_at TEXT)`.
- Una marca local `dirty INTEGER DEFAULT 0` por registro: indica que el registro tiene
  cambios locales aún no confirmados por el servidor. El push envía solo los `dirty`.

Mantener los esquemas gemelos hace que la sincronización sea un mapeo directo, sin
traducción de modelos.

---

## 5. Motor de sincronización (núcleo del diseño)

### 5.1 Modelo
- **IDs:** UUID generados en el cliente. Permiten crear registros offline sin esperar
  un id del servidor y evitan colisiones.
- **Reloj de orden:** `server_seq`, un contador monotónico que el servidor incrementa
  en cada escritura aceptada. El cliente guarda el mayor `server_seq` que recibió
  (`last_server_seq`) y lo usa como cursor.
- **Resolución de conflictos:** última-escritura-gana por `updated_at` a nivel de
  registro. Empates se rompen de forma determinista por `id` (lexicográfico).

### 5.2 Pull (descargar cambios)
```
POST /sync/pull   { last_server_seq }
→ devuelve todos los registros (de perfiles donde el usuario es miembro)
  con server_seq > last_server_seq, incluidos tombstones (deleted=true),
  ordenados por server_seq ascendente, paginados.
→ el cliente los aplica en orden de dependencia (FK) y actualiza last_server_seq
  al mayor recibido.
```

### 5.3 Push (subir cambios)
```
POST /sync/push   { records: [...registros dirty...] }
Para cada registro entrante, el servidor:
  - verifica permiso (rol del usuario en ese perfil),
  - compara con la versión almacenada:
      · si no existe        → inserta,
      · si entrante.updated_at >= almacenado.updated_at → actualiza (LWW),
      · si entrante.updated_at <  almacenado.updated_at → rechaza (gana el servidor),
  - en toda escritura aceptada, asigna un nuevo server_seq.
→ responde con el resultado por registro y el server_seq asignado.
El cliente limpia `dirty` en los aceptados; los rechazados se corrigen en el próximo pull.
```

### 5.4 Orden de aplicación y FKs
Aplicar siempre en orden de dependencia:
`users → treatment_profiles → profile_memberships → medications →
medication_schedules → dose_logs / appointments / notes`.
Esto evita violaciones de clave foránea al insertar hijos antes que padres.

### 5.5 Borrados
Borrado lógico (`deleted=true` + `deleted_at`). El tombstone viaja como cualquier
cambio para que el borrado se propague a todos los miembros. Purga física en el
servidor tras una ventana de retención (p. ej. 90 días).

### 5.6 Limitación conocida (honestidad de ingeniería)
Última-escritura-gana descarta el cambio perdedor a nivel de **registro completo**:
si dos cuidadores editan campos distintos del mismo medicamento offline, gana uno
entero. Para datos de cuidado esto es aceptable en el MVP porque las acciones
frecuentes (registrar una dosis) son **inserciones** en `dose_logs`, no ediciones del
mismo registro — y las inserciones no compiten. La fusión a nivel de campo queda
explícitamente fuera de alcance (post-MVP).

---

## 6. Autenticación

- **Hash:** BCrypt para `password_hash`.
- **Tokens:** JWT de acceso de vida corta (~15 min) + refresh token de vida larga.
- **Almacenamiento en el cliente:** `expo-secure-store` (no AsyncStorage) para los tokens.
- **Renovación:** endpoint `POST /auth/refresh`. Si el refresh expira, se pide login.
- El filtro de seguridad resuelve el usuario autenticado y lo inyecta en cada servicio.

---

## 7. Programación de notificaciones (detalle real)

El punto que más se subestima. Las notificaciones locales son finitas y un
tratamiento puede generar miles de disparos futuros. Estrategia de **ventana rodante**:

- No se agenda todo el futuro. Se agenda una ventana corta (p. ej. próximos 7 días
  o las próximas N notificaciones por perfil).
- La ventana se **rehidrata** al abrir la app y, si es posible, con una tarea
  periódica en segundo plano (`expo-background-task`).
- Tras **cualquier** cambio que afecte horarios — edición local o llegada de datos por
  sync (pull) — se recalcula y se reprograman las notificaciones de la ventana.
- Cada notificación lleva acciones rápidas: "Tomada" / "Posponer" (RF-26), que
  escriben un `dose_log` local al instante.
- En el primer arranque: pedir permiso de notificaciones y solicitar exención de
  optimización de batería (mitigar **Doze**, RNF-1).

Componente: un `NotificationScheduler` que es función pura del estado
(medicamentos + horarios + citas) → conjunto de notificaciones de la ventana. Idempotente:
cancelar las agendadas del perfil y reagendar evita duplicados.

---

## 8. Auditoría

- `audit_logs` es **append-only** e inmutable: nunca se actualiza ni se borra, por lo
  que no participa de la lógica last-write-wins. En el pull solo se **agregan** filas
  nuevas (server_seq > cursor); nunca llegan tombstones de auditoría.
- El servicio escribe una entrada de auditoría en la misma transacción que la
  operación que la origina (crear/editar/borrar datos de perfil), registrando
  `actor_user_id`, `action`, `entity_type`, `entity_id` y un `summary`.
- Solo el Dueño la consulta (`GET /profiles/{id}/audit-log`), paginada.

---

## 9. Códigos de compartición

- Generación: código corto legible (p. ej. 8 caracteres base32, sin ambigüedades
  tipo 0/O). `expires_at = now + 15 min` (por defecto y máximo, RF-12).
- Join: `POST /share-codes/{code}/join` valida no-revocado y no-expirado, crea la
  `ProfileMembership` con `default_role`, y registra auditoría.
- El Dueño puede revocar antes de tiempo (`revoked=true`).

---

## 10. Arquitectura del frontend (React Native + Expo)

- **Capas:** UI (componentes/pantallas) → Domain (casos de uso) → Repositories (acceso
  a SQLite). El Sync Engine y el NotificationScheduler son servicios de infraestructura.
- **Estado:** estado de servidor/datos derivado del store local; una librería de estado
  ligera (p. ej. Zustand) para UI. La fuente de verdad en el cliente es SQLite, no el
  estado en memoria.
- **Navegación:** Expo Router.
- **Accesibilidad (RNF-3):** tipografía grande, alto contraste, "registrar dosis" en un
  toque desde el recordatorio.

---

## 11. Entorno y Docker

- `docker-compose` con dos servicios: `api` (Spring Boot) y `db` (MySQL), red interna,
  volumen persistente para MySQL, variables de entorno para credenciales.
- La app se prueba con Expo en un dispositivo físico (Fase 1), apuntando a la IP local
  del backend.
- Fase 2: mismo `docker-compose` desplegado en un host; build de la app publicado.

---

## 12. Decisiones técnicas y tradeoffs

| Decisión | Alternativa descartada | Por qué |
|----------|------------------------|---------|
| expo-sqlite | WatermelonDB | Menos setup para el MVP; esquema gemelo de MySQL. WatermelonDB queda como upgrade de sync. |
| Notificaciones locales | Push desde servidor | Fiabilidad offline; una dosis no depende de la red. |
| LWW por registro | Merge por campo / CRDTs | Simplicidad; las acciones frecuentes son inserciones, no ediciones concurrentes. |
| `server_seq` monotónico | Cursor por `updated_at` | Inmune al desfase de reloj entre dispositivos para el cursor de pull. |
| UUID en cliente | IDs autoincrementales del servidor | Permite crear offline sin round-trip y evita colisiones. |

---

## 13. Riesgos

- **Doze / fabricantes Android:** algunos OEM matan tareas en segundo plano de forma
  agresiva. Mitigación: ventana rodante + rehidratación al abrir + exención de batería.
- **Desfase de reloj del dispositivo:** afecta `updated_at` (no el cursor). Para datos
  de cuidado el impacto es bajo; documentado como limitación.
- **Crecimiento de tombstones y auditoría:** mitigado con purga por retención y
  paginación.
- **Datos de salud sensibles:** HTTPS obligatorio, secure-store para tokens, cifrado en
  reposo donde sea viable (RNF-2).

---

## 14. Próximo paso sugerido
Pasar a la descomposición en tareas de implementación (fase `tasks` del flujo SDD),
empezando por: esquema de datos + migraciones → auth → CRUD de perfiles/medicación →
motor de sync → notificaciones → compartición/auditoría.

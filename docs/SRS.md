# Especificación de Requisitos de Software (SRS)

**Proyecto:** Care Companion — App de recordatorios de medicación y citas
**Versión:** 0.2 (MVP)
**Fecha:** 2026-06-22
**Estado:** Borrador para revisión

---

## 1. Introducción

### 1.1 Propósito
Definir los requisitos de una aplicación móvil que ayuda a las familias a organizar
el cuidado de un paciente con un tratamiento complejo: tomar varios medicamentos en
horario, asistir a citas médicas y registrar dolencias y dudas para la próxima
visita. El primer usuario objetivo es un paciente oncológico cuidado por varios
familiares.

### 1.2 Alcance
El sistema se compone de:
- Una app móvil Android (React Native + Expo) usada por pacientes y cuidadores.
- Un backend (Spring Boot) que almacena los datos compartidos y habilita la
  compartición familiar.
- Una base de datos en línea (MySQL) como fuente de verdad de los datos
  compartidos/sincronizados.
- Una base de datos local en el dispositivo (SQLite con `expo-sqlite`) para uso
  sin conexión (offline-first).

La app admite múltiples **perfiles de tratamiento** (p. ej. "Papá", "Yo", "Hijo"),
nombrados libremente por el usuario. Los perfiles se pueden **compartir con la
familia** mediante un código, con permisos por rol. La app dispara **recordatorios
locales** antes de cada dosis de medicación y de cada cita.

### 1.3 Estrategia de entrega
1. **Fase 1 — MVP local:** App + backend + base de datos corriendo en local con
   Docker. App probada con Expo en un dispositivo Android físico.
2. **Fase 2 — Despliegue:** Desplegar backend + base de datos (Docker) en un host
   y publicar la app.

### 1.4 Definiciones
| Término | Significado |
|---------|-------------|
| Perfil de tratamiento | Contenedor con nombre que agrupa los medicamentos, citas y notas de una persona. |
| Dueño (Owner) | Usuario que creó el perfil; control total, incluida la compartición y la asignación de roles. |
| Cuidador (Caregiver) | Miembro con permiso de edición (registra dosis, agrega notas, edita datos). |
| Lector (Viewer) | Miembro con acceso de solo lectura. |
| Registro de dosis | Anotación de que una dosis programada fue tomada, omitida o pospuesta. |
| Código de compartición | Código corto que otorga a otro usuario acceso a un perfil. |
| Offline-first | Los datos se escriben primero en local y luego se sincronizan con el servidor. |

### 1.5 Actores
- **Dueño** — crea y gestiona perfiles, los comparte, asigna roles.
- **Cuidador** — edita datos y registra la adherencia en perfiles compartidos.
- **Lector** — consulta un perfil compartido.
- **Sistema (programador de notificaciones)** — dispara recordatorios en el dispositivo.
- **Backend** — autentica usuarios y sincroniza los datos compartidos.

---

## 2. Descripción general

### 2.1 Visión de arquitectura
```
[ App Android: React Native + Expo ]
        |  expo-sqlite (almacén local offline-first)
        |  expo-notifications (recordatorios programados en el dispositivo)
        |
        |  HTTPS / REST + JWT
        v
[ API Spring Boot ]  ----  [ MySQL ]
        (ambos en Docker)
```

### 2.2 Principios de diseño
- **Offline-first.** Toda acción funciona sin conexión; los datos se sincronizan al
  recuperar la red.
- **Notificaciones en el dispositivo.** Los recordatorios se programan y disparan
  localmente en el dispositivo, nunca desde el servidor, para que una dosis no se
  pierda por falta de señal.
- **El servidor es la fuente de verdad compartida.** El backend concilia los cambios
  de varios familiares y resuelve conflictos.

### 2.3 Clases de usuario y permisos
| Capacidad | Dueño | Cuidador | Lector |
|-----------|:-----:|:--------:|:------:|
| Ver datos del perfil | ✓ | ✓ | ✓ |
| Registrar dosis tomada/omitida/pospuesta | ✓ | ✓ | — |
| Agregar/editar medicamentos, citas, notas | ✓ | ✓ | — |
| Generar / revocar códigos de compartición | ✓ | — | — |
| Asignar / cambiar roles de miembros | ✓ | — | — |
| Ver historial de auditoría | ✓ | — | — |
| Eliminar el perfil | ✓ | — | — |

### 2.4 Entorno operativo
- Dispositivo Android con runtime de Expo (Fase 1) / build publicado (Fase 2).
- Backend y base de datos como contenedores Docker.

### 2.5 Restricciones
- Datos de salud: transporte por HTTPS; contraseñas con hash; cifrado en reposo de
  los datos sensibles donde sea viable.
- La optimización de batería de Android (modo Doze) puede suprimir las
  notificaciones programadas; la app debe solicitar la exención de optimización de
  batería y el permiso de notificaciones.

### 2.6 Supuestos confirmados
- Autenticación por email + contraseña para el MVP (login social diferido).
- **Verificación de email diferida a post-MVP.**
- Resolución de conflictos: **última escritura gana** por registro, usando `updated_at`.
- **Los códigos de compartición caducan por defecto a los 15 minutos, siendo 15
  minutos también el máximo permitido.** El dueño puede revocarlos antes.
- Las horas de programación se almacenan en UTC y se muestran en la zona horaria
  local del dispositivo.

---

## 3. Modelo de datos

Cada entidad sincronizada incluye: `id` (UUID generado en el cliente), `created_at`,
`updated_at`, `deleted` (borrado lógico), `deleted_at`.

- **User**: `name`, `email` (único), `birth_date`, `password_hash`.
- **TreatmentProfile**: `title`, `owner_user_id`, `condition` opcional (enfermedad).
- **ProfileMembership**: `profile_id`, `user_id`, `role` (OWNER | CAREGIVER | VIEWER).
- **Medication**: `profile_id`, `name`, `dose_amount`, `dose_unit`,
  `treatment_start_date`, `treatment_end_date` (nullable), `notes`.
- **MedicationSchedule**: `medication_id`, `frequency_type`
  (FIXED_TIMES | EVERY_X_HOURS | DAYS_OF_WEEK | AS_NEEDED), `times` (lista),
  `interval_hours` (nullable), `days_of_week` (nullable),
  `reminder_lead_minutes` (por defecto 30).
- **DoseLog**: `schedule_id`, `scheduled_at`, `status` (TAKEN | SKIPPED | POSTPONED),
  `logged_by_user_id`, `logged_at`, `note` opcional.
- **Appointment**: `profile_id`, `title`, `datetime`, `location`, `purpose`
  (control/visita), `reminder_lead_minutes` (por defecto 30), `notes`.
- **Note** (dolencia / duda): `profile_id`, `type` (SYMPTOM | QUESTION),
  `text`, `created_at`, `linked_appointment_id` opcional, `resolved` (booleano).
- **ShareCode**: `profile_id`, `code`, `default_role`, `expires_at`
  (por defecto +15 min, máximo +15 min), `revoked` (booleano).
- **AuditLog**: `profile_id`, `actor_user_id` (quién), `action`
  (CREATE | UPDATE | DELETE), `entity_type`, `entity_id`, `timestamp`,
  `summary` opcional (resumen del cambio). Registro inmutable (solo se agrega).

---

## 4. Requisitos funcionales

### 4.1 Autenticación y registro
- **RF-1** El usuario se registra con nombre, email, fecha de nacimiento y contraseña.
- **RF-2** El email debe ser único; las contraseñas se almacenan con hash (BCrypt).
- **RF-3** El login devuelve un JWT para autenticar las peticiones a la API.
- **RF-4** La sesión persiste en el dispositivo hasta el cierre de sesión/expiración.

### 4.2 Perfiles de tratamiento
- **RF-5** El usuario puede crear varios perfiles, cada uno con un título libre.
- **RF-6** El creador queda como Dueño del perfil.
- **RF-7** Al agregar un perfil, el usuario puede crearlo desde cero O unirse a uno
  existente ingresando un código de compartición.
- **RF-8** Un perfil agrupa medicamentos, citas y notas.

### 4.3 Compartición y roles
- **RF-9** El Dueño puede generar un código de compartición para un perfil, eligiendo
  el rol por defecto otorgado (Cuidador o Lector).
- **RF-10** Un usuario que ingresa un código válido se agrega como miembro con ese rol.
- **RF-11** El Dueño puede cambiar el rol de un miembro o eliminarlo.
- **RF-12** Los códigos caducan automáticamente a los 15 minutos (valor por defecto y
  máximo). El Dueño puede revocarlos antes; los códigos caducados o revocados se
  rechazan.

### 4.4 Medicamentos y programación
- **RF-13** Un Cuidador/Dueño puede agregar un medicamento con nombre, dosis y unidad.
- **RF-14** Un medicamento tiene una o más programaciones que admiten horarios fijos,
  cada-X-horas, días específicos de la semana o según necesidad (sin recordatorio).
- **RF-15** Cada programación tiene fecha de inicio y de fin opcionales del tratamiento.
- **RF-16** Cada programación tiene un tiempo de antelación configurable para el
  recordatorio (por defecto 30 min).

### 4.5 Registro de adherencia
- **RF-17** Desde una dosis vencida o próxima, un Cuidador/Dueño puede marcarla como
  Tomada, Omitida o Pospuesta, registrando quién la marcó y cuándo.
- **RF-18** La app muestra el historial de adherencia por medicamento y por perfil.

### 4.6 Citas médicas
- **RF-19** Un Cuidador/Dueño puede agregar una cita con título, fecha/hora, lugar y
  motivo.
- **RF-20** Cada cita tiene un tiempo de antelación configurable (por defecto 30 min).

### 4.7 Notas (dolencias y dudas)
- **RF-21** Un Cuidador/Dueño puede registrar una dolencia o una duda, con fecha.
- **RF-22** Una nota se puede vincular a una cita próxima.
- **RF-23** Una nota se puede marcar como resuelta (p. ej. ya preguntada al médico).

### 4.8 Notificaciones
- **RF-24** La app programa una notificación local antes de cada dosis y cita
  próximas, usando el tiempo de antelación configurado.
- **RF-25** Los recordatorios se disparan incluso sin conexión.
- **RF-26** Desde el recordatorio de medicación, el usuario puede marcar la dosis como
  Tomada o Posponerla (snooze).
- **RF-27** En el primer arranque, la app solicita el permiso de notificaciones e
  invita a desactivar la optimización de batería para una entrega fiable.

### 4.9 Sincronización
- **RF-28** Todos los cambios se escriben primero en local y luego se envían al servidor.
- **RF-29** La app descarga los cambios remotos y los fusiona con la regla última
  escritura gana por `updated_at` a nivel de registro.
- **RF-30** Los borrados son lógicos y se propagan a todos los miembros.
- **RF-31** Los miembros de un perfil compartido ven los cambios de los demás tras
  sincronizar.

### 4.10 Historial de auditoría
- **RF-32** Toda creación, edición o borrado de datos de un perfil se registra en un
  historial inmutable con la acción, la entidad afectada, el autor (quién) y la fecha.
- **RF-33** El Dueño puede consultar el historial de auditoría de su perfil.
- **RF-34** El historial de auditoría se sincroniza con el servidor junto al resto de
  los datos del perfil.

---

## 5. Requisitos no funcionales

- **RNF-1 Fiabilidad:** Los recordatorios de medicación/citas deben dispararse a
  tiempo sin conexión; el programador vive en el dispositivo. Mitigar el modo Doze de
  Android con la solicitud de exención de batería.
- **RNF-2 Seguridad y privacidad:** HTTPS en todo el tráfico; contraseñas con hash;
  autenticación JWT; datos de salud tratados como sensibles (cifrado en reposo donde
  sea viable). Solo los miembros de un perfil acceden a sus datos.
- **RNF-3 Usabilidad/Accesibilidad:** El paciente puede ser mayor o estar fatigado —
  objetivos táctiles grandes, tipografía legible, alto contraste y flujos esenciales
  en mínimos pasos (registrar una dosis con un toque desde el recordatorio).
- **RNF-4 Rendimiento:** Las acciones principales (abrir perfil, registrar dosis)
  responden al instante desde el almacén local, sin depender de la latencia de red.
- **RNF-5 Disponibilidad:** Operación totalmente offline; la sincronización se reanuda
  automáticamente al reconectar.
- **RNF-6 Portabilidad:** Backend y base de datos totalmente contenedorizados (Docker)
  para entornos idénticos en local y en despliegue.

---

## 6. Interfaces externas (esbozo de API REST)

- `POST /auth/register`, `POST /auth/login`
- `GET/POST /profiles`, `PATCH/DELETE /profiles/{id}`
- `POST /profiles/{id}/share-codes`, `POST /profiles/{id}/share-codes/{code}/revoke`
- `POST /share-codes/{code}/join`
- `GET/PATCH /profiles/{id}/members`
- CRUD bajo `/profiles/{id}/medications`, `/schedules`, `/appointments`, `/notes`
- `POST /profiles/{id}/dose-logs`
- `GET /profiles/{id}/audit-log`
- `POST /sync` (envío de deltas locales + descarga de cambios remotos desde un cursor)

---

## 7. Alcance del MVP y fuera de alcance

**En alcance (MVP):** registro/login, perfiles, compartición por código con roles,
medicamentos + programaciones, registro de dosis, citas, notas, recordatorios
locales, sincronización offline-first con última escritura gana, historial de
auditoría.

**Fuera de alcance (más adelante):** verificación de email, login social, build de
iOS, notificaciones push entre dispositivos, comprobación de interacciones de
medicamentos, exportación de informe/PDF para el médico, interfaz multilingüe,
resolución de conflictos a nivel de campo.

---

## 8. Decisiones confirmadas en esta versión
1. Verificación de email: **diferida a post-MVP**.
2. Códigos de compartición: **caducan por defecto a los 15 minutos (máximo 15 min)**.
3. Auditoría: **se registra el historial con la acción y el autor de cada cambio**.
4. Idioma del documento: **español**.

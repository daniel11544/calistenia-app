# FitCalisthenics 🤸‍♂️🔥

**FitCalisthenics** es una aplicación Android de alto rendimiento inspirada en BetterMe, diseñada para actuar como tu entrenador personal digital de calistenia. Funciona de manera 100% offline (local-first) utilizando Room Database y DataStore, e integra opcionalmente la API de Google Gemini para brindar recomendaciones personalizadas de entrenamiento y recuperación muscular.

---

## 🚀 Características Principales

1. **Onboarding Inteligente (4 Pantallas):**
   - Bienvenida estética con diseño deportivo de alta energía (verde eléctrico y acentos naranjas).
   - Recopilación de biometría y nivel (Principiante, Intermedio, Avanzado).
   - Selección de objetivos (Ganar masa muscular, Perder grasa, Resistencia, Habilidades como Handstand y Muscle-up).
   - Configuración de disponibilidad semanal (2 a 6 días) y duración de sesión (15 a 60 min).

2. **Generador Determinista de Planes con Sobrecarga Progresiva:**
   - Creación automática de programas estructurados de 4 a 12 semanas.
   - Ajuste de series, repeticiones y descansos según el objetivo.
   - Distribución inteligente de días de entrenamiento y días de descanso activo.
   - Sobrecarga progresiva semana a semana (+2 reps o +1 serie) y semanas de descarga articular programadas.

3. **Catálogo de más de 40 Ejercicios de Calistenia:**
   - Precargado en assets (`exercises.json`) con instrucciones paso a paso, errores comunes a evitar y progresiones (variantes fáciles y avanzadas).
   - Filtros por grupos musculares (Pecho, Espalda, Piernas, Core, Hombros, Brazos) y niveles de dificultad.

4. **Pantalla "Hoy" (Daily Dashboard):**
   - Vista de la sesión activa con barra de progreso interactiva.
   - Contador de racha de días consecutivos de entrenamiento.
   - Resumen de calorías quemadas y tiempo estimado bajo tensión.
   - Botón directo para iniciar el entrenamiento guiado interactivo.

5. **Temporizador de Entrenamiento y Servicio en Primer Plano:**
   - Control de series y descansos con aviso de vibración y sonido al finalizar la cuenta regresiva.
   - `TimerForegroundService` que mantiene el cronómetro activo en segundo plano con notificación persistente interactiva (Pausar, Reanudar, Saltar descanso).

6. **Notificaciones y Recordatorios Programados:**
   - `WorkManager` para recordatorios diarios de entrenamiento configurables.
   - Alerta periódica de hidratación cada 2 horas.
   - `BootReceiver` que restaura las alarmas tras el reinicio del dispositivo.

7. **Estadísticas, Historial y Logros:**
   - Gráfica semanal interactiva en Compose.
   - Métricas acumuladas: sesiones completadas, minutos entrenados y calorías.
   - Sistema de insignias y logros desbloqueables.
   - Historial detallado de cada sesión realizada.

8. **Asesoría con IA Gemini (Opcional & Resiliente):**
   - Botón "Pedir recomendación a Gemini" en la pantalla de progreso.
   - Envía el resumen de volumen y racha para recibir consejos técnicos y de nutrición.
   - Si no hay conexión o API Key, conmuta de forma automática y transparente a un motor heurístico local de calistenia.

---

## 🛠️ Arquitectura y Tecnologías

- **Lenguaje:** Kotlin 100%
- **UI:** Jetpack Compose con Material Design 3
- **Arquitectura:** MVVM (Model-View-ViewModel) + Clean Architecture con Casos de Uso
- **Base de Datos Local:** Room Database con Kotlin Symbol Processing (KSP) y `Flow` reactivos
- **Preferencias:** Jetpack DataStore Preferences
- **Tareas en Segundo Plano:** WorkManager & Android Foreground Services
- **Notificaciones:** NotificationManager con Notification Channels específicos (API 26+) y permiso `POST_NOTIFICATIONS` (API 33+)
- **Testing:** JUnit 4 + Robolectric / Unit Tests para algoritmos de sobrecarga progresiva

---

## 🏃 Cómo Ejecutar los Tests

Para ejecutar las pruebas unitarias del generador de planes y casos de uso:
```bash
gradle :app:testDebugUnitTest
```

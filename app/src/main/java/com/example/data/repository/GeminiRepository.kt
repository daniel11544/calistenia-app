package com.example.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    suspend fun getCoachingAdvice(
        goal: String,
        level: String,
        totalSessions: Int,
        totalMinutes: Int,
        streakDays: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Eres un entrenador de calistenia experto y motivador de élite.
                Perfil del atleta:
                - Objetivo: $goal
                - Nivel: $level
                - Sesiones completadas: $totalSessions
                - Minutos totales de entrenamiento: $totalMinutes
                - Racha actual: $streakDays días
                
                Entrega un análisis breve, potente y altamente motivador en español (máximo 3 párrafos o puntos clave) con:
                1. Evaluación del ritmo y felicitación por su racha de $streakDays días.
                2. Un consejo técnico clave de calistenia enfocado en su objetivo de $goal (ej. retracción escapular, tensión corporal o respiración).
                3. Una recomendación sobre nutrición o recuperación muscular para su siguiente sesión.
            """.trimIndent()

            val model = Firebase.ai.generativeModel("gemini-3.8-flash")
            val response = model.generateContent(prompt)
            val adviceText = response.text

            if (!adviceText.isNullOrBlank()) {
                Result.success(adviceText.trim())
            } else {
                Result.success(getFallbackCoachingAdvice(goal, level, totalSessions, streakDays))
            }
        } catch (e: Exception) {
            // Offline or unconfigured backend - graceful offline calisthenics coaching fallback
            Result.success(getFallbackCoachingAdvice(goal, level, totalSessions, streakDays))
        }
    }

    private fun getFallbackCoachingAdvice(
        goal: String,
        level: String,
        totalSessions: Int,
        streakDays: Int
    ): String {
        val streakMessage = when {
            streakDays >= 7 -> "🔥 ¡Racha legendaria de $streakDays días! Tu consistencia es la clave del progreso neuromuscular."
            streakDays >= 3 -> "⚡ Llevas $streakDays días consecutivos entrenando con disciplina. La inercia positiva está de tu lado."
            totalSessions > 0 -> "💪 Has acumulado $totalSessions sesiones de calistenia. Cada repetición suma a tu transformación física."
            else -> "🚀 Estás al inicio de un gran viaje de fuerza corporal. La técnica y paciencia construyen tendones fuertes."
        }

        val technicalTip = when {
            goal.contains("Músculo", ignoreCase = true) || goal.contains("Masa", ignoreCase = true) ->
                "📌 Consejo Técnico: Concéntrate en la fase excéntrica (bajada lenta de 3 segundos) en cada flexión y dominada. El tiempo bajo tensión es el mayor estímulo de hipertrofia con tu propio peso."
            goal.contains("Grasa", ignoreCase = true) ->
                "📌 Consejo Técnico: Reduce los descansos entre ejercicios a 45 segundos para mantener las pulsaciones elevadas y generar un efecto EPOC (quema calórica post-ejercicio prolongada)."
            goal.contains("Habilidades", ignoreCase = true) || goal.contains("Fuerza", ignoreCase = true) ->
                "📌 Consejo Técnico: Practica la postura Hollow Body y la depresión/retracción escapular activa antes de cada serie. El control de la línea pélvica es el secreto para desbloquear el Handstand y Muscle-up."
            else ->
                "📌 Consejo Técnico: Mantén siempre el core y los glúteos compactos en cada movimiento. Un cuerpo en tensión perfecta transmite la fuerza con un 30% más de eficiencia."
        }

        val recoveryTip = "🌱 Recuperación y Nutrición: Asegura al menos 7-8 horas de sueño profundo para permitir la regeneración del tejido conectivo y consume suficiente proteína e hidratación hoy."

        return "$streakMessage\n\n$technicalTip\n\n$recoveryTip"
    }
}

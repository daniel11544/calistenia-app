package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ExerciseDao
import com.example.data.local.dao.PlanDao
import com.example.data.local.dao.ProgressDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.WorkoutDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.PlanEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WorkoutDayEntity
import com.example.data.local.entity.WorkoutDayExerciseEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.WorkoutWeekEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.InputStreamReader

@Database(
    entities = [
        UserProfileEntity::class,
        PlanEntity::class,
        WorkoutWeekEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutDayExerciseEntity::class,
        WorkoutSessionEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun userDao(): UserDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fit_calisthenics_db"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    populateInitialData(context, database)
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    populateInitialData(context, database)
                }
            }
        }

        suspend fun populateInitialData(context: Context, database: AppDatabase) {
            val exerciseDao = database.exerciseDao()
            val progressDao = database.progressDao()

            val existingCount = exerciseDao.getCount()
            Log.d("AppDatabase", "Current exercise count in DB: $existingCount")

            if (existingCount < 40) {
                try {
                    val inputStream = context.assets.open("exercises.json")
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    val entities = mutableListOf<ExerciseEntity>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        val instructionsList = mutableListOf<String>()
                        val instructionsArray = obj.optJSONArray("instructions")
                        if (instructionsArray != null) {
                            for (j in 0 until instructionsArray.length()) {
                                instructionsList.add(instructionsArray.getString(j))
                            }
                        }

                        val mistakesList = mutableListOf<String>()
                        val mistakesArray = obj.optJSONArray("commonMistakes")
                        if (mistakesArray != null) {
                            for (j in 0 until mistakesArray.length()) {
                                mistakesList.add(mistakesArray.getString(j))
                            }
                        }

                        entities.add(
                            ExerciseEntity(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                muscleGroup = obj.getString("muscleGroup"),
                                difficulty = obj.getString("difficulty"),
                                equipment = obj.getString("equipment"),
                                description = obj.optString("description", ""),
                                instructions = instructionsList,
                                commonMistakes = mistakesList,
                                defaultSets = obj.optInt("defaultSets", 3),
                                defaultReps = obj.optString("defaultReps", "10-12 reps"),
                                defaultRestSeconds = obj.optInt("defaultRestSeconds", 60),
                                easierVariation = obj.optString("easierVariation", ""),
                                harderVariation = obj.optString("harderVariation", "")
                            )
                        )
                    }

                    if (entities.isNotEmpty()) {
                        exerciseDao.insertExercises(entities)
                        Log.d("AppDatabase", "Successfully seeded ${entities.size} exercises into DB")
                    }
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error seeding exercises JSON", e)
                }
            }

            // Seed achievements
            val initialAchievements = listOf(
                AchievementEntity(
                    id = "first_session",
                    title = "Primer Paso",
                    description = "Completa tu primer entrenamiento de calistenia",
                    iconType = "flag",
                    maxProgress = 1
                ),
                AchievementEntity(
                    id = "streak_3",
                    title = "Hábito de Acero",
                    description = "Mantén una racha de 3 días consecutivos",
                    iconType = "fire",
                    maxProgress = 3
                ),
                AchievementEntity(
                    id = "streak_7",
                    title = "Semana Imparable",
                    description = "Alcanza 7 días continuos de racha",
                    iconType = "fire",
                    maxProgress = 7
                ),
                AchievementEntity(
                    id = "workouts_5",
                    title = "Constancia Pura",
                    description = "Completa 5 sesiones de entrenamiento",
                    iconType = "star",
                    maxProgress = 5
                ),
                AchievementEntity(
                    id = "workouts_15",
                    title = "Atleta Calisténico",
                    description = "Completa 15 sesiones totales",
                    iconType = "trophy",
                    maxProgress = 15
                ),
                AchievementEntity(
                    id = "century_time",
                    title = "Club de los 100",
                    description = "Acumula 100 minutos de entrenamiento activo",
                    iconType = "timer",
                    maxProgress = 100
                )
            )
            progressDao.insertAchievements(initialAchievements)
        }
    }
}

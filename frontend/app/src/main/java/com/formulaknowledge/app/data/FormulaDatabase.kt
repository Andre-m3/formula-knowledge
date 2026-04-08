package com.formulaknowledge.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. ENTITIES (Tabelle del DB) ---

@Entity(tableName = "driver_standings")
data class DriverStandingEntity(
    @PrimaryKey val position: Int,
    val driver_name: String,
    val constructor_name: String,
    val points: Int,
    val wins: Int
)

@Entity(tableName = "constructor_standings")
data class ConstructorStandingEntity(
    @PrimaryKey val position: Int,
    val constructor_name: String,
    val chassis_name: String?,
    val points: Int,
    val wins: Int
)

@Entity(tableName = "circuit_details")
data class CircuitDetailEntity(
    @PrimaryKey val round: Int,
    val gp_name: String,
    val circuit_name: String,
    val location: String,
    val country: String,
    val length: String,
    val corners: Int,
    val laps: Int,
    val record: String,
    val is_sprint: Boolean,
    val dates_joined: String,
    val status: String,
    val previous_winner: String,
    val most_driver_wins: String,
    val most_constructor_wins: String,
    val most_driver_podiums: String,
    val most_poles: String,
    val num_races_held: Int,
    val fp1_time: String?,
    val fp2_time: String?,
    val fp3_time: String?,
    val sprint_shootout_time: String?,
    val sprint_race_time: String?,
    val quali_time: String?,
    val race_time: String?
)

@Entity(tableName = "race_results")
data class RaceResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val round_number: Int,
    val position: Int,
    val driver: String,
    val team: String,
    val points: Int,
    val time: String
)

@Entity(tableName = "calendar_entries")
data class CalendarEntity(
    @PrimaryKey val round: Int,
    val name: String,
    val country: String,
    val city: String,
    val circuit_name: String?,
    val date: String,
    val status: String,
    val is_clickable: Boolean,
    val cancelled: Boolean
)

@Entity(tableName = "current_raceweek")
data class RaceWeekEntity(
    @PrimaryKey val id: Int = 1,
    val gp_name: String,
    val country: String,
    val city: String,
    val circuit_name: String?,
    val round_number: Int,
    val is_sprint: Boolean,
    val dates_joined: String,
    val weather_json: String?, // Salviamo il meteo convertito in testo
    val weather_last_updated: Long,
    val fp1_time: String?,
    val fp2_time: String?,
    val fp3_time: String?,
    val sprint_shootout_time: String?,
    val sprint_race_time: String?,
    val quali_time: String?,
    val race_time: String?
)

// --- 2. DAOs (Data Access Objects) ---

@Dao
interface StandingsDao {
    // Ritorna un Flow: ogni volta che i dati nel DB cambiano, la UI si aggiorna da sola!
    @Query("SELECT * FROM driver_standings ORDER BY position ASC")
    fun getDriverStandings(): Flow<List<DriverStandingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriverStandings(standings: List<DriverStandingEntity>)

    @Query("DELETE FROM driver_standings")
    suspend fun clearDriverStandings()

    @Query("SELECT * FROM constructor_standings ORDER BY position ASC")
    fun getConstructorStandings(): Flow<List<ConstructorStandingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConstructorStandings(standings: List<ConstructorStandingEntity>)

    @Query("DELETE FROM constructor_standings")
    suspend fun clearConstructorStandings()

    @Transaction
    suspend fun updateDriverStandings(standings: List<DriverStandingEntity>) {
        clearDriverStandings()
        insertDriverStandings(standings)
    }

    @Transaction
    suspend fun updateConstructorStandings(standings: List<ConstructorStandingEntity>) {
        clearConstructorStandings()
        insertConstructorStandings(standings)
    }
}

@Dao
interface RaceDao {
    @Query("SELECT * FROM circuit_details WHERE round = :round")
    fun getCircuitDetail(round: Int): Flow<CircuitDetailEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCircuitDetail(circuit: CircuitDetailEntity)

    @Query("SELECT * FROM race_results WHERE round_number = :round ORDER BY position ASC")
    fun getRaceResults(round: Int): Flow<List<RaceResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceResults(results: List<RaceResultEntity>)

    @Query("DELETE FROM race_results WHERE round_number = :round")
    suspend fun clearRaceResults(round: Int)

    @Transaction
    suspend fun updateRaceResults(round: Int, results: List<RaceResultEntity>) {
        clearRaceResults(round)
        insertRaceResults(results)
    }
}

@Dao
interface GeneralDao {
    @Query("SELECT * FROM calendar_entries ORDER BY round ASC")
    fun getCalendar(): Flow<List<CalendarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendar(entries: List<CalendarEntity>)

    @Query("DELETE FROM calendar_entries")
    suspend fun clearCalendar()

    @Transaction
    suspend fun updateCalendar(entries: List<CalendarEntity>) {
        clearCalendar()
        insertCalendar(entries)
    }

    @Query("SELECT * FROM current_raceweek WHERE id = 1")
    fun getCurrentRaceWeek(): Flow<RaceWeekEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceWeek(raceWeek: RaceWeekEntity)
}

// --- 3. DATABASE (Il motore Room) ---

@Database(
    entities = [DriverStandingEntity::class, ConstructorStandingEntity::class, CircuitDetailEntity::class, RaceResultEntity::class, CalendarEntity::class, RaceWeekEntity::class],
    version = 10,
    exportSchema = false
)
abstract class FormulaDatabase : RoomDatabase() {
    abstract fun standingsDao(): StandingsDao
    abstract fun raceDao(): RaceDao
    abstract fun generalDao(): GeneralDao

    companion object {
        @Volatile
        private var INSTANCE: FormulaDatabase? = null

        fun getDatabase(context: Context): FormulaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FormulaDatabase::class.java,
                    "formula_knowledge_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

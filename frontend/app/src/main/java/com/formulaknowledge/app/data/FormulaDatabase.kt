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

// --- 3. DATABASE (Il motore Room) ---

@Database(
    entities = [DriverStandingEntity::class, ConstructorStandingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FormulaDatabase : RoomDatabase() {
    abstract fun standingsDao(): StandingsDao

    companion object {
        @Volatile
        private var INSTANCE: FormulaDatabase? = null

        fun getDatabase(context: Context): FormulaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FormulaDatabase::class.java,
                    "formula_knowledge_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

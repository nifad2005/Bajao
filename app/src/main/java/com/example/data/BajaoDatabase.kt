package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bajao_track_states")
data class BajaoTrackState(
    @PrimaryKey val trackId: Int,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTime: Long = 0L,
    val customBpm: Int = 0 // 0 means default track BPM
)

@Dao
interface BajaoTrackDao {
    @Query("SELECT * FROM bajao_track_states")
    fun getAllTrackStates(): Flow<List<BajaoTrackState>>

    @Query("SELECT * FROM bajao_track_states WHERE trackId = :trackId")
    suspend fun getTrackStateById(trackId: Int): BajaoTrackState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: BajaoTrackState)

    @Query("UPDATE bajao_track_states SET isFavorite = :isFav WHERE trackId = :trackId")
    suspend fun updateFavorite(trackId: Int, isFav: Boolean)

    @Query("UPDATE bajao_track_states SET playCount = playCount + 1, lastPlayedTime = :timestamp WHERE trackId = :trackId")
    suspend fun incrementPlayCount(trackId: Int, timestamp: Long)

    @Query("UPDATE bajao_track_states SET customBpm = :bpm WHERE trackId = :trackId")
    suspend fun updateCustomBpm(trackId: Int, bpm: Int)
}

@Database(entities = [BajaoTrackState::class], version = 1, exportSchema = false)
abstract class BajaoDatabase : RoomDatabase() {
    abstract fun trackDao(): BajaoTrackDao

    companion object {
        @Volatile
        private var INSTANCE: BajaoDatabase? = null

        fun getDatabase(context: Context): BajaoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BajaoDatabase::class.java,
                    "bajao_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class BajaoRepository(private val trackDao: BajaoTrackDao) {
    val allTrackStates: Flow<List<BajaoTrackState>> = trackDao.getAllTrackStates()

    suspend fun getTrackState(trackId: Int): BajaoTrackState {
        return trackDao.getTrackStateById(trackId) ?: BajaoTrackState(trackId = trackId)
    }

    suspend fun saveTrackState(state: BajaoTrackState) {
        trackDao.insertOrUpdate(state)
    }

    suspend fun toggleFavorite(trackId: Int) {
        val current = getTrackState(trackId)
        trackDao.updateFavorite(trackId, !current.isFavorite)
    }

    suspend fun registerPlay(trackId: Int) {
        // Ensure state exists first
        val current = getTrackState(trackId)
        if (current.playCount == 0 && current.lastPlayedTime == 0L) {
            trackDao.insertOrUpdate(current)
        }
        trackDao.incrementPlayCount(trackId, System.currentTimeMillis())
    }

    suspend fun updateBpm(trackId: Int, bpm: Int) {
        val current = getTrackState(trackId)
        if (current.playCount == 0 && current.lastPlayedTime == 0L) {
            trackDao.insertOrUpdate(current)
        }
        trackDao.updateCustomBpm(trackId, bpm)
    }
}

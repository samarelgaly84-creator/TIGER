package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val category: String,
    val content: String,
    val mediaType: String,
    val likesCount: Int,
    val cachedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_messages")
data class CachedMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderName: String,
    val cipherText: String,
    val plainPreview: String,
    val cachedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface TigerCacheDao {
    @Query("SELECT * FROM cached_posts ORDER BY cachedAtMs DESC LIMIT 50")
    fun observeCachedPosts(): Flow<List<CachedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosts(posts: List<CachedPostEntity>)

    @Query("SELECT * FROM cached_messages WHERE chatId = :chatId ORDER BY cachedAtMs ASC")
    fun observeCachedMessages(chatId: String): Flow<List<CachedMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<CachedMessageEntity>)

    @Query("DELETE FROM cached_posts")
    suspend fun clearPosts()

    @Query("DELETE FROM cached_messages")
    suspend fun clearMessages()
}

@Database(
    entities = [CachedPostEntity::class, CachedMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TigerDatabase : RoomDatabase() {
    abstract fun cacheDao(): TigerCacheDao

    companion object {
        @Volatile
        private var instance: TigerDatabase? = null

        fun getInstance(context: Context): TigerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TigerDatabase::class.java,
                    "tiger_connect_cache.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}

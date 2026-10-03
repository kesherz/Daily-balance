package com.example.dailycandle.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.example.dailycandle.domain.AmountInput
import com.example.dailycandle.domain.DailyEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "entries")
data class StoredEntry(
    @PrimaryKey val epochDay: Long,
    val value: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toEntry() = DailyEntry(LocalDate.ofEpochDay(epochDay), value.toBigDecimal(), createdAt, updatedAt)

    companion object {
        fun from(entry: DailyEntry) = StoredEntry(
            entry.date.toEpochDay(), AmountInput.canonical(entry.value), entry.createdAt, entry.updatedAt,
        )
    }
}

@Entity(tableName = "migration_receipts")
data class MigrationReceipt(
    @PrimaryKey val id: String = LEGACY_MIGRATION_ID,
    val imported: Int,
    val rejected: Int,
    val duplicateDates: Int,
    val existingDates: Int,
)

const val LEGACY_MIGRATION_ID = "shared_preferences_v1"

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY epochDay ASC")
    fun observe(): Flow<List<StoredEntry>>

    @Query("SELECT * FROM entries ORDER BY epochDay ASC")
    suspend fun all(): List<StoredEntry>

    @Query("SELECT * FROM entries WHERE epochDay = :day")
    suspend fun find(day: Long): StoredEntry?

    @Upsert
    suspend fun upsert(entry: StoredEntry)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entries: List<StoredEntry>): List<Long>

    @Query("DELETE FROM entries WHERE epochDay = :day AND updatedAt = :expectedUpdate")
    suspend fun delete(day: Long, expectedUpdate: Long): Int

    @Query("SELECT * FROM migration_receipts WHERE id = :id")
    suspend fun migration(id: String = LEGACY_MIGRATION_ID): MigrationReceipt?

    @Insert
    suspend fun finishMigration(receipt: MigrationReceipt)
}

@Database(entities = [StoredEntry::class, MigrationReceipt::class], version = 1, exportSchema = true)
abstract class BalanceDatabase : RoomDatabase() {
    abstract fun entries(): EntryDao
}

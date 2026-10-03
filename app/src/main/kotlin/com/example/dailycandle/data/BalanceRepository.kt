package com.example.dailycandle.data

import androidx.room.withTransaction
import com.example.dailycandle.domain.AmountInput
import com.example.dailycandle.domain.CsvImport
import com.example.dailycandle.domain.DailyEntry
import com.example.dailycandle.domain.DuplicatePolicy
import com.example.dailycandle.domain.ImportResult
import com.example.dailycandle.domain.LegacyParser
import com.example.dailycandle.domain.LegacySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

fun interface LegacySource { fun read(): LegacySnapshot }
class EntryConflictException : IllegalStateException("The entry changed while it was being edited")

class BalanceRepository(
    private val database: BalanceDatabase,
    val legacySource: LegacySource,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dao = database.entries()
    val entries = dao.observe().map { stored -> stored.map(StoredEntry::toEntry) }

    /** Receipt and inserted rows share one transaction. Failure rolls back BOTH. */
    suspend fun initialize(): MigrationReceipt = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.migration() ?: run {
                val parsed = LegacyParser.parse(legacySource.read())
                val now = clock.millis()
                val results = dao.insertIfAbsent(parsed.entries.map {
                    StoredEntry.from(it.copy(createdAt = now, updatedAt = now))
                })
                MigrationReceipt(
                    imported = results.count { it != -1L },
                    rejected = parsed.rejected,
                    duplicateDates = parsed.duplicateDates,
                    existingDates = results.count { it == -1L },
                ).also { dao.finishMigration(it) }
            }
        }
    }

    suspend fun all(): List<DailyEntry> = dao.all().map(StoredEntry::toEntry)

    suspend fun save(date: LocalDate, value: BigDecimal, expectedUpdate: Long?) {
        require(AmountInput.parseCanonical(AmountInput.canonical(value)) != null)
        database.withTransaction {
            val existing = dao.find(date.toEpochDay())
            if (existing?.updatedAt != expectedUpdate) throw EntryConflictException()
            val now = maxOf(clock.millis(), (existing?.updatedAt ?: -1) + 1)
            dao.upsert(StoredEntry(date.toEpochDay(), AmountInput.canonical(value), existing?.createdAt ?: now, now))
        }
    }

    suspend fun delete(entry: DailyEntry) {
        if (dao.delete(entry.date.toEpochDay(), entry.updatedAt) != 1) throw EntryConflictException()
    }

    /** Never overwrite a new value recorded after deletion. */
    suspend fun restore(entry: DailyEntry): Boolean =
        dao.insertIfAbsent(listOf(StoredEntry.from(entry))).single() != -1L

    suspend fun import(preview: CsvImport, policy: DuplicatePolicy): ImportResult {
        require(preview.failure == null)
        require(preview.entries.all { AmountInput.parseCanonical(AmountInput.canonical(it.value)) != null })
        return database.withTransaction {
            var written = 0
            var skipped = 0
            preview.entries.forEach { entry ->
                val existing = dao.find(entry.date.toEpochDay())
                if (existing != null && policy == DuplicatePolicy.KEEP_EXISTING) skipped++
                else {
                    val now = maxOf(clock.millis(), (existing?.updatedAt ?: -1) + 1)
                    dao.upsert(StoredEntry.from(entry.copy(createdAt = existing?.createdAt ?: now, updatedAt = now)))
                    written++
                }
            }
            ImportResult(written, skipped)
        }
    }
}

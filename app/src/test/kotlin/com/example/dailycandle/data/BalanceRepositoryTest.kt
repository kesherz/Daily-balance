package com.example.dailycandle.data

import androidx.room.Room
import com.example.dailycandle.domain.CsvCodec
import com.example.dailycandle.domain.DuplicatePolicy
import com.example.dailycandle.domain.LegacySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.StringReader
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BalanceRepositoryTest {
    private lateinit var database: BalanceDatabase
    private lateinit var repository: BalanceRepository
    private val day = LocalDate.of(2026, 1, 1)
    private var legacy = LegacySnapshot("1,2,bad,4", "2026/01/01,2026/01/01,2026/01/02")

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, BalanceDatabase::class.java).build()
        repository = BalanceRepository(database, LegacySource { legacy }, Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC))
    }

    @After fun close() { database.close() }

    @Test fun `migration is durable idempotent and preserves raw preferences`() = runBlocking {
        val first = repository.initialize()
        assertEquals(1, first.imported)
        assertEquals(2, first.rejected)
        assertEquals(1, first.duplicateDates)
        assertEquals("2", repository.all().single().value.toPlainString())
        repository.delete(repository.all().single())
        assertEquals(first, repository.initialize())
        assertTrue(repository.all().isEmpty())
        assertEquals("1,2,bad,4", legacy.values)
    }

    @Test fun `failed receipt transaction rolls back entries and retries safely`() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_receipt BEFORE INSERT ON migration_receipts BEGIN SELECT RAISE(ABORT, 'test failure'); END",
        )
        try { repository.initialize(); fail("Expected migration failure") } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(repository.all().isEmpty())
        assertNull(database.entries().migration())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_receipt")
        assertEquals(1, repository.initialize().imported)
        assertEquals(1, repository.all().size)
    }

    @Test fun `migration cannot overwrite an existing new record`() = runBlocking {
        repository.save(day, "99".toBigDecimal(), null)
        val receipt = repository.initialize()
        assertEquals(0, receipt.imported)
        assertEquals(1, receipt.existingDates)
        assertEquals("99", repository.all().single().value.toPlainString())
    }

    @Test fun `one date has explicit update and stale edits fail`() = runBlocking {
        repository.save(day.plusDays(2), "0.1".toBigDecimal(), null)
        repository.save(day, "-1.23".toBigDecimal(), null)
        assertEquals(listOf(day, day.plusDays(2)), repository.all().map { it.date })
        val old = repository.all().first()
        try { repository.save(day, "2".toBigDecimal(), null); fail("Expected conflict") } catch (_: EntryConflictException) { }
        repository.save(day, "2.000001".toBigDecimal(), old.updatedAt)
        val updated = repository.all().first()
        assertEquals(old.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt > old.updatedAt)
        try { repository.save(day, "3".toBigDecimal(), old.updatedAt); fail("Expected stale conflict") } catch (_: EntryConflictException) { }
        assertEquals(2, repository.all().size)
        assertEquals("2.000001", updated.value.toPlainString())
    }

    @Test fun `delete undo preserves a newer value for the same day`() = runBlocking {
        repository.save(day, "1".toBigDecimal(), null)
        val entry = repository.all().single()
        repository.delete(entry)
        assertTrue(repository.restore(entry))
        repository.delete(entry)
        repository.save(day, "7".toBigDecimal(), null)
        assertFalse(repository.restore(entry))
        assertEquals("7", repository.all().single().value.toPlainString())
    }

    @Test fun `import preserves existing by default and replaces only explicitly`() = runBlocking {
        repository.save(day, "10".toBigDecimal(), null)
        val preview = CsvCodec.read(StringReader("date,value\n2026-01-01,20\n2026-01-02,-0.005\n2026-01-03,bad\n"))
        val kept = repository.import(preview, DuplicatePolicy.KEEP_EXISTING)
        assertEquals(1, kept.written)
        assertEquals(1, kept.skipped)
        assertEquals("10", repository.all().first().value.toPlainString())
        val before = repository.all().first()
        val replaced = repository.import(preview, DuplicatePolicy.REPLACE_EXISTING)
        assertEquals(2, replaced.written)
        assertEquals("20", repository.all().first().value.toPlainString())
        assertEquals(before.createdAt, repository.all().first().createdAt)
        assertEquals(2, repository.all().size)
    }

    @Test fun `failed import rolls back all writes including replacements`() = runBlocking {
        repository.save(day, "10".toBigDecimal(), null)
        val before = repository.all()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_entry BEFORE INSERT ON entries WHEN NEW.epochDay = ${day.plusDays(1).toEpochDay()} BEGIN SELECT RAISE(ABORT, 'test failure'); END",
        )
        val preview = CsvCodec.read(StringReader("date,value\n2026-01-01,20\n2026-01-02,30\n"))
        try { repository.import(preview, DuplicatePolicy.REPLACE_EXISTING); fail("Expected import failure") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(before, repository.all())
    }
}

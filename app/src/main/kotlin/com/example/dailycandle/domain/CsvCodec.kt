package com.example.dailycandle.domain

import java.io.Reader
import java.io.Writer
import java.time.LocalDate

enum class CsvProblem { COLUMNS, DATE, VALUE, DUPLICATE_DATE }
data class CsvIssue(val line: Int, val problem: CsvProblem)
enum class CsvFailure { HEADER, TOO_LARGE }
data class CsvImport(
    val entries: List<DailyEntry>,
    val issues: List<CsvIssue>,
    val invalidRows: Int,
    val failure: CsvFailure? = null,
)
enum class DuplicatePolicy { KEEP_EXISTING, REPLACE_EXISTING }
data class ImportResult(val written: Int, val skipped: Int)

object CsvCodec {
    const val MAX_CHARACTERS = 5_000_000
    const val MAX_ROWS = 50_000
    const val MAX_LINE_LENGTH = 1_024
    const val MAX_REPORTED_ISSUES = 100

    /** Bounded streaming input. No changes are written until the user accepts this preview. */
    fun read(reader: Reader): CsvImport {
        val input = reader.buffered()
        val entries = linkedMapOf<LocalDate, DailyEntry>()
        val issues = mutableListOf<CsvIssue>()
        var invalid = 0
        var characters = 0
        var lineNumber = 0
        fun failure(reason: CsvFailure) = CsvImport(emptyList(), issues, invalid, reason)
        fun issue(reason: CsvProblem) {
            invalid++
            if (issues.size < MAX_REPORTED_ISSUES) issues += CsvIssue(lineNumber, reason)
        }
        while (true) {
            val line = StringBuilder()
            var endOfInput = false
            while (true) {
                val code = input.read()
                if (code == -1) { endOfInput = true; break }
                characters++
                if (characters > MAX_CHARACTERS) return failure(CsvFailure.TOO_LARGE)
                if (code == '\n'.code) break
                line.append(code.toChar())
                if (line.length > MAX_LINE_LENGTH) return failure(CsvFailure.TOO_LARGE)
            }
            if (endOfInput && line.isEmpty()) break
            lineNumber++
            if (lineNumber > MAX_ROWS + 1) return failure(CsvFailure.TOO_LARGE)
            val record = line.toString().removeSuffix("\r")
            val fields = splitRow(if (lineNumber == 1) record.removePrefix("\uFEFF") else record)
            if (lineNumber == 1) {
                if (fields?.map { it.trim().lowercase(java.util.Locale.ROOT) } != listOf("date", "value")) {
                    return failure(CsvFailure.HEADER)
                }
            } else if (line.isNotBlank()) {
                if (fields == null || fields.size != 2) issue(CsvProblem.COLUMNS)
                else {
                    val date = runCatching { LocalDate.parse(fields[0].trim()) }.getOrNull()
                    val value = AmountInput.parseCanonical(fields[1].trim())
                    when {
                        date == null -> issue(CsvProblem.DATE)
                        value == null -> issue(CsvProblem.VALUE)
                        entries.containsKey(date) -> issue(CsvProblem.DUPLICATE_DATE)
                        else -> entries[date] = DailyEntry(date, value)
                    }
                }
            }
            if (endOfInput) break
        }
        if (lineNumber == 0) return failure(CsvFailure.HEADER)
        return CsvImport(entries.values.sortedBy { it.date }, issues, invalid)
    }

    // RFC-style quoted fields and escaped quotes. Multiline records are intentionally rejected.
    private fun splitRow(line: String): List<String>? {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closed = false
        var index = 0
        while (index < line.length) {
            val c = line[index]
            when {
                quoted && c == '"' -> {
                    if (line.getOrNull(index + 1) == '"') { field.append('"'); index++ }
                    else { quoted = false; closed = true }
                }
                quoted -> field.append(c)
                c == ',' -> { fields += field.toString(); field.clear(); closed = false }
                c == '"' && field.isEmpty() && !closed -> quoted = true
                c == '"' || closed -> return null
                else -> field.append(c)
            }
            index++
        }
        if (quoted) return null
        fields += field.toString()
        return fields
    }

    fun write(entries: List<DailyEntry>, writer: Writer) {
        writer.write("date,value\n")
        orderedEntries(entries).forEach {
            writer.write("${it.date},${AmountInput.canonical(it.value)}\n")
        }
    }

    /** Recovery keeps every original slot, including malformed and repeated-date records. */
    fun writeLegacy(snapshot: LegacySnapshot, writer: Writer) {
        val values = if (snapshot.values.isEmpty()) emptyList() else snapshot.values.split(',')
        val dates = if (snapshot.dates.isEmpty()) emptyList() else snapshot.dates.split(',')
        fun quote(s: String) = "\"${s.replace("\"", "\"\"")}\""
        writer.write("index,original_date,original_value\n")
        repeat(maxOf(values.size, dates.size)) { index ->
            writer.write("${index + 1},${quote(dates.getOrElse(index) { "" })},${quote(values.getOrElse(index) { "" })}\n")
        }
    }
}

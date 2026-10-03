package com.example.dailycandle.data

import android.content.ContentResolver
import android.net.Uri
import com.example.dailycandle.domain.CsvCodec
import com.example.dailycandle.domain.CsvImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction

class DataTransfer(private val resolver: ContentResolver, private val repository: BalanceRepository) {
    suspend fun preview(uri: Uri): CsvImport = withContext(Dispatchers.IO) {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val stream = resolver.openInputStream(uri) ?: throw IOException("Unable to open input")
        InputStreamReader(stream, decoder).use(CsvCodec::read)
    }

    suspend fun export(uri: Uri, legacy: Boolean) = withContext(Dispatchers.IO) {
        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Unable to open output")
        stream.bufferedWriter(Charsets.UTF_8).use { writer ->
            if (legacy) CsvCodec.writeLegacy(repository.legacySource.read(), writer)
            else CsvCodec.write(repository.all(), writer)
        }
    }
}

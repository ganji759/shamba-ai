package ai.shamba.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Past scans in one JSON file plus one photo and one thumbnail per scan.
 * Writes go to a temp file first and then replace the old file, so a crash never leaves half a file.
 */
class HistoryStore(rootDir: File) {
    val scansDir: File = File(rootDir, "scans")
    private val file = File(rootDir, "history.json")
    private val mutex = Mutex()
    private val _records = MutableStateFlow<List<ScanRecord>>(emptyList())
    private var loaded = false

    /** Newest first. */
    val records: StateFlow<List<ScanRecord>> = _records.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock { ensureLoaded() }
    }

    suspend fun get(id: String): ScanRecord? {
        load()
        return _records.value.firstOrNull { it.id == id }
    }

    suspend fun add(record: ScanRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoaded()
            val next = (listOf(record) + _records.value.filterNot { it.id == record.id })
                .sortedByDescending { it.timeMillis }
            persist(next)
            _records.value = next
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoaded()
            val victim = _records.value.firstOrNull { it.id == id } ?: return@withLock
            val next = _records.value.filterNot { it.id == id }
            persist(next)
            _records.value = next
            File(scansDir, victim.photoFile).delete()
            File(scansDir, victim.thumbFile).delete()
        }
    }

    fun photoFile(record: ScanRecord): File = File(scansDir, record.photoFile)

    fun thumbFile(record: ScanRecord): File = File(scansDir, record.thumbFile)

    private fun ensureLoaded() {
        if (loaded) return
        scansDir.mkdirs()
        _records.value = try {
            if (file.isFile) HistoryCodec.decode(file.readText()).sortedByDescending { it.timeMillis } else emptyList()
        } catch (e: Exception) {
            // Keep the broken file for support, then start clean. The app must still open.
            Log.e(TAG, "history.json is broken, starting a new one", e)
            file.renameTo(File(file.parentFile, "history.broken.json"))
            emptyList()
        }
        loaded = true
    }

    private fun persist(records: List<ScanRecord>) {
        val tmp = File(file.parentFile, "history.json.tmp")
        tmp.writeText(HistoryCodec.encode(records))
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Could not save history" }
        }
    }

    private companion object {
        const val TAG = "HistoryStore"
    }
}

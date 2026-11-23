package com.example.chess

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

data class SyzygyTableEntry(
    val fileName: String,
    val isWdl: Boolean,
    val sizeBytes: Long
)

data class SyzygyImportReport(
    val importedCount: Int,
    val skippedReasonsAr: List<String>
)

/**
 * Manages optional offline Syzygy endgame tablebases (.rtbw / .rtbz) imported via SAF.
 * Validation rules per Ruqaa Section 5.5:
 * - Name regex: K[QRBNP]{0,4}vK[QRBNP]{0,4}.rtb[wz] with max name length <= 12 before extension? Wait: KQQvKQQ.rtbw = 12 chars total!
 * - Magic bytes: WDL = 71 E8 23 5D, DTZ = D7 66 0C A5
 * - File size: >= 16 bytes, size % 64 == 16, <= 32 MiB per file, <= 128 MiB total
 * - Atomic write via temporary file then rename
 */
object SyzygyManager {

    const val MAX_FILE_BYTES = 32L * 1024L * 1024L
    const val MAX_TOTAL_BYTES = 128L * 1024L * 1024L

    private val NAME_REGEX = Regex("""^K[QRBNP]{0,4}vK[QRBNP]{0,4}\.rtb[wz]$""")
    private val WDL_MAGIC = byteArrayOf(0x71.toByte(), 0xE8.toByte(), 0x23.toByte(), 0x5D.toByte())
    private val DTZ_MAGIC = byteArrayOf(0xD7.toByte(), 0x66.toByte(), 0x0C.toByte(), 0xA5.toByte())

    fun syzygyDir(context: Context): File = File(context.filesDir, "syzygy").apply {
        if (!exists()) mkdirs()
    }

    fun listTables(context: Context): List<SyzygyTableEntry> {
        val dir = syzygyDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".rtbw") || it.name.endsWith(".rtbz")) }
            ?.sortedBy { it.name }
            ?.map {
                SyzygyTableEntry(
                    fileName = it.name,
                    isWdl = it.name.endsWith(".rtbw"),
                    sizeBytes = it.length()
                )
            }
            ?: emptyList()
    }

    fun hasWdlTables(context: Context): Boolean = listTables(context).any { it.isWdl }

    fun totalSizeBytes(context: Context): Long = listTables(context).sumOf { it.sizeBytes }

    fun validateMetadataAndMagic(
        fileName: String,
        header4Bytes: ByteArray,
        sizeBytes: Long
    ): String? {
        if (fileName.length > 12 || !NAME_REGEX.matches(fileName)) {
            return "اسم الملف غير صالح لجداول Syzygy (حتى ٦ قطع بصيغة K..vK...rtbw/.rtbz وبحد ١٢ حرفاً): $fileName"
        }
        val baseWithoutExt = fileName.substringBeforeLast('.')
        if (baseWithoutExt.length > 7) {
            return "اسم الجدول يتجاوز الحد المسموح لـ ٦ قطع: $fileName"
        }
        if (sizeBytes < 16L || sizeBytes > MAX_FILE_BYTES) {
            return "حجم الملف يتجاوز حد ٣٢ ميجابايت أو أقل من الحد الأدنى: $fileName"
        }
        if (sizeBytes % 64L != 16L) {
            return "توافق الحجم (% 64 == 16) غير صحيح للملف: $fileName"
        }
        if (header4Bytes.size < 4) {
            return "ترويسة الملف ناقصة: $fileName"
        }
        val expectedMagic = if (fileName.endsWith(".rtbw")) WDL_MAGIC else DTZ_MAGIC
        for (i in 0 until 4) {
            if (header4Bytes[i] != expectedMagic[i]) {
                return "ترويسة Syzygy غير مطابقة للملف: $fileName"
            }
        }
        return null
    }

    fun importFromUris(context: Context, uris: List<Uri>): SyzygyImportReport {
        val dir = syzygyDir(context)
        var imported = 0
        val errors = mutableListOf<String>()
        var currentTotal = totalSizeBytes(context)

        for (uri in uris) {
            val fileName = resolveFileName(context, uri)
            if (fileName == null) {
                errors.add("تعذّر قراءة اسم أحد الملفات")
                continue
            }
            if (fileName.length > 12 || !NAME_REGEX.matches(fileName) || fileName.substringBeforeLast('.').length > 7) {
                errors.add("تم تخطي $fileName: صيغة الاسم غير مطابقة لـ Syzygy")
                continue
            }
            val tmp = File(dir, "$fileName.tmp")
            var writtenSize = 0L
            val header = ByteArray(4)
            var headerRead = 0
            var oversized = false

            val openResult = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmp).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            if (headerRead < 4) {
                                val take = minOf(4 - headerRead, read)
                                System.arraycopy(buffer, 0, header, headerRead, take)
                                headerRead += take
                            }
                            writtenSize += read
                            if (writtenSize > MAX_FILE_BYTES || currentTotal + writtenSize > MAX_TOTAL_BYTES) {
                                oversized = true
                                break
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: error("تعذّر فتح التدفق")
            }

            if (openResult.isFailure || oversized) {
                tmp.delete()
                errors.add(
                    if (oversized) "تم تخطي $fileName: يتجاوز حد ٣٢ ميجابايت أو الإجمالي ١٢٨ ميجابايت"
                    else "تعذّر نسخ $fileName"
                )
                continue
            }

            val validationError = validateMetadataAndMagic(
                fileName = fileName,
                header4Bytes = if (headerRead == 4) header else ByteArray(0),
                sizeBytes = writtenSize
            )
            if (validationError != null) {
                tmp.delete()
                errors.add(validationError)
                continue
            }

            val target = File(dir, fileName)
            val prevLen = if (target.exists()) target.length() else 0L
            if (target.exists()) target.delete()
            if (tmp.renameTo(target)) {
                currentTotal = currentTotal - prevLen + writtenSize
                imported++
            } else {
                tmp.delete()
                errors.add("فشل الحفظ الذرّي للملف $fileName")
            }
        }
        return SyzygyImportReport(importedCount = imported, skippedReasonsAr = errors)
    }

    fun deleteTable(context: Context, fileName: String): Boolean {
        val f = File(syzygyDir(context), fileName)
        return f.exists() && f.delete()
    }

    fun clearAll(context: Context) {
        syzygyDir(context).listFiles()?.forEach { it.delete() }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}

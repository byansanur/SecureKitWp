package com.byan.securekit.core

import android.content.Context
import java.io.File

/**
 * Validator file path untuk mencegah serangan Path Traversal.
 */
object PathValidation {
    /**
     * Memvalidasi bahwa [fileName] tetap berada di dalam direktori internal [baseDir].
     * @throws SecurityException Jika terdeteksi upaya path traversal atau path absolut
     */
    fun getValidatedFile(context: Context, fileName: String, baseDir: File = context.filesDir): File {
        if (fileName.startsWith("/") || fileName.startsWith("\\") || File(fileName).isAbsolute) {
            throw SecurityException("Absolute path tidak diizinkan! Jalur yang dicoba: $fileName")
        }

        val targetFile = File(baseDir, fileName)

        val canonicalBase = baseDir.canonicalPath
        val canonicalTarget = targetFile.canonicalPath

        val basePrefix = if (canonicalBase.endsWith(File.separator)) canonicalBase else canonicalBase + File.separator

        if (canonicalTarget != canonicalBase && !canonicalTarget.startsWith(basePrefix)) {
            throw SecurityException("Path Traversal Attack terdeteksi! Jalur yang dicoba: $fileName")
        }

        return targetFile
    }
}

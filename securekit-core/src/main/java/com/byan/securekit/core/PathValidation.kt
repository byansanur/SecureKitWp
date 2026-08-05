package com.byan.securekit.core

import android.content.Context
import java.io.File

/**
 * Validator file path untuk mencegah serangan Path Traversal.
 */
object PathValidation {
    /**
     * Memvalidasi bahwa [fileName] tetap berada di dalam direktori internal [baseDir].
     * @throws SecurityException Jika terdeteksi upaya path traversal (misal: "../file.txt")
     */
    fun getValidatedFile(context: Context, fileName: String, baseDir: File = context.filesDir): File {
        val targetFile = File(baseDir, fileName)

        val canonicalBase = baseDir.canonicalPath
        val canonicalTarget = targetFile.canonicalPath

        if (!canonicalTarget.startsWith(canonicalBase)) {
            throw SecurityException("Path Traversal Attack terdeteksi! Jalur yang dicoba: $fileName")
        }

        return targetFile
    }
}

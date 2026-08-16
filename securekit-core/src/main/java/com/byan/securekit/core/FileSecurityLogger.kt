package com.byan.securekit.core

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Implementasi [SecurityLogger] yang menyimpan log terstruktur ke file privat aplikasi.
 * Mendukung label lingkungan ([SecurityEnvironment]: DEV, QA, PT, STAGING, PROD),
 * rotasi ukuran file otomatis, dan ekspor log file untuk kebutuhan analisa/debugging.
 */
class FileSecurityLogger(
    context: Context,
    val environment: SecurityEnvironment = SecurityEnvironment.PROD,
    private val maxFileSizeBytes: Long = 1 * 1024 * 1024 // 1 MB default
) : SecurityLogger {

    private val logDirectory: File = File(context.filesDir, "security_logs")
    private val logFile: File = File(logDirectory, "security.log")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
    }

    @SuppressLint("LogUsage")
    override fun d(tag: String, message: String) {
        val entry = formatLogEntry("DEBUG", tag, message, null)
        writeLogEntry(entry)

        // Log ke Logcat hanya pada lingkungan non-produksi (DEV, QA, PT, STAGING)
        if (environment.isNonProd) {
            try {
                Log.d("SecureKit[$environment][$tag]", message)
            } catch (t: Throwable) {
                // Ignore Logcat error on host JVM unit testing
            }
        }
    }

    @SuppressLint("LogUsage")
    override fun e(tag: String, message: String, throwable: Throwable?) {
        val entry = formatLogEntry("ERROR", tag, message, throwable)
        writeLogEntry(entry)

        // Log ke Logcat hanya pada lingkungan non-produksi (DEV, QA, PT, STAGING)
        if (environment.isNonProd) {
            try {
                Log.e("SecureKit[$environment][$tag]", message, throwable)
            } catch (t: Throwable) {
                // Ignore Logcat error on host JVM unit testing
            }
        }
    }

    /**
     * Mengambil referensi file log saat ini.
     */
    fun getLogFile(): File = logFile

    /**
     * Mengekspor isi log ke [targetFile] yang ditentukan.
     */
    fun exportLogFile(targetFile: File): SecureResult<File> {
        return try {
            if (!logFile.exists()) {
                return SecureResult.Error(IllegalStateException("File log belum dibuat"))
            }

            logFile.copyTo(targetFile, overwrite = true)
            SecureResult.Success(targetFile)
        } catch (e: Exception) {
            SecureResult.Error(e, "Gagal mengekspor file log")
        }
    }

    /**
     * Membersihkan seluruh isi log file.
     */
    fun clearLogs() {
        synchronized(this) {
            if (logFile.exists()) {
                logFile.writeText("")
            }
        }
    }

    @Synchronized
    private fun writeLogEntry(entry: String) {
        try {
            checkAndRotateFile()
            FileOutputStream(logFile, true).use { fos ->
                fos.write(entry.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            // Ignore log writing failure to avoid crashing the app
        }
    }

    private fun checkAndRotateFile() {
        if (logFile.exists() && logFile.length() >= maxFileSizeBytes) {
            val backupFile = File(logDirectory, "security_old.log")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
        }
    }

    private fun formatLogEntry(level: String, tag: String, message: String, throwable: Throwable?): String {
        val timestamp = dateFormat.format(Date())
        val sb = StringBuilder()
        sb.append("[$timestamp] [${environment.name}] [$level] [$tag] $message\n")
        if (throwable != null) {
            sb.append("   Exception: ").append(throwable.javaClass.name).append(": ").append(throwable.message).append("\n")
            for (element in throwable.stackTrace.take(5)) {
                sb.append("     at ").append(element.toString()).append("\n")
            }
        }
        return sb.toString()
    }
}

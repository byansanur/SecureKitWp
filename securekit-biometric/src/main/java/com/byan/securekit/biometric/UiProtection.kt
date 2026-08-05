package com.byan.securekit.biometric

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager

/**
 * Modul Perlindungan UI Runtime (FLAG_SECURE, Anti-Tapjacking, Clipboard Sanitizer).
 */
object UiProtection {

    /**
     * Menerapkan FLAG_SECURE pada Activity untuk memblokir screenshot dan screen recording.
     */
    fun enableScreenProtection(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * Mencegah serangan Tapjacking (Overlay Attack) pada View sensitif.
     */
    fun preventTapjacking(view: View) {
        view.filterTouchesWhenObscured = true
    }

    /**
     * Membersihkan papan klip (Clipboard) sistem.
     * Menggunakan API `clearPrimaryClip()` untuk Android 9+ (API 28+ / Pie).
     */
    fun clearClipboard(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (e: Exception) {
            // Ignore clipboard errors on restricted system services
        }
    }
}

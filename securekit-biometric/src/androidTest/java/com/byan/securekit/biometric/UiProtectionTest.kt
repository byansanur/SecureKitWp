package com.byan.securekit.biometric

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiProtectionTest {

    @Test
    fun testPreventTapjackingSetsFilterTouchesWhenObscured() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = View(context)

        UiProtection.preventTapjacking(view)

        assertTrue(view.filterTouchesWhenObscured)
    }

    @Test
    fun testClearClipboardDoesNotThrowException() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Ensure clearClipboard runs safely without throwing exceptions across API levels
        UiProtection.clearClipboard(context)
    }
}

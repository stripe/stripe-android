package com.stripe.android.test.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

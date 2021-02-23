package com.stripe.android.test.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class EndToEndTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val service = ServiceFactory().create(
        baseUrl = BASE_URL
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    private companion object {
        private const val BASE_URL = "https://stp-e2e.glitch.me/"
    }
}

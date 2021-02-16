package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class DefaultDeviceIdRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val defaultDeviceIdRepository = DefaultDeviceIdRepository(
        ApplicationProvider.getApplicationContext(),
        testDispatcher
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `get() should return same value across multiple calls`() = testDispatcher.runBlockingTest {
        assertThat(defaultDeviceIdRepository.get())
            .isEqualTo(defaultDeviceIdRepository.get())
    }
}

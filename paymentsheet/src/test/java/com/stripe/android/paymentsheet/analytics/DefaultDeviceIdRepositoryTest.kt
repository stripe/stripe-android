package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class DefaultDeviceIdRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val defaultDeviceIdRepository = DefaultDeviceIdRepository(
        ApplicationProvider.getApplicationContext(),
        testDispatcher
    )

    @Test
    fun `get() should return same value across multiple calls`() = runTest {
        assertThat(defaultDeviceIdRepository.get())
            .isEqualTo(defaultDeviceIdRepository.get())
    }
}

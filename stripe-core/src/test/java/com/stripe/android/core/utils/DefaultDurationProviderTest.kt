package com.stripe.android.core.utils

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DefaultDurationProviderTest {

    /**
     * Creates a fresh [DefaultDurationProvider] instance for each test via reflection,
     * avoiding shared singleton state.
     */
    private fun createProvider(): DurationProvider {
        val constructor = DefaultDurationProvider::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance()
    }

    @Test
    fun `completedDuration returns null when key was never measured`() {
        val provider = createProvider()
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad)).isNull()
    }

    @Test
    fun `end stores completed duration retrievable via completedDuration`() {
        val provider = createProvider()
        provider.start(DurationProvider.Key.PaymentSheetLoadSessionLoad)
        val returned = provider.end(DurationProvider.Key.PaymentSheetLoadSessionLoad)
        assertThat(returned).isNotNull()
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad))
            .isEqualTo(returned)
    }

    @Test
    fun `measureDuration stores completed duration after block completes`() = runTest {
        val provider = createProvider()
        provider.measureDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad) { }
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad)).isNotNull()
    }

    @Test
    fun `measureDuration clears previous completed duration before recording a new one`() = runTest {
        val provider = createProvider()

        provider.measureDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad) { }
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad)).isNotNull()

        // Second call must clear and re-record
        provider.measureDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad) { }
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad)).isNotNull()
    }

    @Test
    fun `start with reset=true clears previous completed duration`() {
        val provider = createProvider()
        provider.start(DurationProvider.Key.PaymentSheetLoadSessionLoad)
        provider.end(DurationProvider.Key.PaymentSheetLoadSessionLoad)
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad)).isNotNull()

        provider.start(DurationProvider.Key.PaymentSheetLoadSessionLoad, reset = true)
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad)).isNull()
    }

    @Test
    fun `start with reset=false preserves previous completed duration`() {
        val provider = createProvider()
        provider.start(DurationProvider.Key.PaymentSheetLoadSessionLoad)
        val first = provider.end(DurationProvider.Key.PaymentSheetLoadSessionLoad)
        assertThat(first).isNotNull()

        // Key no longer in active store after end(); reset=false means do not clear completedStore
        provider.start(DurationProvider.Key.PaymentSheetLoadSessionLoad, reset = false)
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad))
            .isEqualTo(first)
    }

    @Test
    fun `keys not measured remain absent from completedDuration`() = runTest {
        val provider = createProvider()
        provider.measureDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad) { }

        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadPrefetchPMs)).isNull()
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadCreateLinkState)).isNull()
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadCreateCustomerState)).isNull()
    }

    @Test
    fun `different keys have independent completed durations`() = runTest {
        val provider = createProvider()
        provider.measureDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad) { }
        provider.measureDuration(DurationProvider.Key.PaymentSheetLoadCreateLinkState) { }

        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadSessionLoad)).isNotNull()
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadCreateLinkState)).isNotNull()
        assertThat(provider.completedDuration(DurationProvider.Key.PaymentSheetLoadPrefetchPMs)).isNull()
    }
}

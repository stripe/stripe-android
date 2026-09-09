package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.common.taptoadd.FakeTapToAddConnectionManager
import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.core.ApiConfiguration
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class TapToAddConnectionStarterTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiConfiguration = ApiConfiguration.State(
        publishableKey = "pk_test_123",
        stripeAccountId = "acct_123",
    )

    @Test
    fun `isSupported delegates to manager`() = runTest(testDispatcher) {
        val manager = FakeTapToAddConnectionManager.noOp(isSupported = true)
        val starter = DefaultTapToAddConnectionStarter(
            tapToAddConnectionManager = manager,
            viewModelScope = this,
            coroutineContext = testDispatcher,
        )

        assertThat(starter.isSupported(apiConfiguration)).isTrue()
    }

    @Test
    fun `isSupported is false when manager not supported`() = runTest(testDispatcher) {
        val manager = FakeTapToAddConnectionManager.noOp(isSupported = false)
        val starter = DefaultTapToAddConnectionStarter(
            tapToAddConnectionManager = manager,
            viewModelScope = this,
            coroutineContext = testDispatcher,
        )

        assertThat(starter.isSupported(apiConfiguration)).isFalse()
    }

    @Test
    fun `start calls manager connect with merchant display name from configuration`() = runTest(testDispatcher) {
        val manager = FakeTapToAddConnectionManager.noOp(isSupported = true)
        val starter = DefaultTapToAddConnectionStarter(
            tapToAddConnectionManager = manager,
            viewModelScope = this,
            coroutineContext = testDispatcher,
        )

        val commonConfiguration = CommonConfigurationFactory.create(
            merchantDisplayName = "Books & Things",
        )

        starter.start(commonConfiguration, apiConfiguration)
        advanceUntilIdle()

        assertThat(manager.connectCalls.awaitItem()).isEqualTo(
            FakeTapToAddConnectionManager.ConnectCall(
                config = TapToAddConnectionManager.ConnectionConfig(
                    merchantDisplayName = "Books & Things",
                    apiConfiguration = apiConfiguration,
                ),
            )
        )
        manager.connectCalls.ensureAllEventsConsumed()
    }
}

package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.common.taptoadd.FakeTapToAddConnectionManager
import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class TapToAddConnectionStarterTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `isSupported delegates to manager`() = runTest(testDispatcher) {
        val manager = FakeTapToAddConnectionManager.noOp(isSupported = true)
        val starter = DefaultTapToAddConnectionStarter(
            tapToAddConnectionManager = manager,
            viewModelScope = this,
            coroutineContext = testDispatcher,
        )

        assertThat(starter.isSupported("pk_test_123", false)).isTrue()
    }

    @Test
    fun `isSupported is false when manager not supported`() = runTest(testDispatcher) {
        val manager = FakeTapToAddConnectionManager.noOp(isSupported = false)
        val starter = DefaultTapToAddConnectionStarter(
            tapToAddConnectionManager = manager,
            viewModelScope = this,
            coroutineContext = testDispatcher,
        )

        assertThat(starter.isSupported("pk_test_123", false)).isFalse()
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

        starter.start(commonConfiguration, "pk_test_123", false)
        advanceUntilIdle()

        assertThat(manager.connectCalls.awaitItem()).isEqualTo(
            FakeTapToAddConnectionManager.ConnectCall(
                config = TapToAddConnectionManager.ConnectionConfig(
                    merchantDisplayName = "Books & Things",
                    publishableKey = "pk_test_123",
                    isLiveMode = false,
                ),
            )
        )
        manager.connectCalls.ensureAllEventsConsumed()
    }
}

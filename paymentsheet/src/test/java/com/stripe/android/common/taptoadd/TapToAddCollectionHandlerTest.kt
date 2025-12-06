package com.stripe.android.common.taptoadd

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TapToAddCollectionHandlerTest {
    @Test
    fun `create returns UnsupportedTapToAddCollectionHandler when terminal SDK is not available`() {
        val handler = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = { false },
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true, isConnected = false),
        )

        assertThat(handler).isInstanceOf(UnsupportedTapToAddCollectionHandler::class.java)
    }

    @Test
    fun `create returns DefaultTapToAddCollectionHandler when terminal SDK is available`() {
        val handler = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = { true },
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true, isConnected = false),
        )

        assertThat(handler).isInstanceOf(DefaultTapToAddCollectionHandler::class.java)
    }

    @Test
    fun `handler returns Collected when already connected`() = runScenario(
        isConnected = true,
    ) {
        val result = handler.collect(DEFAULT_METADATA)

        assertThat(result).isEqualTo(TapToAddCollectionHandler.CollectionState.Collected)
    }

    @Test
    fun `handler calls connect and returns Collected when not connected`() = runScenario(
        isConnected = false,
        awaitResult = Result.success(true)
    ) {
        val result = handler.collect(DEFAULT_METADATA)

        assertThat(managerScenario.connectCalls.awaitItem()).isNotNull()
        assertThat(managerScenario.awaitCalls.awaitItem()).isNotNull()

        assertThat(result).isEqualTo(TapToAddCollectionHandler.CollectionState.Collected)
    }

    @Test
    fun `handler returns FailedCollection when await fails`() {
        val error = IllegalStateException("Failed")

        runScenario(
            isConnected = false,
            awaitResult = Result.failure(error)
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertThat(managerScenario.connectCalls.awaitItem()).isNotNull()
            assertThat(managerScenario.awaitCalls.awaitItem()).isNotNull()

            assertThat(result).isEqualTo(
                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = error,
                    displayMessage = R.string.stripe_something_went_wrong.resolvableString
                )
            )
        }
    }

    private fun runScenario(
        isConnected: Boolean = true,
        awaitResult: Result<Boolean> = Result.success(true),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        FakeTapToAddConnectionManager.test(
            isSupported = true,
            isConnected = isConnected,
            awaitResult = awaitResult,
        ) {
            block(
                Scenario(
                    handler = DefaultTapToAddCollectionHandler(
                        connectionManager = tapToAddConnectionManager,
                    ),
                    managerScenario = this,
                )
            )
        }
    }

    private class Scenario(
        val handler: TapToAddCollectionHandler,
        val managerScenario: FakeTapToAddConnectionManager.Scenario,
    )

    private companion object {
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)
    }
}

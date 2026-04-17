package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.stripeterminal.BuildConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultIsStripeTerminalSdkAvailableTest {
    @Test
    fun `returns true when terminal SDK classes are on classpath & valid Terminal version`() = runScenario(
        validatorResult = true,
    ) {
        assertThat(isStripeTerminalSdkAvailable()).isTrue()
        assertThat(validatorCalls.awaitItem()).isEqualTo(BuildConfig.SDK_VERSION_NAME)
    }

    @Test
    fun `returns false when version validator rejects SDK version`() = runScenario(
        validatorResult = false,
    ) {
        assertThat(isStripeTerminalSdkAvailable()).isFalse()
        assertThat(validatorCalls.awaitItem()).isEqualTo(BuildConfig.SDK_VERSION_NAME)
    }

    private fun runScenario(
        validatorResult: Boolean,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val versionValidator = FakeStripeTerminalVersionValidator(validatorResult)
        val isAvailable = DefaultIsStripeTerminalSdkAvailable(versionValidator)

        block(
            Scenario(
                isStripeTerminalSdkAvailable = isAvailable,
                validatorCalls = versionValidator.calls,
            )
        )

        versionValidator.calls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
        val validatorCalls: ReceiveTurbine<String>,
    )

    private class FakeStripeTerminalVersionValidator(
        val result: Boolean,
    ) : StripeTerminalVersionValidator {
        val calls = Turbine<String>()

        override fun invoke(versionName: String): Boolean {
            calls.add(versionName)

            return result
        }
    }
}

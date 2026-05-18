package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultIsStripeTerminalSdkAvailableTest {
    @Test
    fun `returns true when terminal SDK classes are on classpath & valid Terminal version`() = runScenario(
        validatorResult = true,
        versionName = "5.4.1",
    ) {
        assertThat(isStripeTerminalSdkAvailable()).isTrue()
        assertThat(coreLibraryCalls.awaitItem()).isEqualTo(Unit)
        assertThat(tapToPayCalls.awaitItem()).isEqualTo(Unit)
        assertThat(validatorCalls.awaitItem()).isEqualTo("5.4.1")
    }

    @Test
    fun `returns false when version validator rejects SDK version`() = runScenario(
        validatorResult = false,
        versionName = "5.4.1",
    ) {
        assertThat(isStripeTerminalSdkAvailable()).isFalse()
        assertThat(coreLibraryCalls.awaitItem()).isEqualTo(Unit)
        assertThat(tapToPayCalls.awaitItem()).isEqualTo(Unit)
        assertThat(validatorCalls.awaitItem()).isEqualTo("5.4.1")
    }

    @Test
    fun `returns false when core library is not on classpath`() = runScenario(
        validatorResult = true,
        versionName = "5.4.1",
        shouldFailTerminalCoreLibraryCheck = true,
    ) {
        assertThat(isStripeTerminalSdkAvailable()).isFalse()
        assertThat(coreLibraryCalls.awaitItem()).isEqualTo(Unit)
        tapToPayCalls.expectNoEvents()
        validatorCalls.expectNoEvents()
    }

    @Test
    fun `returns false when tap to pay library is not on classpath`() = runScenario(
        validatorResult = true,
        versionName = "5.4.1",
        shouldFailTerminalTapToPayLibraryCheck = true,
    ) {
        assertThat(isStripeTerminalSdkAvailable()).isFalse()
        assertThat(coreLibraryCalls.awaitItem()).isEqualTo(Unit)
        assertThat(tapToPayCalls.awaitItem()).isEqualTo(Unit)
        validatorCalls.expectNoEvents()
    }

    @Test
    fun `returns false when version validator throws`() = runScenario(
        validatorResult = true,
        versionName = "5.4.1",
        validatorError = IllegalStateException("validator failed"),
    ) {
        assertThat(isStripeTerminalSdkAvailable()).isFalse()
        assertThat(coreLibraryCalls.awaitItem()).isEqualTo(Unit)
        assertThat(tapToPayCalls.awaitItem()).isEqualTo(Unit)
        assertThat(validatorCalls.awaitItem()).isEqualTo("5.4.1")
    }

    @Test
    fun `checking Terminal Core library returns version name from library`() {
        val coreSdk = DefaultHasStripeTerminalCoreLibrary()

        assertThat(coreSdk.invoke())
            .isEqualTo(com.stripe.stripeterminal.BuildConfig.SDK_VERSION_NAME)
    }

    private fun runScenario(
        validatorResult: Boolean,
        versionName: String,
        shouldFailTerminalCoreLibraryCheck: Boolean = false,
        shouldFailTerminalTapToPayLibraryCheck: Boolean = false,
        validatorError: Exception? = null,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val versionValidator = FakeStripeTerminalVersionValidator(
            result = validatorResult,
            error = validatorError,
        )
        val hasStripeTerminalCoreLibrary = FakeHasStripeTerminalCoreLibrary(
            versionName = versionName,
            shouldFail = shouldFailTerminalCoreLibraryCheck
        )
        val hasStripeTerminalTapToPayLibrary = FakeHasStripeTerminalTapToPayLibrary(
            shouldFail = shouldFailTerminalTapToPayLibraryCheck
        )
        val isAvailable = DefaultIsStripeTerminalSdkAvailable(
            versionValidator = versionValidator,
            hasStripeTerminalCoreLibrary = hasStripeTerminalCoreLibrary,
            hasStripeTerminalTapToPayLibrary = hasStripeTerminalTapToPayLibrary,
        )

        block(
            Scenario(
                isStripeTerminalSdkAvailable = isAvailable,
                coreLibraryCalls = hasStripeTerminalCoreLibrary.calls,
                tapToPayCalls = hasStripeTerminalTapToPayLibrary.calls,
                validatorCalls = versionValidator.calls,
            )
        )

        hasStripeTerminalCoreLibrary.calls.ensureAllEventsConsumed()
        hasStripeTerminalTapToPayLibrary.calls.ensureAllEventsConsumed()
        versionValidator.calls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
        val coreLibraryCalls: ReceiveTurbine<Unit>,
        val tapToPayCalls: ReceiveTurbine<Unit>,
        val validatorCalls: ReceiveTurbine<String>,
    )

    private class FakeHasStripeTerminalCoreLibrary(
        val versionName: String,
        val shouldFail: Boolean,
    ) : HasStripeTerminalCoreLibrary {
        val calls = Turbine<Unit>()

        override fun invoke(): String {
            calls.add(Unit)

            if (shouldFail) {
                throw ClassNotFoundException("com.stripe.stripeterminal.TerminalApplicationDelegate")
            }

            return versionName
        }
    }

    private class FakeHasStripeTerminalTapToPayLibrary(
        val shouldFail: Boolean,
    ) : HasStripeTerminalTapToPayLibrary {
        val calls = Turbine<Unit>()

        override fun invoke() {
            calls.add(Unit)

            if (shouldFail) {
                throw ClassNotFoundException("com.stripe.stripeterminal.taptopay.TapToPay")
            }
        }
    }

    private class FakeStripeTerminalVersionValidator(
        val result: Boolean,
        val error: Exception? = null,
    ) : StripeTerminalVersionValidator {
        val calls = Turbine<String>()

        override fun invoke(versionName: String): Boolean {
            calls.add(versionName)

            error?.let { throw it }

            return result
        }
    }
}

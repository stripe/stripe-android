package com.stripe.android.paymentsheet.paymentdatacollection.upi

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class UpiAppChooserViewModelTest {
    @Test
    fun `chooser preserves URL and does not target a package or new task`() = runScenario {
        val chooser = viewModel.createLaunchIntent()
        val target = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))

        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        assertThat(chooser.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo("Pay with")
        assertThat(target.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(target.data.toString()).isEqualTo(UpiFixtures.URL)
        assertThat(target.`package`).isNull()
        assertThat(target.component).isNull()
        assertThat(target.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(0)
        assertThat(resolver.calls.awaitItem().data).isEqualTo(target.data)
    }

    @Test
    fun `no compatible app returns a localized failure`() = runScenario(available = false) {
        val cause = assertFailsWith<ActivityNotFoundException> { viewModel.createLaunchIntent() }
        assertThat(resolver.calls.awaitItem().action).isEqualTo(Intent.ACTION_VIEW)
        val result = viewModel.getFailure(cause)

        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception?.message).isEqualTo("No compatible UPI app")
        assertThat(result.clientSecret).isEqualTo(UpiFixtures.ARGS.clientSecret)
        assertThat(result.stripeAccountId).isEqualTo("acct_upi")
        assertThat(errorReporter.awaitCall().errorEvent).isEqualTo(UpiAppChooserError.NoCompatibleApp)
    }

    @Test
    fun `missing URL fails before resolving apps`() = runScenario(args = UpiFixtures.ARGS.copy(mobileAuthUrl = null)) {
        val cause = assertFailsWith<IllegalArgumentException> { viewModel.createLaunchIntent() }
        val result = viewModel.getFailure(cause)

        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception?.message).isEqualTo("Unable to authenticate")
        assertThat(errorReporter.awaitCall().errorEvent).isEqualTo(UpiAppChooserError.InvalidArgs)
    }

    @Test
    fun `empty URL fails before resolving apps`() = runScenario(args = UpiFixtures.ARGS.copy(mobileAuthUrl = "")) {
        assertFailsWith<IllegalArgumentException> { viewModel.createLaunchIntent() }
    }

    @Test
    fun `HTTPS URL never opens a browser`() = runScenario(
        args = UpiFixtures.ARGS.copy(mobileAuthUrl = "https://payments.stripe.com/upi/instructions/test"),
    ) {
        assertFailsWith<IllegalArgumentException> { viewModel.createLaunchIntent() }
    }

    @Test
    fun `nonpayment UPI host is rejected`() = runScenario(
        args = UpiFixtures.ARGS.copy(mobileAuthUrl = "upi://collect?pa=merchant@upi"),
    ) {
        assertFailsWith<IllegalArgumentException> { viewModel.createLaunchIntent() }
    }

    @Test
    fun `demo scheme is not accepted in production flow`() = runScenario(
        args = UpiFixtures.ARGS.copy(mobileAuthUrl = "stripe-upi-demo://pay"),
    ) {
        assertFailsWith<IllegalArgumentException> { viewModel.createLaunchIntent() }
    }

    @Test
    fun `missing client secret fails before resolving apps`() = runScenario(
        args = UpiFixtures.ARGS.copy(clientSecret = ""),
    ) {
        assertFailsWith<IllegalArgumentException> { viewModel.createLaunchIntent() }
    }

    @Test
    fun `security exception has UPI launch diagnostics without leaking URI`() = runScenario {
        val result = viewModel.getFailure(SecurityException(UpiFixtures.URL))

        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception?.message).isEqualTo("Unable to authenticate")
        val report = errorReporter.awaitCall()
        assertThat(report.errorEvent).isEqualTo(UpiAppChooserError.LaunchFailed)
        assertThat(report.stripeException?.message).doesNotContain(UpiFixtures.URL)
    }

    @Test
    fun `return requests verification with no source cancellation or claimed success`() = runScenario {
        // The complete result is the contract: UNKNOWN outcome, no exception, no source or cancellation.
        assertThat(viewModel.getResult()).isEqualTo(
            PaymentFlowResult.Unvalidated(
                clientSecret = UpiFixtures.ARGS.clientSecret,
                flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                stripeAccountId = "acct_upi",
            )
        )
    }

    @Test
    fun `initial resume is not a return`() = runScenario {
        viewModel.hasLaunched = true
        assertThat(viewModel.hasLeftForExternalActivity).isFalse()
    }

    @Test
    fun `configuration change is not leaving for external app`() = runScenario {
        viewModel.hasLaunched = true
        viewModel.onStop(isChangingConfigurations = true)
        assertThat(viewModel.hasLeftForExternalActivity).isFalse()
    }

    @Test
    fun `stopping before launch is not leaving for external app`() = runScenario {
        viewModel.onStop(isChangingConfigurations = false)
        assertThat(viewModel.hasLeftForExternalActivity).isFalse()
    }

    @Test
    fun `stopping after launch records external app departure`() = runScenario {
        viewModel.hasLaunched = true
        viewModel.onStop(isChangingConfigurations = false)
        assertThat(viewModel.hasLeftForExternalActivity).isTrue()
    }

    @Test
    fun `launch and departure survive restored saved state`() = runScenario {
        viewModel.hasLaunched = true
        viewModel.onStop(isChangingConfigurations = false)
        val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) })
        val restoredViewModel = createViewModel(UpiFixtures.ARGS, restoredState, resolver, errorReporter)

        assertThat(restoredViewModel.hasLaunched).isTrue()
        assertThat(restoredViewModel.hasLeftForExternalActivity).isTrue()
    }

    @Test
    fun `process recreation after state saved before stop still verifies on return`() = runScenario {
        viewModel.hasLaunched = true
        assertThat(viewModel.hasLeftForExternalActivity).isFalse()
        val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) })
        val restoredViewModel = createViewModel(UpiFixtures.ARGS, restoredState, resolver, errorReporter)

        assertThat(restoredViewModel.hasLaunched).isTrue()
        assertThat(restoredViewModel.hasLeftForExternalActivity).isTrue()
    }

    private fun runScenario(
        args: UpiAppChooserContract.Args = UpiFixtures.ARGS,
        available: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val resolver = FakeUpiActivityResolver(available)
        val errorReporter = FakeErrorReporter()
        val savedState = SavedStateHandle()
        val viewModel = createViewModel(args, savedState, resolver, errorReporter)
        Scenario(viewModel, savedState, resolver, errorReporter).block()
        resolver.calls.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private fun createViewModel(
        args: UpiAppChooserContract.Args,
        savedState: SavedStateHandle,
        resolver: FakeUpiActivityResolver,
        errorReporter: FakeErrorReporter,
    ) = UpiAppChooserViewModel(
        args = args,
        savedStateHandle = savedState,
        canResolveActivity = resolver::resolve,
        chooserTitle = "Pay with",
        noCompatibleAppMessage = "No compatible UPI app",
        launchFailedMessage = "Unable to authenticate",
        errorReporter = errorReporter,
    )

    private data class Scenario(
        val viewModel: UpiAppChooserViewModel,
        val savedState: SavedStateHandle,
        val resolver: FakeUpiActivityResolver,
        val errorReporter: FakeErrorReporter,
    )
}

internal class FakeUpiActivityResolver(private val available: Boolean) {
    val calls = Turbine<Intent>()

    fun resolve(intent: Intent): Boolean {
        calls.add(intent)
        return available
    }
}

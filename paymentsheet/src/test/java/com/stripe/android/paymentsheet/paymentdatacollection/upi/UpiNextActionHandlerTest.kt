package com.stripe.android.paymentsheet.paymentdatacollection.upi

import android.app.Application
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.PaymentSheetNextActionHandlers
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.FakeActivityResultLauncher
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpiNextActionHandlerTest {
    @Test
    fun `UPI is registered with PaymentSheet`() {
        assertThat(PaymentSheetNextActionHandlers.get()[StripeIntent.NextActionData.UpiRedirect::class.java])
            .isInstanceOf(UpiNextActionHandler::class.java)
    }

    @Test
    fun `registers the UPI contract and forwards its verification result`() = runScenario {
        handler.onNewActivityResultCaller(caller, results::add)
        val registration = caller.calls.awaitItem()
        assertThat(registration.contract).isInstanceOf(UpiAppChooserContract::class.java)
        val result = PaymentFlowResult.Unvalidated(clientSecret = UpiFixtures.ARGS.clientSecret)
        registration.callback.onActivityResult(result)
        assertThat(results.awaitItem()).isSameInstanceAs(result)
    }

    @Test
    fun `launch preserves client secret URL and request-specific configuration`() = runScenario {
        register()
        handler.performNextAction(host, intent, requestOptions)
        assertThat(caller.launcher.calls.awaitItem().input).isEqualTo(UpiFixtures.ARGS)
    }

    @Test
    fun `missing URL reaches chooser validation without handler crash`() = runScenario {
        register()
        handler.performNextAction(
            host,
            intent.copy(nextActionData = StripeIntent.NextActionData.UpiRedirect(null)),
            requestOptions,
        )
        assertThat(caller.launcher.calls.awaitItem().input.mobileAuthUrl).isNull()
    }

    @Test
    fun `invalidation unregisters and clears the launcher`() = runScenario {
        register()
        handler.onLauncherInvalidated()
        assertThat(caller.launcher.unregisterCalls.awaitItem()).isEqualTo(Unit)
        handler.performNextAction(host, intent, requestOptions)
        assertThat(reporterConfigurations.awaitItem()).isEqualTo(UpiFixtures.ARGS.apiConfiguration)
        assertThat(errorReporter.awaitCall().errorEvent).isEqualTo(UpiAppChooserError.MissingLauncher)
    }

    @Test
    fun `new caller replaces and unregisters old launcher`() = runScenario {
        register()
        val newCaller = FakeUpiActivityResultCaller()
        handler.onNewActivityResultCaller(newCaller, results::add)
        assertThat(caller.launcher.unregisterCalls.awaitItem()).isEqualTo(Unit)
        assertThat(newCaller.calls.awaitItem().contract).isInstanceOf(UpiAppChooserContract::class.java)
        handler.performNextAction(host, intent, requestOptions)
        assertThat(newCaller.launcher.calls.awaitItem().input).isEqualTo(UpiFixtures.ARGS)
        newCaller.ensureAllEventsConsumed()
    }

    @Test
    fun `missing registration reports UPI error without browser or legacy launch`() = runScenario {
        handler.performNextAction(host, intent, requestOptions)
        assertThat(reporterConfigurations.awaitItem()).isEqualTo(UpiFixtures.ARGS.apiConfiguration)
        assertThat(errorReporter.awaitCall().errorEvent).isEqualTo(UpiAppChooserError.MissingLauncher)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val host = FakeUpiHost()
        val caller = FakeUpiActivityResultCaller()
        val errorReporter = FakeErrorReporter()
        val reporterConfigurations = Turbine<ApiConfiguration.State>()
        val results = Turbine<PaymentFlowResult.Unvalidated>()
        val handler = UpiNextActionHandler { _, configuration ->
            reporterConfigurations.add(configuration)
            errorReporter
        }
        Scenario(handler, host, caller, errorReporter, reporterConfigurations, results).block()
        caller.ensureAllEventsConsumed()
        host.calls.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
        reporterConfigurations.ensureAllEventsConsumed()
        results.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val handler: UpiNextActionHandler,
        val host: FakeUpiHost,
        val caller: FakeUpiActivityResultCaller,
        val errorReporter: FakeErrorReporter,
        val reporterConfigurations: Turbine<ApiConfiguration.State>,
        val results: Turbine<PaymentFlowResult.Unvalidated>,
    ) {
        val requestOptions = ApiRequest.Options("pk_test_upi", "acct_upi")
        val intent = PaymentIntentFactory.create(clientSecret = UpiFixtures.ARGS.clientSecret).copy(
            nextActionData = StripeIntent.NextActionData.UpiRedirect(UpiFixtures.URL),
        )

        suspend fun register() {
            handler.onNewActivityResultCaller(caller, results::add)
            assertThat(caller.calls.awaitItem().contract).isInstanceOf(UpiAppChooserContract::class.java)
        }
    }
}

internal class FakeUpiActivityResultCaller : ActivityResultCaller {
    val calls = Turbine<Registration>()
    val launcher = FakeActivityResultLauncher<UpiAppChooserContract.Args>()

    @Suppress("UNCHECKED_CAST")
    override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        calls.add(Registration(contract, callback as ActivityResultCallback<PaymentFlowResult.Unvalidated>))
        return launcher as ActivityResultLauncher<I>
    }

    override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> = registerForActivityResult(contract, callback)

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
        launcher.calls.ensureAllEventsConsumed()
        launcher.unregisterCalls.ensureAllEventsConsumed()
    }

    data class Registration(
        val contract: ActivityResultContract<*, *>,
        val callback: ActivityResultCallback<PaymentFlowResult.Unvalidated>,
    )
}

internal class FakeUpiHost : AuthActivityStarterHost {
    override val application: Application = ApplicationProvider.getApplicationContext()
    override val statusBarColor: Int? = null
    override val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
    val calls = Turbine<Call>()

    override fun startActivityForResult(target: Class<*>, extras: Bundle, requestCode: Int) {
        calls.add(Call(target, extras, requestCode))
    }

    data class Call(val target: Class<*>, val extras: Bundle, val requestCode: Int)
}

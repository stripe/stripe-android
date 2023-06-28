package com.stripe.android.googlepaylauncher.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayLauncherContract
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.Result.Completed
import com.stripe.android.googlepaylauncher.rememberGooglePayPaymentMethodLauncher
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private val defaultConfig = GooglePayPaymentMethodLauncher.Config(
    environment = GooglePayEnvironment.Test,
    merchantCountryCode = "US",
    merchantName = "Widget Store",
)

internal fun runGooglePayPaymentMethodLauncherTest(
    integrationTypes: Array<LauncherIntegrationType> = LauncherIntegrationType.values(),
    result: GooglePayPaymentMethodLauncher.Result = Completed(CARD_PAYMENT_METHOD),
    isReady: Boolean = true,
    expectResult: Boolean = true,
    block: (ComponentActivity, GooglePayPaymentMethodLauncher) -> Unit,
) {
    for (integrationType in integrationTypes) {
        runGooglePayPaymentMethodLauncherTest(
            integrationType = integrationType,
            isReady = isReady,
            result = result,
            expectResult = expectResult,
            block = block,
        )
    }
}

private fun runGooglePayPaymentMethodLauncherTest(
    integrationType: LauncherIntegrationType,
    isReady: Boolean,
    result: GooglePayPaymentMethodLauncher.Result,
    expectResult: Boolean,
    block: (ComponentActivity, GooglePayPaymentMethodLauncher) -> Unit,
) {
    val readyCallback = mock<GooglePayPaymentMethodLauncher.ReadyCallback>()
    val resultCallback = mock<GooglePayPaymentMethodLauncher.ResultCallback>()

    Mockito.mockStatic(Wallet::class.java).use { wallet ->
        val paymentsClient = mock<PaymentsClient>()
        whenever(paymentsClient.isReadyToPay(any())).thenReturn(Tasks.forResult(isReady))

        // Provide a static mock here because DefaultGooglePayRepository calls this static
        // create method.
        wallet.`when`<PaymentsClient> {
            Wallet.getPaymentsClient(isA<Context>(), any())
        }.thenReturn(paymentsClient)

        // Mock the return value from GooglePayLauncherActivity so that we immediately return
        val resultData = Intent().putExtras(
            bundleOf(GooglePayLauncherContract.EXTRA_RESULT to result)
        )
        val activityResult = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        intending(anyIntent()).respondWith(activityResult)

        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.moveToState(Lifecycle.State.CREATED)

        scenario.onActivity { activity ->
            PaymentConfiguration.init(activity, "pk_test")

            lateinit var launcher: GooglePayPaymentMethodLauncher

            when (integrationType) {
                LauncherIntegrationType.Activity -> {
                    launcher = GooglePayPaymentMethodLauncher(
                        activity = activity,
                        config = defaultConfig,
                        readyCallback = readyCallback,
                        resultCallback = resultCallback,
                    )
                }
                LauncherIntegrationType.Compose -> {
                    activity.setContent {
                        launcher = rememberGooglePayPaymentMethodLauncher(
                            config = defaultConfig,
                            readyCallback = readyCallback,
                            resultCallback = resultCallback,
                        )
                    }
                }
            }

            scenario.moveToState(Lifecycle.State.RESUMED)
            block(activity, launcher)
        }

        Espresso.onIdle()
    }

    verify(readyCallback).onReady(eq(isReady))

    if (expectResult) {
        verify(resultCallback).onResult(eq(result))
    }
}

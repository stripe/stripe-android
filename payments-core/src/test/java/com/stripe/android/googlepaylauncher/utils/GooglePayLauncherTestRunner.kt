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
import com.stripe.android.googlepaylauncher.GooglePayLauncher
import com.stripe.android.googlepaylauncher.GooglePayLauncherContract
import com.stripe.android.googlepaylauncher.rememberGooglePayLauncher
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private val defaultConfig = GooglePayLauncher.Config(
    environment = GooglePayEnvironment.Test,
    merchantCountryCode = "US",
    merchantName = "Widget Store",
)

internal fun runGooglePayLauncherTest(
    isReady: Boolean = true,
    result: GooglePayLauncher.Result = GooglePayLauncher.Result.Completed,
    integrationTypes: Array<LauncherIntegrationType> = LauncherIntegrationType.values(),
    expectResult: Boolean = true,
    block: (ComponentActivity, GooglePayLauncher) -> Unit,
) {
    for (integrationType in integrationTypes) {
        runGooglePayLauncherTest(
            integrationType = integrationType,
            isReady = isReady,
            result = result,
            verifyResult = expectResult,
            block = block,
        )
    }
}

private fun runGooglePayLauncherTest(
    integrationType: LauncherIntegrationType,
    isReady: Boolean,
    result: GooglePayLauncher.Result,
    verifyResult: Boolean,
    block: (ComponentActivity, GooglePayLauncher) -> Unit,
) {
    val readyCallback = mock<GooglePayLauncher.ReadyCallback>()
    val resultCallback = mock<GooglePayLauncher.ResultCallback>()

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

            lateinit var launcher: GooglePayLauncher

            when (integrationType) {
                LauncherIntegrationType.Activity -> {
                    launcher = GooglePayLauncher(
                        activity = activity,
                        config = defaultConfig,
                        readyCallback = readyCallback,
                        resultCallback = resultCallback,
                    )
                }
                LauncherIntegrationType.Compose -> {
                    activity.setContent {
                        launcher = rememberGooglePayLauncher(
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

    if (verifyResult) {
        verify(resultCallback).onResult(eq(result))
    }
}

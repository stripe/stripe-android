package com.stripe.android.googlepaylauncher.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import app.cash.turbine.Turbine
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayLauncher
import com.stripe.android.googlepaylauncher.GooglePayLauncherContract
import com.stripe.android.googlepaylauncher.rememberGooglePayLauncher
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private val defaultConfig = GooglePayLauncher.Config(
    environment = GooglePayEnvironment.Test,
    merchantCountryCode = "US",
    merchantName = "Widget Store",
)

internal fun runGooglePayLauncherTest(
    isReady: Boolean = true,
    result: GooglePayLauncher.Result = GooglePayLauncher.Result.Completed,
    integrationTypes: List<LauncherIntegrationType> = LauncherIntegrationType.entries,
    expectResult: Boolean = true,
    includeReadyCallback: Boolean,
    block: (ComponentActivity, GooglePayLauncher) -> Unit,
) {
    for (integrationType in integrationTypes) {
        runGooglePayLauncherTest(
            integrationType = integrationType,
            isReady = isReady,
            result = result,
            verifyResult = expectResult,
            includeReadyCallback = includeReadyCallback,
            block = block,
        )
    }
}

private fun runGooglePayLauncherTest(
    integrationType: LauncherIntegrationType,
    isReady: Boolean,
    result: GooglePayLauncher.Result,
    verifyResult: Boolean,
    includeReadyCallback: Boolean,
    block: (ComponentActivity, GooglePayLauncher) -> Unit,
) {
    val readyCalls = Turbine<Boolean>()
    val resultCalls = Turbine<GooglePayLauncher.Result>()
    val readyCallback = GooglePayLauncher.ReadyCallback(readyCalls::add)
    val resultCallback = GooglePayLauncher.ResultCallback(resultCalls::add)

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

        val scenario = ActivityScenario.launch(FragmentActivity::class.java)
        // Keep the host started when adding a Fragment so its state has not been saved.
        scenario.moveToState(
            if (integrationType == LauncherIntegrationType.BuilderFragment) {
                Lifecycle.State.STARTED
            } else {
                Lifecycle.State.CREATED
            }
        )

        scenario.onActivity { activity ->
            PaymentConfiguration.init(activity, "pk_test")

            lateinit var launcher: GooglePayLauncher

            createLauncher(
                integrationType = integrationType,
                activity = activity,
                readyCallback = readyCallback,
                resultCallback = resultCallback,
                includeReadyCallback = includeReadyCallback,
                onLauncherCreated = { launcher = it },
            )

            scenario.moveToState(Lifecycle.State.RESUMED)
            block(activity, launcher)
        }

        Espresso.onIdle()
        scenario.close()
    }

    if (includeReadyCallback) {
        assertThat(readyCalls.takeItem()).isEqualTo(isReady)
    }
    readyCalls.ensureAllEventsConsumed()

    if (verifyResult) {
        assertThat(resultCalls.takeItem()).isEqualTo(result)
    }
    resultCalls.ensureAllEventsConsumed()
}

@Suppress("DEPRECATION")
private fun createLauncher(
    integrationType: LauncherIntegrationType,
    activity: FragmentActivity,
    readyCallback: GooglePayLauncher.ReadyCallback,
    resultCallback: GooglePayLauncher.ResultCallback,
    includeReadyCallback: Boolean,
    onLauncherCreated: (GooglePayLauncher) -> Unit,
) {
    val builder = if (includeReadyCallback) {
        GooglePayLauncher.Builder(
            resultCallback = resultCallback,
            readyCallback = readyCallback,
        )
    } else {
        GooglePayLauncher.Builder(resultCallback)
    }

    when (integrationType) {
        LauncherIntegrationType.Activity -> {
            onLauncherCreated(
                GooglePayLauncher(
                    activity = activity,
                    config = defaultConfig,
                    readyCallback = readyCallback,
                    resultCallback = resultCallback,
                )
            )
        }
        LauncherIntegrationType.Compose -> {
            activity.setContent {
                onLauncherCreated(
                    rememberGooglePayLauncher(
                        config = defaultConfig,
                        readyCallback = readyCallback,
                        resultCallback = resultCallback,
                    )
                )
            }
        }
        LauncherIntegrationType.BuilderActivity -> {
            onLauncherCreated(builder.build(activity, defaultConfig))
        }
        LauncherIntegrationType.BuilderFragment -> {
            val fragment = GooglePayTestFragment(initializeInOnViewCreated = false) {
                onLauncherCreated(builder.build(it, defaultConfig))
            }
            activity.supportFragmentManager.beginTransaction()
                .add(fragment, "google_pay")
                .commitNow()
        }
        LauncherIntegrationType.BuilderCompose -> {
            activity.setContent {
                onLauncherCreated(builder.build(defaultConfig))
            }
        }
    }
}

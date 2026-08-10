package com.stripe.android.paymentsheet.utils

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataChangedCallback
import com.stripe.android.googlepaylauncher.injection.InternalGooglePayPaymentMethodLauncherFactory
import kotlinx.coroutines.test.runTest

internal class RecordingInternalGooglePayPaymentMethodLauncherFactory private constructor(
    private val launcher: InternalGooglePayPaymentMethodLauncher,
) : InternalGooglePayPaymentMethodLauncherFactory {
    private val calls = Turbine<Call>()

    override fun create(
        instanceId: String,
        lifecycleOwner: LifecycleOwner,
        onPaymentDataChangedCallback: GooglePayPaymentDataChangedCallback?,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    ): InternalGooglePayPaymentMethodLauncher {
        calls.add(
            Call(
                instanceId = instanceId,
                lifecycleOwner = lifecycleOwner,
                onPaymentDataChangedCallback = onPaymentDataChangedCallback,
                activityResultLauncher = activityResultLauncher,
            )
        )

        return launcher
    }

    data class Call(
        val instanceId: String,
        val lifecycleOwner: LifecycleOwner,
        val onPaymentDataChangedCallback: GooglePayPaymentDataChangedCallback?,
        val activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    )

    class Scenario(
        val factory: InternalGooglePayPaymentMethodLauncherFactory,
        val createGooglePayPaymentMethodLauncherCalls: ReceiveTurbine<Call>
    )

    companion object {
        fun test(
            launcher: InternalGooglePayPaymentMethodLauncher,
            test: suspend Scenario.() -> Unit
        ) = runTest {
            val factory = RecordingInternalGooglePayPaymentMethodLauncherFactory(launcher)

            test(
                Scenario(
                    factory = factory,
                    createGooglePayPaymentMethodLauncherCalls = factory.calls,
                )
            )

            factory.calls.ensureAllEventsConsumed()
        }

        fun noOp(
            launcher: InternalGooglePayPaymentMethodLauncher
        ): RecordingInternalGooglePayPaymentMethodLauncherFactory {
            return RecordingInternalGooglePayPaymentMethodLauncherFactory(launcher)
        }
    }
}

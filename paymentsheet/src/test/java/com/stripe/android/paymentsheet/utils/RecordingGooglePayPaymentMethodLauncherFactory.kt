package com.stripe.android.paymentsheet.utils

import androidx.activity.result.ActivityResultLauncher
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.CardBrandFilter
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest

internal class RecordingGooglePayPaymentMethodLauncherFactory(
    private val googlePayPaymentMethodLauncher: GooglePayPaymentMethodLauncher,
) : GooglePayPaymentMethodLauncherFactory {
    private val calls = Turbine<Call>()

    var config: GooglePayPaymentMethodLauncher.Config? = null
        private set

    override fun create(
        lifecycleScope: CoroutineScope,
        config: GooglePayPaymentMethodLauncher.Config,
        readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        skipReadyCheck: Boolean,
        cardBrandFilter: CardBrandFilter
    ): GooglePayPaymentMethodLauncher {
        calls.add(
            Call(
                config = config,
                activityResultLauncher = activityResultLauncher,
                skipReadyCheck = skipReadyCheck,
                cardBrandFilter = cardBrandFilter,
            )
        )

        this.config = config
        return googlePayPaymentMethodLauncher
    }

    data class Call(
        val config: GooglePayPaymentMethodLauncher.Config,
        val activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        val skipReadyCheck: Boolean,
        val cardBrandFilter: CardBrandFilter,
    )

    class Scenario(
        val factory: GooglePayPaymentMethodLauncherFactory,
        val createGooglePayPaymentMethodLauncherCalls: ReceiveTurbine<Call>
    )

    companion object {
        fun test(
            launcher: GooglePayPaymentMethodLauncher,
            test: suspend Scenario.() -> Unit
        ) = runTest {
            val factory = RecordingGooglePayPaymentMethodLauncherFactory(launcher)

            test(
                Scenario(
                    factory = factory,
                    createGooglePayPaymentMethodLauncherCalls = factory.calls,
                )
            )

            factory.calls.ensureAllEventsConsumed()
        }
    }
}

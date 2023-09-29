package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutModeSettingsDefinition :
    PlaygroundSettingDefinition<CheckoutModeSettingsDefinition.CheckoutMode>(
        key = "checkoutMode",
        displayName = "Checkout Mode",
    ) {
    override val defaultValue: CheckoutMode = CheckoutMode.Payment
    override val options: List<Option<CheckoutMode>> = listOf(
        Option("Pay", CheckoutMode.Payment),
        Option("P & S", CheckoutMode.PaymentWithSetup),
        Option("Setup", CheckoutMode.Setup),
    )

    override fun convertToValue(value: String): CheckoutMode {
        return CheckoutMode.values().firstOrNull { it.value == value } ?: defaultValue
    }

    override fun convertToString(value: CheckoutMode): String {
        return value.value
    }

    override fun configure(value: CheckoutMode, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.mode(value.value)
    }

    enum class CheckoutMode(val value: String) {
        Setup("setup") {
            override fun intentConfigurationMode(
                playgroundState: PlaygroundState
            ): PaymentSheet.IntentConfiguration.Mode {
                return PaymentSheet.IntentConfiguration.Mode.Setup(
                    currency = playgroundState.currencyCode,
                    setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                )
            }
        },
        Payment("payment") {
            override fun intentConfigurationMode(
                playgroundState: PlaygroundState
            ): PaymentSheet.IntentConfiguration.Mode {
                return PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = playgroundState.amount,
                    currency = playgroundState.currencyCode,
                )
            }
        },
        PaymentWithSetup("payment_with_setup") {
            override fun intentConfigurationMode(
                playgroundState: PlaygroundState
            ): PaymentSheet.IntentConfiguration.Mode {
                return PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = playgroundState.amount,
                    currency = playgroundState.currencyCode,
                    setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                )
            }
        };

        abstract fun intentConfigurationMode(
            playgroundState: PlaygroundState
        ): PaymentSheet.IntentConfiguration.Mode
    }
}

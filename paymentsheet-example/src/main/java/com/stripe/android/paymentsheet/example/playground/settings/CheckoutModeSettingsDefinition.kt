package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutModeSettingsDefinition :
    PlaygroundSettingDefinition<CheckoutModeSettingsDefinition.CheckoutMode>(
        key = "checkoutMode",
        displayName = "Checkout Mode",
    ) {
    override val defaultValue: CheckoutMode = CheckoutMode.PAYMENT
    override val options: List<Option<CheckoutMode>> = listOf(
        Option("Pay", CheckoutMode.PAYMENT),
        Option("P & S", CheckoutMode.PAYMENT_WITH_SETUP),
        Option("Setup", CheckoutMode.SETUP),
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
        SETUP("setup") {
            override fun intentConfigurationMode(
                playgroundState: PlaygroundState
            ): PaymentSheet.IntentConfiguration.Mode {
                return PaymentSheet.IntentConfiguration.Mode.Setup(
                    currency = playgroundState.currencyCode.value,
                    setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                )
            }
        },
        PAYMENT("payment") {
            override fun intentConfigurationMode(
                playgroundState: PlaygroundState
            ): PaymentSheet.IntentConfiguration.Mode {
                return PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = playgroundState.amount,
                    currency = playgroundState.currencyCode.value,
                )
            }
        },
        PAYMENT_WITH_SETUP("payment_with_setup") {
            override fun intentConfigurationMode(
                playgroundState: PlaygroundState
            ): PaymentSheet.IntentConfiguration.Mode {
                return PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = playgroundState.amount,
                    currency = playgroundState.currencyCode.value,
                    setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                )
            }
        };

        abstract fun intentConfigurationMode(
            playgroundState: PlaygroundState
        ): PaymentSheet.IntentConfiguration.Mode
    }
}

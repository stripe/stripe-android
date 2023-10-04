package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutModeSettingsDefinition :
    PlaygroundSettingDefinition<CheckoutMode>,
    PlaygroundSettingDefinition.Saveable<CheckoutMode> by EnumSaveable(
        key = "checkoutMode",
        values = CheckoutMode.values(),
        defaultValue = CheckoutMode.PAYMENT
    ),
    PlaygroundSettingDefinition.Displayable<CheckoutMode> {

    override val displayName: String = "Checkout Mode"
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<CheckoutMode>> =
        listOf(
            option("Pay", CheckoutMode.PAYMENT),
            option("P & S", CheckoutMode.PAYMENT_WITH_SETUP),
            option("Setup", CheckoutMode.SETUP),
        )

    override fun configure(value: CheckoutMode, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.mode(value.value)
    }
}

internal enum class CheckoutMode(override val value: String) : ValueEnum {
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

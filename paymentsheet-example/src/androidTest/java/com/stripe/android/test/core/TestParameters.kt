package com.stripe.android.test.core

import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.DefaultShippingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DelayedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings

/**
 * This is the data class that represents the parameters used to run the test.
 */
internal data class TestParameters(
    val paymentMethodCode: String,
    val saveCheckboxValue: Boolean,
    val saveForFutureUseCheckboxVisible: Boolean,
    val useBrowser: Browser? = null,
    val authorizationAction: AuthorizeAction? = null,
    val playgroundSettingsSnapshot: PlaygroundSettings.Snapshot = playgroundSettings().snapshot(),
) {
    fun copyPlaygroundSettings(block: (PlaygroundSettings) -> Unit): TestParameters {
        val playgroundSettings = playgroundSettingsSnapshot.playgroundSettings()
        block(playgroundSettings)
        return copy(playgroundSettingsSnapshot = playgroundSettings.snapshot())
    }

    companion object {
        fun create(
            paymentMethodCode: String,
            requiresBrowser: Boolean = true,
            playgroundSettingsBlock: (PlaygroundSettings) -> Unit = {},
        ): TestParameters {
            return TestParameters(
                paymentMethodCode = paymentMethodCode,
                saveCheckboxValue = false,
                saveForFutureUseCheckboxVisible = false,
                authorizationAction = AuthorizeAction.AuthorizePayment(requiresBrowser),
                playgroundSettingsSnapshot = playgroundSettings(playgroundSettingsBlock).snapshot()
            )
        }

        private fun playgroundSettings(block: (PlaygroundSettings) -> Unit = {}): PlaygroundSettings {
            val settings = PlaygroundSettings.createFromDefaults()
            settings[CustomerSettingsDefinition] = CustomerType.NEW
            settings[LinkSettingsDefinition] = false
            settings[CountrySettingsDefinition] = Country.GB
            settings[CurrencySettingsDefinition] = Currency.EUR
            settings[DefaultShippingAddressSettingsDefinition] = false
            settings[DelayedPaymentMethodsSettingsDefinition] = false
            settings[AutomaticPaymentMethodsSettingsDefinition] = false
            block(settings)
            return settings
        }
    }
}

/**
 * Indicates a specific browser is required for the test.
 */
enum class Browser {
    Chrome,
    Firefox
}

/**
 * Indicate the payment method for this test expects authorization and how the authorization
 * should be handled: complete, fail, cancel
 */
internal sealed interface AuthorizeAction {

    fun text(checkoutMode: CheckoutMode): String

    val requiresBrowser: Boolean
    val isConsideredDone: Boolean

    data object PollingSucceedsAfterDelay : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = "POLLING SUCCEEDS AFTER DELAY"

        override val requiresBrowser: Boolean = false
        override val isConsideredDone: Boolean = true
    }

    data object Authorize3ds2 : AuthorizeAction {
        override val requiresBrowser: Boolean = false

        override fun text(checkoutMode: CheckoutMode): String {
            return "COMPLETE"
        }
        override val isConsideredDone: Boolean = true
    }

    data class AuthorizePayment(
        override val requiresBrowser: Boolean = true,
    ) : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String {
            return if (checkoutMode == CheckoutMode.SETUP) {
                "AUTHORIZE TEST SETUP"
            } else {
                "AUTHORIZE TEST PAYMENT"
            }
        }
        override val isConsideredDone: Boolean = true
    }

    data object DisplayQrCode : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = "Display QR code"

        override val requiresBrowser: Boolean = false
        override val isConsideredDone: Boolean = true
    }

    data class Fail(val expectedError: String) : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = "FAIL TEST PAYMENT"

        override val requiresBrowser: Boolean = true
        override val isConsideredDone: Boolean = false
    }

    data object Cancel : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = ""
        override val requiresBrowser: Boolean = true
        override val isConsideredDone: Boolean = false
    }

    sealed interface Bacs : AuthorizeAction {
        data object Confirm : Bacs {
            override fun text(checkoutMode: CheckoutMode): String = "Confirm"
            override val requiresBrowser: Boolean = false
            override val isConsideredDone: Boolean = false
        }

        data object ModifyDetails : Bacs {
            override fun text(checkoutMode: CheckoutMode): String = "Modify details"
            override val requiresBrowser: Boolean = false
            override val isConsideredDone: Boolean = false
        }
    }
}

package com.stripe.android.test.core

import androidx.test.platform.app.InstrumentationRegistry
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
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.initializedLpmRepository

/**
 * This is the data class that represents the parameters used to run the test.
 */
internal data class TestParameters(
    val paymentMethod: LpmRepository.SupportedPaymentMethod,
    val saveCheckboxValue: Boolean,
    val saveForFutureUseCheckboxVisible: Boolean,
    val useBrowser: Browser? = null,
    val authorizationAction: AuthorizeAction? = null,
    val snapshotReturningCustomer: Boolean = false,
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
            playgroundSettingsBlock: (PlaygroundSettings) -> Unit = {},
        ): TestParameters {
            return create(
                paymentMethod = lpmRepository.fromCode(paymentMethodCode)!!,
                playgroundSettingsBlock = playgroundSettingsBlock,
            )
        }

        fun create(
            paymentMethod: LpmRepository.SupportedPaymentMethod,
            playgroundSettingsBlock: (PlaygroundSettings) -> Unit = {},
        ): TestParameters {
            return TestParameters(
                paymentMethod = paymentMethod,
                saveCheckboxValue = false,
                saveForFutureUseCheckboxVisible = false,
                authorizationAction = AuthorizeAction.AuthorizePayment,
                playgroundSettingsSnapshot = playgroundSettings(playgroundSettingsBlock).snapshot()
            )
        }

        fun playgroundSettings(block: (PlaygroundSettings) -> Unit = {}): PlaygroundSettings {
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

        private val lpmRepository = initializedLpmRepository(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
        )
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

    object PollingSucceedsAfterDelay : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = "POLLING SUCCEEDS AFTER DELAY"

        override val requiresBrowser: Boolean = false
    }

    object AuthorizePayment : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String {
            return if (checkoutMode == CheckoutMode.SETUP) {
                "AUTHORIZE TEST SETUP"
            } else {
                "AUTHORIZE TEST PAYMENT"
            }
        }

        override val requiresBrowser: Boolean = true
    }

    object DisplayQrCode : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = "Display QR code"

        override val requiresBrowser: Boolean = false
    }

    data class Fail(val expectedError: String) : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = "FAIL TEST PAYMENT"

        override val requiresBrowser: Boolean = true
    }

    object Cancel : AuthorizeAction {
        override fun text(checkoutMode: CheckoutMode): String = ""
        override val requiresBrowser: Boolean = true
    }

    sealed interface Bacs : AuthorizeAction {
        object Confirm : Bacs {
            override fun text(checkoutMode: CheckoutMode): String = "Confirm"
            override val requiresBrowser: Boolean = false
        }

        object ModifyDetails : Bacs {
            override fun text(checkoutMode: CheckoutMode): String = "Modify details"
            override val requiresBrowser: Boolean = false
        }
    }
}

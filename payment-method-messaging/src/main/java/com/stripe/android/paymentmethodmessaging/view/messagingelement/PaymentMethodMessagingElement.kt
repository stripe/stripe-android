package com.stripe.android.paymentmethodmessaging.view.messagingelement

import android.app.Application
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentmethodmessaging.view.injection.DaggerPaymentMethodMessagingComponent
import com.stripe.android.paymentmethodmessaging.view.messagingelement.PaymentMethodMessagingElement.Configuration
import com.stripe.android.uicore.StripeThemeDefaults
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

class PaymentMethodMessagingElement @Inject internal constructor(
    private val contentHelper: MessagingContentHelper
) {

    /**
     * Call this method to configure [PaymentMethodMessagingElement] or when the [Configuration] values
     * (amount, currency, etc.) change.
     */
    suspend fun configure(
        configuration: Configuration
    ): Result {
       return contentHelper.configure(configuration)
    }

    /**
     * A composable function that displays BNPL promo messaging.
     * @param appearance the custom [Appearance] for the element.
     */
    @Composable
    fun Content(appearance: Appearance = Appearance()) {
        contentHelper.Content(appearance)
    }

    companion object {
        fun create(application: Application): PaymentMethodMessagingElement {
            return DaggerPaymentMethodMessagingComponent.builder()
                .application(application)
                .build().element
        }
    }

    /**
     * The result of a [configure] call.
     */
    sealed interface Result {

        /**
         * The configuration succeeded and [Content] will display a view.
         */
        data object Succeeded : Result

        /**
         * The configuration succeeded but no content is available to display. (e.g. the amount is less than the
         * minimum for available payment methods).
         */
        data object NoContent : Result

        /**
         * The configure call failed e.g. due to network failure or because of an invalid [Configuration].
         */
        class Failed internal constructor(val error: Throwable) : Result
    }

    /**
     * Configuration for [PaymentMethodMessagingElement].
     */
    class Configuration {

        class State(
            val amount: Long,
            val currency: String,
            val locale: String?,
            val countryCode: String?,
            val paymentMethodTypes: List<PaymentMethod.Type>?,
        )

        private var amount: Long? = null
        private var currency: String? = null
        private var locale: String? = null
        private var countryCode: String? = null
        private var paymentMethodTypes: List<PaymentMethod.Type>? = null
        /**
         * Amount intended to be collected in the smallest currency unit (e.g. 100 cents to charge $1.00).
         */
        fun amount(amount: Long) = apply {
            this.amount = amount
        }

        /**
         * Three-letter ISO currency code.
         */
        fun currency(currency: String) = apply {
            this.currency = currency
        }

        /**
         * Language code used to localize message displayed in the element.
         */
        fun locale(locale: String) = apply {
            this.locale = locale
        }

        /**
         * Two letter country code of the customer's location. If not provided, country will be determined based
         * on IP Address.
         */
        fun countryCode(countryCode: String?) = apply {
            this.countryCode = countryCode
        }

        /**
         * The payment methods to request messaging for. Supported values are [PaymentMethod.Type.Affirm],
         * [PaymentMethod.Type.AfterpayClearpay], and [PaymentMethod.Type.Klarna]
         * If null, uses your preferences from the
         * [Stripe dashboard](https://dashboard.stripe.com/settings/payment_methods) to show the relevant payment
         * methods.
         * See [Dynamic payment methods])https://docs.stripe.com/payments/payment-methods/dynamic-payment-methods)
         * for more information.
         */
        fun paymentMethodTypes(paymentMethodTypes: List<PaymentMethod.Type>?) = apply {
            this.paymentMethodTypes = paymentMethodTypes
        }

        fun build(): State {
            // Implementation detail: validate that required params are not null, throw exception otherwise.
            return State(
                amount = amount!!,
                currency = currency!!,
                locale = locale ?: Locale.getDefault().language,
                countryCode = countryCode,
                paymentMethodTypes = paymentMethodTypes,
            )
        }
    }

    class Appearance {
        private var theme: Theme = Theme.LIGHT
        private var font: Font.State? = null
        private var colors: Colors.State = Colors().build()

        /**
         * The theme of the payment method icons to display.
         * See [our docs](https://docs.stripe.com/elements/payment-method-messaging#appearance) for more info.
         */
        fun theme(theme: Theme) = apply {
            this.theme = theme
        }

        /**
         * The font style of PaymentMethodMessagingElement text.
         * - Note: If null, [MaterialTheme.typography.body1] will be used.
         */
        fun font(font: Font) = apply {
            this.font = font.build()
        }

        /**
         * The colors of the PaymentMethodMessagingElement.
         */
        fun colors(colors: Colors) = apply {
            this.colors = colors.build()
        }

        internal class State(
            val theme: Theme,
            val font: Font.State?,
            val colors: Colors.State,
        )

        internal fun build() = State(
            theme = theme,
            font = font,
            colors = colors,
        )

        /**
         * The theme of the payment method icons to display.
         */
        enum class Theme {
            LIGHT,
            DARK,
            FLAT
        }

        class Font  {
            private var fontFamily: Int? = null
            private var fontSizeSp: Float? = null
            private var fontWeight: Int? = null
            private var letterSpacingSp: Float? = null

            /**
             * The font used in text. This should be a resource ID value.
             */
            fun fontFamily(@FontRes fontFamily: Int?) = apply {
                this.fontFamily = fontFamily
            }

            /**
             * The font size used for the text. This should represent an sp value.
             */
            fun fontSizeSp(fontSizeSp: Float?) = apply {
                this.fontSizeSp = fontSizeSp
            }

            /**
             * The font weight used for the text.
             */
            fun fontWeight(fontWeight: Int?) = apply {
                this.fontWeight = fontWeight
            }

            /**
             * The letter spacing used for the text. This should represent an sp value.
             */
            fun letterSpacingSp(letterSpacingSp: Float?) = apply {
                this.letterSpacingSp = letterSpacingSp
            }

            @Poko
            internal class State(
                @FontRes
                val fontFamily: Int? = null,
                val fontSizeSp: Float? = null,
                val fontWeight: Int? = null,
                val letterSpacingSp: Float? = null,
            )

            internal fun build() = State(
                fontFamily = fontFamily,
                fontSizeSp = fontSizeSp,
                fontWeight = fontWeight,
                letterSpacingSp = letterSpacingSp,
            )
        }

        class Colors {
            private var textColor: Int = StripeThemeDefaults.colorsLight.onComponent.toArgb()
            private var infoIconColor: Int = StripeThemeDefaults.colorsLight.subtitle.toArgb()

            /**
             * The color used for the message text.
             */
            fun textColor(@ColorInt textColor: Int) = apply {
                this.textColor = textColor
            }

            /**
             * The color used for the "i" information icon.
             */
            fun infoIconColor(@ColorInt infoIconColor: Int) = apply {
                this.infoIconColor = infoIconColor
            }

            @Poko
            internal class State(
                @ColorInt
                val textColor: Int,
                @ColorInt
                val infoIconColor: Int
            )

            internal fun build() = State(
                textColor = textColor,
                infoIconColor = infoIconColor
            )
        }
    }
}

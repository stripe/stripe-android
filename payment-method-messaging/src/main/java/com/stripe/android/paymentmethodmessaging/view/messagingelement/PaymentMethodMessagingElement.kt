package com.stripe.android.paymentmethodmessaging.view.messagingelement

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.StripeThemeDefaults
import kotlinx.coroutines.launch

class PaymentMethodMessagingElement internal constructor() {

    /**
     * Call this method to configure [PaymentMethodMessagingElement] or when the [Configuration] values
     * (amount, currency, etc.) change.
     */
    suspend fun configure(
        configuration: Configuration
    ): Result {
        // ...
    }

    /**
     * A composable function that displays BNPL promo messaging.
     */
    @Composable
    fun Content() {
        // ...
    }

    /**
     * The result of a [configure] call.
     */
    sealed interface Result {

        /**
         * The configuration succeeded and [Content] will display a view.
         */
        class Succeeded internal constructor() : Result

        /**
         * The configuration succeeded but no content is available to display. (e.g. the amount is less than the
         * minimum for available payment methods).
         */
        class NoContent internal constructor() : Result

        /**
         * The configure call failed e.g. due to network failure or because of an invalid [Configuration].
         */
        class Failed internal constructor(val error: Throwable) : Result
    }

    /**
     * Configuration for [PaymentMethodMessagingElement].
     */
    class Configuration internal constructor(
        val amount: Long,
        val currency: String,
        val locale: String,
        val publishableKey: String,
        val countryCode: String?,
        val paymentMethodList: List<PaymentMethod.Type>?,
        val referrer: String?,
        val appearance: Appearance?
    ) {

        class Builder {
            private var amount: Long? = null
            private var currency: String? = null
            private var locale: String? = null
            private var publishableKey: String? = null
            private var countryCode: String? = null
            private var paymentMethodList: List<PaymentMethod.Type>? = null
            private var referrer: String? = null
            private var appearance: Appearance = Appearance.Builder().build()

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
             * A publishable key from the Dashboard's [API keys](https://dashboard.stripe.com/apikeys) page.
             */
            fun publishableKey(publishableKey: String) = apply {
                this.publishableKey = publishableKey
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
             */
            fun paymentMethodList(paymentMethodList: List<PaymentMethod.Type>?) = apply {
                this.paymentMethodList = paymentMethodList
            }

            /**
             * TBD
             */
            fun referrer(referrer: String?) = apply {
                this.referrer = referrer
            }

            fun build(): Configuration {
                // Implementation detail: validate that required params are not null, throw exception otherwise.
                return Configuration(
                    amount = amount,
                    currency = currency,
                    locale = locale,
                    publishableKey = publishableKey,
                    countryCode = countryCode,
                    paymentMethodList = paymentMethodList,
                    referrer = referrer,
                    appearance = appearance
                )
            }
        }

        class Appearance internal constructor(
            val theme: Theme,
            val font: Font?,
            val colors: Colors,
        ) {

            /**
             * The theme of the payment method icons to display.
             */
            enum class Theme {
                LIGHT,
                DARK,
                FLAT
            }

            @Poko
            class Font(
                /**
                 * The font used in text. This should be a resource ID value.
                 */
                @FontRes
                val fontFamily: Int? = null,
                /**
                 * The font size used for the text. This should represent a sp value.
                 */
                val fontSizeSp: Float? = null,
                /**
                 * The font weight used for the text.
                 */
                val fontWeight: Int? = null,
                /**
                 * The letter spacing used for the text. This should represent a sp value.
                 */
                val letterSpacingSp: Float? = null,
            )

            @Poko
            class Colors(
                /**
                 * The color used for the message text.
                 */
                @ColorInt
                val textColor: Int,
                /**
                 * The color used for the "i" information icon.
                 */
                @ColorInt
                val infoIconColor: Int
            ) {
                internal companion object {
                    val colorsLight = Colors(
                        textColor = StripeThemeDefaults.colorsLight.onComponent.toArgb(),
                        infoIconColor = StripeThemeDefaults.colorsLight.subtitle.toArgb()
                    )
                }
            }

            class Builder {
                private var theme: Theme = Theme.LIGHT
                private var font: Font? = null
                private var colors: Colors = Colors.colorsLight

                /**
                 * The theme of the payment method icons to display.
                 * See [our docs](https://docs.stripe.com/elements/payment-method-messaging#appearance) for more info.
                 */
                fun theme(theme: Theme) = apply {
                    this.theme = theme
                }

                /**
                 * The font style of PMME text.
                 * - Note: If null, [MaterialTheme.typography.body1] will be used.
                 */
                fun font(font: Font) = apply {
                    this.font = font
                }

                /**
                 * The colors of the PMME.
                 */
                fun colorsLight(colors: Colors) = apply {
                    this.colors = colors
                }

                fun build() = Appearance(
                    theme = theme,
                    font = font,
                    colors = colors,
                )
            }
        }
    }
}


class CheckoutActivity: AppCompateActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        setContent {
            val pmme = rememberPaymentMethodMessagingElement()
            val configureResult: PaymentMethodMessagingElement.Result? by remember {
                mutableStateOf(null)
            }
            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                val configuration = PaymentMethodMessagingElement.Configuration.Builder()
                    .amount(5000L)
                    .currency("usd")
                    .locale("en")
                    .publishableKey("pk_test")
                    .build()

                pmme.configure(configuration)
            }

            PaymentMethodMessagingElementView(
                paymentMethodMessagingElement = pmme,
                result = configureResult
            )
        }
    }

    @Composable
    fun PaymentMethodMessagingElementView(
        paymentMethodMessagingElement: PaymentMethodMessagingElement,
        result: PaymentMethodMessagingElement.Result?
    ) {
        when (result) {
            is PaymentMethodMessagingElement.Result.Succeeded -> paymentMethodMessagingElement.Content()
            is PaymentMethodMessagingElement.Result.Failed -> showErrorMessage()
            is PaymentMethodMessagingElement.Result.NoContent -> {
                // NO-OP
            }
            null -> LoadingScreen()
        }
    }
}
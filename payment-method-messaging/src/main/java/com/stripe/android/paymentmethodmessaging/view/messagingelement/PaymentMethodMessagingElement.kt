package com.stripe.android.paymentmethodmessaging.view.messagingelement

import android.app.Application
import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentmethodmessaging.view.injection.DaggerPaymentMethodMessagingComponent
import com.stripe.android.uicore.StripeThemeDefaults
import dev.drewhamilton.poko.Poko
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
        configuration: Configuration,
    ): Result {
       return contentHelper.configure(configuration)
    }

    /**
     * A composable function that displays BNPL promo messaging.
     */
    @Composable
    fun Content() {
        contentHelper.Content()
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
//        val publishableKey: String,
        val countryCode: String?,
//        val stripeAccountId: String?,
        val paymentMethodList: List<PaymentMethod.Type>?,
        val referrer: String?,
        val appearance: Appearance?
    ) {

        class Builder {
            private var amount: Long? = null
            private var currency: String? = null
            private var locale: String? = null
//            private var publishableKey: String? = null
//            private var stripeAccountId: String? = null
            private var countryCode: String? = null
            private var paymentMethodList: List<PaymentMethod.Type>? = null
            private var referrer: String? = null
            private var appearance: Appearance? = Appearance.Builder().build()

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
//            fun publishableKey(publishableKey: String) = apply {
//                this.publishableKey = publishableKey
//            }

            /**
             * The optional stripe account ID, Connect platforms that create direct charges must identify the connected
             * account that renders the PaymentMethodMessagingElement.
             */
//            fun stripeAccountId(stripeAccountId: String?) = apply {
//                this.stripeAccountId = stripeAccountId
//            }

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
            fun paymentMethodList(paymentMethodList: List<PaymentMethod.Type>?) = apply {
                this.paymentMethodList = paymentMethodList
            }

            /**
             * The [Appearance] of the PaymentMethodMessagingElement.Content
             */
            fun appearance(appearance: Appearance?) = apply {
                this.appearance = appearance
            }

            fun build(): Configuration {
                // Implementation detail: validate that required params are not null, throw exception otherwise.
                return Configuration(
                    amount = amount!!,
                    currency = currency!!,
                    locale = locale ?: Locale.getDefault().language,
//                    publishableKey = publishableKey!!,
//                    stripeAccountId = stripeAccountId,
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
            class Font internal constructor(
                @FontRes
                val fontFamily: Int? = null,
                val fontSizeSp: Float? = null,
                val fontWeight: Int? = null,
                val letterSpacingSp: Float? = null,
            ) {
                class Builder {
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

                    fun build() = Font(
                        fontFamily = fontFamily,
                        fontSizeSp = fontSizeSp,
                        fontWeight = fontWeight,
                        letterSpacingSp = letterSpacingSp,
                    )
                }
            }

            @Poko
            class Colors internal constructor(
                @ColorInt
                val textColor: Int,
                @ColorInt
                val infoIconColor: Int
            ) {
                internal companion object {
                    val colorsLight = Builder().build()
                }

                class Builder {
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

                    fun build() = Colors(textColor, infoIconColor)
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


//class ViewModel : ViewModel() {
//    val element = PaymentMethodMessagingElement.create()
//
//    init {
//        viewModelScope.launch {
//            val appearance = PaymentMethodMessagingElement.Configuration.Appearance.Builder()
//                .colorsLight(
//                    PaymentMethodMessagingElement.Configuration.Appearance.Colors.Builder()
//                        .infoIconColor(myIconColor)
//                        .textColor(myTextColor)
//                        .build()
//                )
//                .build()
//            val configuration = PaymentMethodMessagingElement.Configuration.Builder()
//                .amount(5000L)
//                .currency("usd")
//                .locale("en")
//                .publishableKey("pk_test")
//                .build()
//            element.configure(configuration)
//        }
//    }
//}
//
//class Activity : AppCompatActivity {
//    private val viewModel = ViewModel()
//    fun onCreate() {
//        setContent {
//            viewModel.element.Content()
//        }
//    }
//}
//
//class CheckoutActivity: AppCompateActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
//        super.onCreate(savedInstanceState, persistentState)
//
//        setContent {
//            viewModel.element.Content()
//        }
//    }
//}

package com.stripe.android.paymentsheet

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.Fragment
import com.stripe.android.link.account.CookieStore
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.getRawValueFromDimenResource
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a bottom sheet to collect and process a customer's payment.
 */
class PaymentSheet internal constructor(
    private val paymentSheetLauncher: PaymentSheetLauncher
) {
    /**
     * Constructor to be used when launching [PaymentSheet] from a [ComponentActivity].
     *
     * @param activity The Activity that is presenting [PaymentSheet].
     * @param callback Called with the result of the payment after [PaymentSheet] is dismissed.
     */
    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(activity, callback)
    )

    /**
     * Constructor to be used when launching [PaymentSheet] from a [ComponentActivity] and intending
     * to create and optionally confirm the [PaymentIntent] or [SetupIntent] on your server.
     *
     * @param activity The Activity that is presenting [PaymentSheet].
     * @param createIntentCallback Called when the customer confirms the payment or setup.
     * @param paymentResultCallback Called with the result of the payment or setup after
     * [PaymentSheet] is dismissed.
     */
    constructor(
        activity: ComponentActivity,
        createIntentCallback: CreateIntentCallback,
        paymentResultCallback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(activity, paymentResultCallback)
    ) {
        IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
    }

    /**
     * Constructor to be used when launching the payment sheet from a [Fragment].
     *
     * @param fragment the Fragment that is presenting the payment sheet.
     * @param callback called with the result of the payment after the payment sheet is dismissed.
     */
    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(fragment, callback)
    )

    /**
     * Constructor to be used when launching [PaymentSheet] from a [Fragment] and intending to
     * create and optionally confirm the [PaymentIntent] or [SetupIntent] on your server.
     *
     * @param fragment The Fragment that is presenting [PaymentSheet].
     * @param createIntentCallback Called when the customer confirms the payment or setup.
     * @param paymentResultCallback Called with the result of the payment or setup after
     * [PaymentSheet] is dismissed.
     */
    constructor(
        fragment: Fragment,
        createIntentCallback: CreateIntentCallback,
        paymentResultCallback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(fragment, paymentResultCallback)
    ) {
        IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
    }

    /**
     * Present [PaymentSheet] to process a [PaymentIntent].
     *
     * If the [PaymentIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param paymentIntentClientSecret The client secret of the [PaymentIntent].
     * @param configuration An optional [PaymentSheet] configuration.
     */
    @JvmOverloads
    fun presentWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.present(
            mode = InitializationMode.PaymentIntent(paymentIntentClientSecret),
            configuration = configuration,
        )
    }

    /**
     * Present [PaymentSheet] to process a [SetupIntent].
     *
     * If the [SetupIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param setupIntentClientSecret The client secret of the [SetupIntent].
     * @param configuration An optional [PaymentSheet] configuration.
     */
    @JvmOverloads
    fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.present(
            mode = InitializationMode.SetupIntent(setupIntentClientSecret),
            configuration = configuration,
        )
    }

    /**
     * Present [PaymentSheet] with an [IntentConfiguration].
     *
     * @param intentConfiguration The [IntentConfiguration] to use.
     * @param configuration An optional [PaymentSheet] configuration.
     */
    @JvmOverloads
    fun presentWithIntentConfiguration(
        intentConfiguration: IntentConfiguration,
        configuration: Configuration? = null,
    ) {
        paymentSheetLauncher.present(
            mode = InitializationMode.DeferredIntent(intentConfiguration),
            configuration = configuration,
        )
    }

    internal sealed class InitializationMode : Parcelable {

        internal abstract fun validate()

        @Parcelize
        internal data class PaymentIntent(
            val clientSecret: String,
        ) : InitializationMode() {

            override fun validate() {
                PaymentIntentClientSecret(clientSecret).validate()
            }
        }

        @Parcelize
        internal data class SetupIntent(
            val clientSecret: String,
        ) : InitializationMode() {

            override fun validate() {
                SetupIntentClientSecret(clientSecret).validate()
            }
        }

        @Parcelize
        internal data class DeferredIntent(
            val intentConfiguration: IntentConfiguration,
        ) : InitializationMode() {

            override fun validate() {
                // Nothing to do here
            }
        }
    }

    /**
     * Contains information needed to render [PaymentSheet]. The values are used to calculate
     * the payment methods displayed and influence the UI.
     *
     * **Note**: The [PaymentIntent] or [SetupIntent] you create on your server must have the same
     * values or the payment/setup will fail.
     *
     * @param mode Whether [PaymentSheet] should present a payment or setup flow.
     * @param paymentMethodTypes The payment methods types to display. If empty, we dynamically
     * determine the payment method types using your [Stripe Dashboard settings](https://dashboard.stripe.com/settings/payment_methods).
     * @param onBehalfOf The account (if any) for which the funds of the intent are intended. See
     * [our docs](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-on_behalf_of) for more info.
     */
    @Parcelize
    class IntentConfiguration @JvmOverloads constructor(
        val mode: Mode,
        val paymentMethodTypes: List<String> = emptyList(),
        val onBehalfOf: String? = null,
    ) : Parcelable {

        /**
         * Contains information about the desired payment or setup flow.
         */
        sealed class Mode : Parcelable {

            internal abstract val setupFutureUse: SetupFutureUse?
            internal abstract val captureMethod: CaptureMethod?

            /**
             * Use this if your integration creates a [PaymentIntent].
             *
             * @param amount Amount intended to be collected in the smallest currency unit
             * (e.g. 100 cents to charge $1.00). Shown in Google Pay, Buy now pay later UIs, the Pay
             * button, and influences available payment methods. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-amount) for more info.
             * @param currency Three-letter ISO currency code. Filters out payment methods based on
             * supported currency. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-currency) for more info.
             * @param setupFutureUse Indicates that you intend to make future payments. See
             * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage) for more info.
             * @param captureMethod Controls when the funds will be captured from the customer's
             * account. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-capture_method) for more info.
             */
            @Parcelize
            class Payment @JvmOverloads constructor(
                val amount: Long,
                val currency: String,
                override val setupFutureUse: SetupFutureUse? = null,
                override val captureMethod: CaptureMethod = CaptureMethod.Automatic,
            ) : Mode()

            /**
             * Use this if your integration creates a [SetupIntent].
             *
             * @param currency Three-letter ISO currency code. Filters out payment methods based on
             * supported currency. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-currency) for more info.
             * @param setupFutureUse Indicates that you intend to make future payments. See
             * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage) for more info.
             */
            @Parcelize
            class Setup @JvmOverloads constructor(
                val currency: String? = null,
                override val setupFutureUse: SetupFutureUse = SetupFutureUse.OffSession,
            ) : Mode() {

                override val captureMethod: CaptureMethod?
                    get() = null
            }
        }

        /**
         * Indicates that you intend to make future payments with this [PaymentIntent]'s payment
         * method. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage) for more info.
         */
        enum class SetupFutureUse {

            /**
             * Use this if you intend to only reuse the payment method when your customer is present
             * in your checkout flow.
             */
            OnSession,

            /**
             * Use this if your customer may or may not be present in your checkout flow.
             */
            OffSession,
        }

        /**
         * Controls when the funds will be captured.
         *
         * See [docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-capture_method).
         */
        enum class CaptureMethod {

            /**
             * Stripe automatically captures funds when the customer authorizes the payment.
             */
            Automatic,

            /**
             * Stripe asynchronously captures funds when the customer authorizes the payment.
             * Recommended over [CaptureMethod.Automatic] due to improved latency, but may require
             * additional integration changes.
             */
            AutomaticAsync,

            /**
             * Place a hold on the funds when the customer authorizes the payment, but don't capture
             * the funds until later.
             *
             * **Note**: Not all payment methods support this.
             */
            Manual,
        }

        companion object {

            /**
             * Pass this as the client secret into [CreateIntentResult.Success] to force
             * [PaymentSheet] to show success, dismiss the sheet without confirming the intent, and
             * return [PaymentSheetResult.Completed].
             *
             * **Note**: If provided, the SDK performs no action to complete the payment or setup.
             * It doesn't confirm a [PaymentIntent] or [SetupIntent] or handle next actions. You
             * should only use this if your integration can't create a [PaymentIntent] or
             * [SetupIntent]. It is your responsibility to ensure that you only pass this value if
             * the payment or setup is successful.
             */
            @DelicatePaymentSheetApi
            const val COMPLETE_WITHOUT_CONFIRMING_INTENT =
                IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT
        }
    }

    /** Configuration for [PaymentSheet] **/
    @Parcelize
    data class Configuration @JvmOverloads constructor(
        /**
         * Your customer-facing business name.
         *
         * The default value is the name of your app.
         */
        val merchantDisplayName: String,

        /**
         * If set, the customer can select a previously saved payment method within PaymentSheet.
         */
        val customer: CustomerConfiguration? = null,

        /**
         * Configuration related to the Stripe Customer making a payment.
         *
         * If set, PaymentSheet displays Google Pay as a payment option.
         */
        val googlePay: GooglePayConfiguration? = null,

        /**
         * The color of the Pay or Add button. Keep in mind the text color is white.
         *
         * If set, PaymentSheet displays the button with this color.
         */
        @Deprecated(
            message = "Use Appearance parameter to customize primary button color",
            replaceWith = ReplaceWith(
                expression = "Appearance.colorsLight/colorsDark.primary " +
                    "or PrimaryButton.colorsLight/colorsDark.background"
            )
        )
        val primaryButtonColor: ColorStateList? = null,

        /**
         * The billing information for the customer.
         *
         * If set, PaymentSheet will pre-populate the form fields with the values provided.
         * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
         * these values will be attached to the payment method even if they are not collected by
         * the PaymentSheet UI.
         */
        val defaultBillingDetails: BillingDetails? = null,

        /**
         * The shipping information for the customer.
         * If set, PaymentSheet will pre-populate the form fields with the values provided.
         * This is used to display a "Billing address is same as shipping" checkbox if `defaultBillingDetails` is not provided.
         * If `name` and `line1` are populated, it's also [attached to the PaymentIntent](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping) during payment.
         */
        val shippingDetails: AddressDetails? = null,

        /**
         * If true, allows payment methods that do not move money at the end of the checkout.
         * Defaults to false.
         *
         * Some payment methods can't guarantee you will receive funds from your customer at the end
         * of the checkout because they take time to settle (eg. most bank debits, like SEPA or ACH)
         * or require customer action to complete (e.g. OXXO, Konbini, Boleto). If this is set to
         * true, make sure your integration listens to webhooks for notifications on whether a
         * payment has succeeded or not.
         *
         * See [payment-notification](https://stripe.com/docs/payments/payment-methods#payment-notification).
         */
        val allowsDelayedPaymentMethods: Boolean = false,

        /**
         * If `true`, allows payment methods that require a shipping address, like Afterpay and
         * Affirm. Defaults to `false`.
         *
         * Set this to `true` if you collect shipping addresses via [shippingDetails] or
         * [FlowController.shippingDetails].
         *
         * **Note**: PaymentSheet considers this property `true` if `shipping` details are present
         * on the PaymentIntent when PaymentSheet loads.
         */
        val allowsPaymentMethodsRequiringShippingAddress: Boolean = false,

        /**
         * Describes the appearance of Payment Sheet.
         */
        val appearance: Appearance = Appearance(),

        /**
         * The label to use for the primary button.
         *
         * If not set, Payment Sheet will display suitable default labels for payment and setup
         * intents.
         */
        val primaryButtonLabel: String? = null,

        /**
         * Describes how billing details should be collected.
         * All values default to `automatic`.
         * If `never` is used for a required field for the Payment Method used during checkout,
         * you **must** provide an appropriate value as part of [defaultBillingDetails].
         */
        val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration(),
    ) : Parcelable {
        /**
         * [Configuration] builder for cleaner object creation from Java.
         */
        @Suppress("TooManyFunctions")
        class Builder(
            private var merchantDisplayName: String
        ) {
            private var customer: CustomerConfiguration? = null
            private var googlePay: GooglePayConfiguration? = null
            private var primaryButtonColor: ColorStateList? = null
            private var defaultBillingDetails: BillingDetails? = null
            private var shippingDetails: AddressDetails? = null
            private var allowsDelayedPaymentMethods: Boolean = false
            private var allowsPaymentMethodsRequiringShippingAddress: Boolean = false
            private var appearance: Appearance = Appearance()
            private var billingDetailsCollectionConfiguration =
                BillingDetailsCollectionConfiguration()

            fun merchantDisplayName(merchantDisplayName: String) =
                apply { this.merchantDisplayName = merchantDisplayName }

            fun customer(customer: CustomerConfiguration?) =
                apply { this.customer = customer }

            fun googlePay(googlePay: GooglePayConfiguration?) =
                apply { this.googlePay = googlePay }

            @Deprecated(
                message = "Use Appearance parameter to customize primary button color",
                replaceWith = ReplaceWith(
                    expression = "Appearance.colorsLight/colorsDark.primary " +
                        "or PrimaryButton.colorsLight/colorsDark.background"
                )
            )
            fun primaryButtonColor(primaryButtonColor: ColorStateList?) =
                apply { this.primaryButtonColor = primaryButtonColor }

            fun defaultBillingDetails(defaultBillingDetails: BillingDetails?) =
                apply { this.defaultBillingDetails = defaultBillingDetails }

            fun shippingDetails(shippingDetails: AddressDetails?) =
                apply { this.shippingDetails = shippingDetails }

            fun allowsDelayedPaymentMethods(allowsDelayedPaymentMethods: Boolean) =
                apply { this.allowsDelayedPaymentMethods = allowsDelayedPaymentMethods }

            fun allowsPaymentMethodsRequiringShippingAddress(
                allowsPaymentMethodsRequiringShippingAddress: Boolean,
            ) = apply {
                this.allowsPaymentMethodsRequiringShippingAddress =
                    allowsPaymentMethodsRequiringShippingAddress
            }

            fun appearance(appearance: Appearance) =
                apply { this.appearance = appearance }

            fun billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
            ) = apply {
                this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
            }

            fun build() = Configuration(
                merchantDisplayName,
                customer,
                googlePay,
                primaryButtonColor,
                defaultBillingDetails,
                shippingDetails,
                allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress,
                appearance,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            )
        }
    }

    @Parcelize
    data class Appearance(
        /**
         * Describes the colors used while the system is in light mode.
         */
        val colorsLight: Colors = Colors.defaultLight,

        /**
         * Describes the colors used while the system is in dark mode.
         */
        val colorsDark: Colors = Colors.defaultDark,

        /**
         * Describes the appearance of shapes.
         */
        val shapes: Shapes = Shapes.default,

        /**
         * Describes the typography used for text.
         */
        val typography: Typography = Typography.default,

        /**
         * Describes the appearance of the primary button (e.g., the "Pay" button).
         */
        val primaryButton: PrimaryButton = PrimaryButton()
    ) : Parcelable {

        fun getColors(isDark: Boolean): Colors {
            return if (isDark) colorsDark else colorsLight
        }

        class Builder {
            private var colorsLight = Colors.defaultLight
            private var colorsDark = Colors.defaultDark
            private var shapes = Shapes.default
            private var typography = Typography.default
            private var primaryButton: PrimaryButton = PrimaryButton()

            fun colorsLight(colors: Colors) = apply {
                this.colorsLight = colors
            }

            fun colorsDark(colors: Colors) = apply {
                this.colorsDark = colors
            }

            fun shapes(shapes: Shapes) = apply {
                this.shapes = shapes
            }

            fun typography(typography: Typography) = apply {
                this.typography = typography
            }

            fun primaryButton(primaryButton: PrimaryButton) = apply {
                this.primaryButton = primaryButton
            }

            fun build(): Appearance {
                return Appearance(colorsLight, colorsDark, shapes, typography, primaryButton)
            }
        }
    }

    @Parcelize
    data class Colors(
        /**
         * A primary color used throughout PaymentSheet.
         */
        @ColorInt
        val primary: Int,

        /**
         * The color used for the surfaces (backgrounds) of PaymentSheet.
         */
        @ColorInt
        val surface: Int,

        /**
         * The color used for the background of inputs, tabs, and other components.
         */
        @ColorInt
        val component: Int,

        /**
         * The color used for borders of inputs, tabs, and other components.
         */
        @ColorInt
        val componentBorder: Int,

        /**
         * The color of the divider lines used inside inputs, tabs, and other components.
         */
        @ColorInt
        val componentDivider: Int,

        /**
         * The default color used for text and on other elements that live on components.
         */
        @ColorInt
        val onComponent: Int,

        /**
         * The color used for items appearing over the background in Payment Sheet.
         */
        @ColorInt
        val onSurface: Int,

        /**
         * The color used for text of secondary importance.
         * For example, this color is used for the label above input fields.
         */
        @ColorInt
        val subtitle: Int,

        /**
         * The color used for input placeholder text.
         */
        @ColorInt
        val placeholderText: Int,

        /**
         * The color used for icons in PaymentSheet, such as the close or back icons.
         */
        @ColorInt
        val appBarIcon: Int,

        /**
         * A color used to indicate errors or destructive actions in PaymentSheet.
         */
        @ColorInt
        val error: Int
    ) : Parcelable {
        constructor(
            primary: Color,
            surface: Color,
            component: Color,
            componentBorder: Color,
            componentDivider: Color,
            onComponent: Color,
            subtitle: Color,
            placeholderText: Color,
            onSurface: Color,
            appBarIcon: Color,
            error: Color
        ) : this(
            primary = primary.toArgb(),
            surface = surface.toArgb(),
            component = component.toArgb(),
            componentBorder = componentBorder.toArgb(),
            componentDivider = componentDivider.toArgb(),
            onComponent = onComponent.toArgb(),
            subtitle = subtitle.toArgb(),
            placeholderText = placeholderText.toArgb(),
            onSurface = onSurface.toArgb(),
            appBarIcon = appBarIcon.toArgb(),
            error = error.toArgb()
        )

        companion object {
            val defaultLight = Colors(
                primary = StripeThemeDefaults.colorsLight.materialColors.primary,
                surface = StripeThemeDefaults.colorsLight.materialColors.surface,
                component = StripeThemeDefaults.colorsLight.component,
                componentBorder = StripeThemeDefaults.colorsLight.componentBorder,
                componentDivider = StripeThemeDefaults.colorsLight.componentDivider,
                onComponent = StripeThemeDefaults.colorsLight.onComponent,
                subtitle = StripeThemeDefaults.colorsLight.subtitle,
                placeholderText = StripeThemeDefaults.colorsLight.placeholderText,
                onSurface = StripeThemeDefaults.colorsLight.materialColors.onSurface,
                appBarIcon = StripeThemeDefaults.colorsLight.appBarIcon,
                error = StripeThemeDefaults.colorsLight.materialColors.error
            )

            val defaultDark = Colors(
                primary = StripeThemeDefaults.colorsDark.materialColors.primary,
                surface = StripeThemeDefaults.colorsDark.materialColors.surface,
                component = StripeThemeDefaults.colorsDark.component,
                componentBorder = StripeThemeDefaults.colorsDark.componentBorder,
                componentDivider = StripeThemeDefaults.colorsDark.componentDivider,
                onComponent = StripeThemeDefaults.colorsDark.onComponent,
                subtitle = StripeThemeDefaults.colorsDark.subtitle,
                placeholderText = StripeThemeDefaults.colorsDark.placeholderText,
                onSurface = StripeThemeDefaults.colorsDark.materialColors.onSurface,
                appBarIcon = StripeThemeDefaults.colorsDark.appBarIcon,
                error = StripeThemeDefaults.colorsDark.materialColors.error
            )
        }
    }

    @Parcelize
    data class Shapes(
        /**
         * The corner radius used for tabs, inputs, buttons, and other components in PaymentSheet.
         */
        val cornerRadiusDp: Float,

        /**
         * The border used for inputs, tabs, and other components in PaymentSheet.
         */
        val borderStrokeWidthDp: Float
    ) : Parcelable {
        constructor(context: Context, cornerRadiusDp: Int, borderStrokeWidthDp: Int) : this(
            cornerRadiusDp = context.getRawValueFromDimenResource(cornerRadiusDp),
            borderStrokeWidthDp = context.getRawValueFromDimenResource(borderStrokeWidthDp)
        )

        companion object {
            val default = Shapes(
                cornerRadiusDp = StripeThemeDefaults.shapes.cornerRadius,
                borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth
            )
        }
    }

    @Parcelize
    data class Typography(
        /**
         * The scale factor for all fonts in PaymentSheet, the default value is 1.0.
         * When this value increases fonts will increase in size and decrease when this value is lowered.
         */
        val sizeScaleFactor: Float,

        /**
         * The font used in text. This should be a resource ID value.
         */
        @FontRes
        val fontResId: Int?
    ) : Parcelable {
        companion object {
            val default = Typography(
                sizeScaleFactor = StripeThemeDefaults.typography.fontSizeMultiplier,
                fontResId = StripeThemeDefaults.typography.fontFamily
            )
        }
    }

    @Parcelize
    data class PrimaryButton(
        /**
         * Describes the colors used while the system is in light mode.
         */
        val colorsLight: PrimaryButtonColors = PrimaryButtonColors.defaultLight,
        /**
         * Describes the colors used while the system is in dark mode.
         */
        val colorsDark: PrimaryButtonColors = PrimaryButtonColors.defaultDark,
        /**
         * Describes the shape of the primary button.
         */
        val shape: PrimaryButtonShape = PrimaryButtonShape(),
        /**
         * Describes the typography of the primary button.
         */
        val typography: PrimaryButtonTypography = PrimaryButtonTypography()
    ) : Parcelable

    @Parcelize
    data class PrimaryButtonColors(
        /**
         * The background color of the primary button.
         * Note: If 'null', {@link Colors#primary} is used.
         */
        @ColorInt
        val background: Int?,
        /**
         * The color of the text and icon in the primary button.
         */
        @ColorInt
        val onBackground: Int,
        /**
         * The border color of the primary button.
         */
        @ColorInt
        val border: Int
    ) : Parcelable {
        constructor(
            background: Color?,
            onBackground: Color,
            border: Color
        ) : this(
            background = background?.toArgb(),
            onBackground = onBackground.toArgb(),
            border = border.toArgb()
        )

        companion object {
            val defaultLight = PrimaryButtonColors(
                background = null,
                onBackground =
                StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
                border = StripeThemeDefaults.primaryButtonStyle.colorsLight.border.toArgb()
            )
            val defaultDark = PrimaryButtonColors(
                background = null,
                onBackground =
                StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
                border = StripeThemeDefaults.primaryButtonStyle.colorsDark.border.toArgb()
            )
        }
    }

    @Parcelize
    data class PrimaryButtonShape(
        /**
         * The corner radius of the primary button.
         * Note: If 'null', {@link Shapes#cornerRadiusDp} is used.
         */
        val cornerRadiusDp: Float? = null,
        /**
         * The border width of the primary button.
         * Note: If 'null', {@link Shapes#borderStrokeWidthDp} is used.
         */
        val borderStrokeWidthDp: Float? = null
    ) : Parcelable {
        constructor(
            context: Context,
            cornerRadiusDp: Int? = null,
            borderStrokeWidthDp: Int? = null
        ) : this(
            cornerRadiusDp = cornerRadiusDp?.let {
                context.getRawValueFromDimenResource(it)
            },
            borderStrokeWidthDp = borderStrokeWidthDp?.let {
                context.getRawValueFromDimenResource(it)
            }
        )
    }

    @Parcelize
    data class PrimaryButtonTypography(
        /**
         * The font used in the primary button.
         * Note: If 'null', Appearance.Typography.fontResId is used.
         */
        @FontRes
        val fontResId: Int? = null,

        /**
         * The font size in the primary button.
         * Note: If 'null', {@link Typography#sizeScaleFactor} is used.
         */
        val fontSizeSp: Float? = null
    ) : Parcelable {
        constructor(
            context: Context,
            fontResId: Int? = null,
            fontSizeSp: Int
        ) : this(
            fontResId = fontResId,
            fontSizeSp = context.getRawValueFromDimenResource(fontSizeSp)
        )
    }

    @Parcelize
    data class Address(
        /**
         * City, district, suburb, town, or village.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val city: String? = null,
        /**
         * Two-letter country code (ISO 3166-1 alpha-2).
         */
        val country: String? = null,
        /**
         * Address line 1 (e.g., street, PO Box, or company name).
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val line1: String? = null,
        /**
         * Address line 2 (e.g., apartment, suite, unit, or building).
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val line2: String? = null,
        /**
         * ZIP or postal code.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val postalCode: String? = null,
        /**
         * State, county, province, or region.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val state: String? = null
    ) : Parcelable {
        /**
         * [Address] builder for cleaner object creation from Java.
         */
        class Builder {
            private var city: String? = null
            private var country: String? = null
            private var line1: String? = null
            private var line2: String? = null
            private var postalCode: String? = null
            private var state: String? = null

            fun city(city: String?) = apply { this.city = city }
            fun country(country: String?) = apply { this.country = country }
            fun line1(line1: String?) = apply { this.line1 = line1 }
            fun line2(line2: String?) = apply { this.line2 = line2 }
            fun postalCode(postalCode: String?) = apply { this.postalCode = postalCode }
            fun state(state: String?) = apply { this.state = state }

            fun build() = Address(city, country, line1, line2, postalCode, state)
        }
    }

    @Parcelize
    data class BillingDetails(
        /**
         * The customer's billing address.
         */
        val address: Address? = null,
        /**
         * The customer's email.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val email: String? = null,
        /**
         * The customer's full name.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val name: String? = null,
        /**
         * The customer's phone number without formatting e.g. 5551234567
         */
        val phone: String? = null
    ) : Parcelable {
        /**
         * [BillingDetails] builder for cleaner object creation from Java.
         */
        class Builder {
            private var address: Address? = null
            private var email: String? = null
            private var name: String? = null
            private var phone: String? = null

            fun address(address: Address?) = apply { this.address = address }
            fun address(addressBuilder: Address.Builder) =
                apply { this.address = addressBuilder.build() }

            fun email(email: String?) = apply { this.email = email }
            fun name(name: String?) = apply { this.name = name }
            fun phone(phone: String?) = apply { this.phone = phone }

            fun build() = BillingDetails(address, email, name, phone)
        }
    }

    /**
     * Configuration for how billing details are collected during checkout.
     */
    @Parcelize
    data class BillingDetailsCollectionConfiguration(
        /**
         * How to collect the name field.
         */
        val name: CollectionMode = CollectionMode.Automatic,

        /**
         * How to collect the phone field.
         */
        val phone: CollectionMode = CollectionMode.Automatic,

        /**
         * How to collect the email field.
         */
        val email: CollectionMode = CollectionMode.Automatic,

        /**
         * How to collect the billing address.
         */
        val address: AddressCollectionMode = AddressCollectionMode.Automatic,

        /**
         * Whether the values included in [PaymentSheet.Configuration.defaultBillingDetails]
         * should be attached to the payment method, this includes fields that aren't displayed in the form.
         *
         * If `false` (the default), those values will only be used to prefill the corresponding fields in the form.
         */
        val attachDefaultsToPaymentMethod: Boolean = false,
    ) : Parcelable {

        /**
         * Billing details fields collection options.
         */
        enum class CollectionMode {
            /**
             * The field will be collected depending on the Payment Method's requirements.
             */
            Automatic,

            /**
             * The field will never be collected.
             * If this field is required by the Payment Method, you must provide it as part of `defaultBillingDetails`.
             */
            Never,

            /**
             * The field will always be collected, even if it isn't required for the Payment Method.
             */
            Always,
        }

        /**
         * Billing address collection options.
         */
        enum class AddressCollectionMode {
            /**
             * Only the fields required by the Payment Method will be collected, this may be none.
             */
            Automatic,

            /**
             * Address will never be collected.
             * If the Payment Method requires a billing address, you must provide it as part of `defaultBillingDetails`.
             */
            Never,

            /**
             * Collect the full billing address, regardless of the Payment Method requirements.
             */
            Full,
        }
    }

    @Parcelize
    data class CustomerConfiguration(
        /**
         * The identifier of the Stripe Customer object.
         * See [Stripe's documentation](https://stripe.com/docs/api/customers/object#customer_object-id).
         */
        val id: String,

        /**
         * A short-lived token that allows the SDK to access a Customer's payment methods.
         */
        val ephemeralKeySecret: String
    ) : Parcelable

    @Parcelize
    data class GooglePayConfiguration(
        /**
         * The Google Pay environment to use.
         *
         * See [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment) for more information.
         */
        val environment: Environment,
        /**
         * The two-letter ISO 3166 code of the country of your business, e.g. "US".
         * See your account's country value [here](https://dashboard.stripe.com/settings/account).
         */
        val countryCode: String,
        /**
         * The three-letter ISO 4217 alphabetic currency code, e.g. "USD" or "EUR".
         * Required in order to support Google Pay when processing a Setup Intent.
         */
        val currencyCode: String? = null
    ) : Parcelable {
        constructor(
            environment: Environment,
            countryCode: String
        ) : this(environment, countryCode, null)

        enum class Environment {
            Production,
            Test
        }
    }

    /**
     * A class that presents the individual steps of a payment sheet flow.
     */
    interface FlowController {

        var shippingDetails: AddressDetails?

        /**
         * Configure the FlowController to process a [PaymentIntent].
         *
         * @param paymentIntentClientSecret the client secret for the [PaymentIntent].
         * @param configuration optional [PaymentSheet] settings.
         * @param callback called with the result of configuring the FlowController.
         */
        fun configureWithPaymentIntent(
            paymentIntentClientSecret: String,
            configuration: Configuration? = null,
            callback: ConfigCallback
        )

        /**
         * Configure the FlowController to process a [SetupIntent].
         *
         * @param setupIntentClientSecret the client secret for the [SetupIntent].
         * @param configuration optional [PaymentSheet] settings.
         * @param callback called with the result of configuring the FlowController.
         */
        fun configureWithSetupIntent(
            setupIntentClientSecret: String,
            configuration: Configuration? = null,
            callback: ConfigCallback
        )

        /**
         * Configure the FlowController with an [IntentConfiguration].
         *
         * @param intentConfiguration The [IntentConfiguration] to use.
         * @param configuration An optional [PaymentSheet] configuration.
         * @param callback called with the result of configuring the FlowController.
         */
        fun configureWithIntentConfiguration(
            intentConfiguration: IntentConfiguration,
            configuration: Configuration? = null,
            callback: ConfigCallback
        )

        /**
         * Retrieve information about the customer's desired payment option.
         * You can use this to e.g. display the payment option in your UI.
         */
        fun getPaymentOption(): PaymentOption?

        /**
         * Present a sheet where the customer chooses how to pay, either by selecting an existing
         * payment method or adding a new one.
         * Call this when your "Select a payment method" button is tapped.
         */
        fun presentPaymentOptions()

        /**
         * Complete the payment or setup.
         */
        fun confirm()

        sealed class Result {
            object Success : Result()

            class Failure(
                val error: Throwable
            ) : Result()
        }

        fun interface ConfigCallback {
            fun onConfigured(
                success: Boolean,
                error: Throwable?
            )
        }

        companion object {

            /**
             * Create a [FlowController] that you configure with a client secret by calling
             * [configureWithPaymentIntent] or [configureWithSetupIntent].
             *
             * @param activity The Activity that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            fun create(
                activity: ComponentActivity,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                return FlowControllerFactory(
                    activity,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with an [IntentConfiguration] by calling
             * [configureWithIntentConfiguration].
             *
             * @param activity The Activity that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param createIntentCallback Called when the customer confirms the payment or setup.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            fun create(
                activity: ComponentActivity,
                paymentOptionCallback: PaymentOptionCallback,
                createIntentCallback: CreateIntentCallback,
                paymentResultCallback: PaymentSheetResultCallback,
            ): FlowController {
                IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
                return FlowControllerFactory(
                    activity,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with a client secret by calling
             * [configureWithPaymentIntent] or [configureWithSetupIntent].
             *
             * @param fragment The Fragment that is presenting [PaymentSheet].
             * @param paymentOptionCallback called when the customer's [PaymentOption] selection changes.
             * @param paymentResultCallback called when a [PaymentSheetResult] is available.
             */
            @JvmStatic
            fun create(
                fragment: Fragment,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                return FlowControllerFactory(
                    fragment,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with an [IntentConfiguration] by calling
             * [configureWithIntentConfiguration].
             *
             * @param fragment The Fragment that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param createIntentCallback Called when the customer confirms the payment or setup.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            fun create(
                fragment: Fragment,
                paymentOptionCallback: PaymentOptionCallback,
                createIntentCallback: CreateIntentCallback,
                paymentResultCallback: PaymentSheetResultCallback,
            ): FlowController {
                IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
                return FlowControllerFactory(
                    fragment,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }
        }
    }

    companion object {
        /**
         * Deletes all persisted authentication state associated with a customer.
         *
         * You must call this method when the user logs out from your app.
         * This will ensure that any persisted authentication state in PaymentSheet, such as
         * authentication cookies, is also cleared during logout.
         *
         * @param context the Application [Context].
         */
        fun resetCustomer(context: Context) {
            CookieStore(context).clear()
        }
    }
}

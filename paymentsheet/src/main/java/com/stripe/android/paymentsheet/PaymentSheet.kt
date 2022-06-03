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
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.ui.core.PaymentsThemeDefaults
import com.stripe.android.ui.core.getRawValueFromDimenResource
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a bottom sheet to collect and process a customer's payment.
 */
class PaymentSheet internal constructor(
    private val paymentSheetLauncher: PaymentSheetLauncher
) {
    /**
     * Constructor to be used when launching the payment sheet from an Activity.
     *
     * @param activity  the Activity that is presenting the payment sheet.
     * @param callback  called with the result of the payment after the payment sheet is dismissed.
     */
    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(activity, callback)
    )

    /**
     * Constructor to be used when launching the payment sheet from a Fragment.
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
     * Present the payment sheet to process a [PaymentIntent].
     * If the [PaymentIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param paymentIntentClientSecret the client secret for the [PaymentIntent].
     * @param configuration optional [PaymentSheet] settings.
     */
    @JvmOverloads
    fun presentWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

    /**
     * Present the payment sheet to process a [SetupIntent].
     * If the [SetupIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param setupIntentClientSecret the client secret for the [SetupIntent].
     * @param configuration optional [PaymentSheet] settings.
     */
    @JvmOverloads
    fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.presentWithSetupIntent(setupIntentClientSecret, configuration)
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
         */
        val defaultBillingDetails: BillingDetails? = null,

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
         * Describes the appearance of Payment Sheet.
         */
        val appearance: Appearance = Appearance()
    ) : Parcelable {
        /**
         * [Configuration] builder for cleaner object creation from Java.
         */
        class Builder(
            private var merchantDisplayName: String
        ) {
            private var customer: CustomerConfiguration? = null
            private var googlePay: GooglePayConfiguration? = null
            private var primaryButtonColor: ColorStateList? = null
            private var defaultBillingDetails: BillingDetails? = null
            private var allowsDelayedPaymentMethods: Boolean = false
            private var appearance: Appearance = Appearance()

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

            fun allowsDelayedPaymentMethods(allowsDelayedPaymentMethods: Boolean) =
                apply { this.allowsDelayedPaymentMethods = allowsDelayedPaymentMethods }

            fun appearance(appearance: Appearance) =
                apply { this.appearance = appearance }

            fun build() = Configuration(
                merchantDisplayName,
                customer,
                googlePay,
                primaryButtonColor,
                defaultBillingDetails,
                allowsDelayedPaymentMethods,
                appearance
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

            fun colorsLight(colors: Colors) = apply { this.colorsLight = colors }
            fun colorsDark(colors: Colors) = apply { this.colorsDark = colors }
            fun shapes(shapes: Shapes) = apply { this.shapes = shapes }
            fun typography(typography: Typography) = apply { this.typography = typography }
            fun primaryButton(primaryButton: PrimaryButton) =
                apply { this.primaryButton = primaryButton }
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
        val error: Int,
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
            error: Color,
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
                primary = PaymentsThemeDefaults.colorsLight.materialColors.primary,
                surface = PaymentsThemeDefaults.colorsLight.materialColors.surface,
                component = PaymentsThemeDefaults.colorsLight.component,
                componentBorder = PaymentsThemeDefaults.colorsLight.componentBorder,
                componentDivider = PaymentsThemeDefaults.colorsLight.componentDivider,
                onComponent = PaymentsThemeDefaults.colorsLight.onComponent,
                subtitle = PaymentsThemeDefaults.colorsLight.subtitle,
                placeholderText = PaymentsThemeDefaults.colorsLight.placeholderText,
                onSurface = PaymentsThemeDefaults.colorsLight.materialColors.onSurface,
                appBarIcon = PaymentsThemeDefaults.colorsLight.appBarIcon,
                error = PaymentsThemeDefaults.colorsLight.materialColors.error,
            )

            val defaultDark = Colors(
                primary = PaymentsThemeDefaults.colorsDark.materialColors.primary,
                surface = PaymentsThemeDefaults.colorsDark.materialColors.surface,
                component = PaymentsThemeDefaults.colorsDark.component,
                componentBorder = PaymentsThemeDefaults.colorsDark.componentBorder,
                componentDivider = PaymentsThemeDefaults.colorsDark.componentDivider,
                onComponent = PaymentsThemeDefaults.colorsDark.onComponent,
                subtitle = PaymentsThemeDefaults.colorsDark.subtitle,
                placeholderText = PaymentsThemeDefaults.colorsDark.placeholderText,
                onSurface = PaymentsThemeDefaults.colorsDark.materialColors.onSurface,
                appBarIcon = PaymentsThemeDefaults.colorsDark.appBarIcon,
                error = PaymentsThemeDefaults.colorsDark.materialColors.error,
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
        val borderStrokeWidthDp: Float,
    ) : Parcelable {
        constructor(context: Context, cornerRadiusDp: Int, borderStrokeWidthDp: Int) : this(
            cornerRadiusDp = context.getRawValueFromDimenResource(cornerRadiusDp),
            borderStrokeWidthDp = context.getRawValueFromDimenResource(borderStrokeWidthDp)
        )

        companion object {
            val default = Shapes(
                cornerRadiusDp = PaymentsThemeDefaults.shapes.cornerRadius,
                borderStrokeWidthDp = PaymentsThemeDefaults.shapes.borderStrokeWidth,
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
        val fontResId: Int?,
    ) : Parcelable {
        companion object {
            val default = Typography(
                sizeScaleFactor = PaymentsThemeDefaults.typography.fontSizeMultiplier,
                fontResId = PaymentsThemeDefaults.typography.fontFamily
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
        val border: Int,
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
                PaymentsThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
                border = PaymentsThemeDefaults.primaryButtonStyle.colorsLight.border.toArgb(),
            )
            val defaultDark = PrimaryButtonColors(
                background = null,
                onBackground =
                PaymentsThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
                border = PaymentsThemeDefaults.primaryButtonStyle.colorsDark.border.toArgb(),
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
             * Create the FlowController when launching the payment sheet from an Activity.
             *
             * @param activity  the Activity that is presenting the payment sheet.
             * @param paymentOptionCallback called when the customer's desired payment method
             *      changes.  Called in response to the [PaymentSheet#presentPaymentOptions()]
             * @param paymentResultCallback called when a [PaymentSheetResult] is available.
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
             * Create the FlowController when launching the payment sheet from a Fragment.
             *
             * @param fragment the Fragment that is presenting the payment sheet.
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
        }
    }
}

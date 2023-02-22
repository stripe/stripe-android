package com.stripe.android.paymentsheet

import android.content.Context
import android.content.res.ColorStateList
import android.os.Environment
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
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.getRawValueFromDimenResource
import kotlinx.parcelize.Parcelize
import java.util.Objects

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
    class Configuration @JvmOverloads constructor(
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
    ) : Parcelable {

        fun copy(
            merchantDisplayName: String = this.merchantDisplayName,
            customer: CustomerConfiguration? = this.customer,
            googlePay: GooglePayConfiguration? = this.googlePay,
            primaryButtonColor: ColorStateList? = this.primaryButtonColor,
            defaultBillingDetails: BillingDetails? = this.defaultBillingDetails,
            shippingDetails: AddressDetails? = this.shippingDetails,
            allowsDelayedPaymentMethods: Boolean = this.allowsDelayedPaymentMethods,
            allowsPaymentMethodsRequiringShippingAddress: Boolean = this.allowsPaymentMethodsRequiringShippingAddress,
            appearance: Appearance = this.appearance,
            primaryButtonLabel: String? = this.primaryButtonLabel,
        ): Configuration {
            return Configuration(
                merchantDisplayName = merchantDisplayName,
                customer = customer,
                googlePay = googlePay,
                primaryButtonColor = primaryButtonColor,
                defaultBillingDetails = defaultBillingDetails,
                shippingDetails = shippingDetails,
                allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
                appearance = appearance,
                primaryButtonLabel = primaryButtonLabel,
            )
        }

        override fun hashCode(): Int {
            return Objects.hash(
                merchantDisplayName,
                customer,
                googlePay,
                primaryButtonColor,
                defaultBillingDetails,
                shippingDetails,
                allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress,
                appearance,
                primaryButtonLabel,
            )
        }

        override fun equals(other: Any?): Boolean {
            return other is Configuration &&
                merchantDisplayName == other.merchantDisplayName &&
                customer == other.customer &&
                googlePay == other.googlePay &&
                primaryButtonColor == other.primaryButtonColor &&
                defaultBillingDetails == other.defaultBillingDetails &&
                shippingDetails == other.shippingDetails &&
                allowsDelayedPaymentMethods == other.allowsDelayedPaymentMethods &&
                allowsPaymentMethodsRequiringShippingAddress == other.allowsPaymentMethodsRequiringShippingAddress &&
                appearance == other.appearance &&
                primaryButtonLabel == other.primaryButtonLabel
        }

        override fun toString(): String {
            return "PaymentSheet.Configuration(" +
                "merchantDisplayName=$merchantDisplayName, " +
                "customer=$customer, " +
                "googlePay=$googlePay, " +
                "primaryButtonColor=$primaryButtonColor, " +
                "defaultBillingDetails=$defaultBillingDetails, " +
                "shippingDetails=$shippingDetails, " +
                "allowsDelayedPaymentMethods=$allowsDelayedPaymentMethods, " +
                "allowsPaymentMethodsRequiringShippingAddress=$allowsPaymentMethodsRequiringShippingAddress, " +
                "appearance=$appearance, " +
                "primaryButtonLabel=$primaryButtonLabel)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): String = merchantDisplayName

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): CustomerConfiguration? = customer

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): GooglePayConfiguration? = googlePay

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component4(): ColorStateList? = primaryButtonColor

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component5(): BillingDetails? = defaultBillingDetails

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component6(): AddressDetails? = shippingDetails

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component7(): Boolean = allowsDelayedPaymentMethods

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component8(): Boolean = allowsPaymentMethodsRequiringShippingAddress

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component9(): Appearance = appearance

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component10(): String? = primaryButtonLabel

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
            private var shippingDetails: AddressDetails? = null
            private var allowsDelayedPaymentMethods: Boolean = false
            private var allowsPaymentMethodsRequiringShippingAddress: Boolean = false
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

            fun build() = Configuration(
                merchantDisplayName,
                customer,
                googlePay,
                primaryButtonColor,
                defaultBillingDetails,
                shippingDetails,
                allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress,
                appearance
            )
        }
    }

    @Parcelize
    class Appearance(
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

        fun copy(
            colorsLight: Colors = this.colorsLight,
            colorsDark: Colors = this.colorsDark,
            shapes: Shapes = this.shapes,
            typography: Typography = this.typography,
            primaryButton: PrimaryButton = this.primaryButton,
        ): Appearance {
            return Appearance(colorsLight, colorsDark, shapes, typography, primaryButton)
        }

        override fun hashCode(): Int {
            return Objects.hash(colorsLight, colorsDark, shapes, typography, primaryButton)
        }

        override fun equals(other: Any?): Boolean {
            return other is Appearance &&
                colorsLight == other.colorsLight &&
                colorsDark == other.colorsDark &&
                shapes == other.shapes &&
                typography == other.typography &&
                primaryButton == other.primaryButton
        }

        override fun toString(): String {
            return "PaymentSheet.Appearance(" +
                "colorsLight=$colorsLight, " +
                "colorsDark=$colorsDark, " +
                "shapes=$shapes, " +
                "typography=$typography, " +
                "primaryButton=$primaryButton)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Colors = colorsLight

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): Colors = colorsDark

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): Shapes = shapes

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component4(): Typography = typography

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component5(): PrimaryButton = primaryButton

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
    class Colors(
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

        fun copy(
            primary: Int = this.primary,
            surface: Int = this.surface,
            component: Int = this.component,
            componentBorder: Int = this.componentBorder,
            componentDivider: Int = this.componentDivider,
            onComponent: Int = this.onComponent,
            onSurface: Int = this.onSurface,
            subtitle: Int = this.subtitle,
            placeholderText: Int = this.placeholderText,
            appBarIcon: Int = this.appBarIcon,
            error: Int = this.error,
        ): Colors {
            return Colors(
                primary = primary,
                surface = surface,
                component = component,
                componentBorder = componentBorder,
                componentDivider = componentDivider,
                onComponent = onComponent,
                onSurface = onSurface,
                subtitle = subtitle,
                placeholderText = placeholderText,
                appBarIcon = appBarIcon,
                error = error
            )
        }

        override fun hashCode(): Int {
            return Objects.hash(
                primary,
                surface,
                component,
                componentBorder,
                componentDivider,
                onComponent,
                subtitle,
                placeholderText,
                onSurface,
                appBarIcon,
                error,
            )
        }

        override fun equals(other: Any?): Boolean {
            return other is Colors &&
                primary == other.primary &&
                surface == other.surface &&
                component == other.component &&
                componentBorder == other.componentBorder &&
                componentDivider == other.componentDivider &&
                onComponent == other.onComponent &&
                subtitle == other.subtitle &&
                placeholderText == other.placeholderText &&
                onSurface == other.onSurface &&
                appBarIcon == other.appBarIcon &&
                error == other.error
        }

        override fun toString(): String {
            return "PaymentSheet.Colors(" +
                "primary=$primary, " +
                "surface=$surface, " +
                "component=$component, " +
                "componentBorder=$componentBorder, " +
                "componentDivider=$componentDivider, " +
                "onComponent=$onComponent, " +
                "subtitle=$subtitle, " +
                "placeholderText=$placeholderText, " +
                "onSurface=$onSurface, " +
                "appBarIcon=$appBarIcon, " +
                "error=$error)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Int = primary

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): Int = surface

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): Int = component

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component4(): Int = componentBorder

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component5(): Int = componentDivider

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component6(): Int = onComponent

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component7(): Int = onSurface

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component8(): Int = subtitle

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component9(): Int = placeholderText

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component10(): Int = appBarIcon

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component11(): Int = error

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
    class Shapes(
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

        fun copy(
            cornerRadiusDp: Float = this.cornerRadiusDp,
            borderStrokeWidthDp: Float = this.borderStrokeWidthDp,
        ): Shapes {
            return Shapes(cornerRadiusDp, borderStrokeWidthDp)
        }

        override fun hashCode(): Int {
            return Objects.hash(cornerRadiusDp, borderStrokeWidthDp)
        }

        override fun equals(other: Any?): Boolean {
            return other is Shapes &&
                cornerRadiusDp == other.cornerRadiusDp &&
                borderStrokeWidthDp == other.borderStrokeWidthDp
        }

        override fun toString(): String {
            return "PaymentSheet.Shapes(" +
                "cornerRadiusDp=$cornerRadiusDp, borderStrokeWidthDp=$borderStrokeWidthDp)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Float = cornerRadiusDp

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): Float = borderStrokeWidthDp

        companion object {
            val default = Shapes(
                cornerRadiusDp = StripeThemeDefaults.shapes.cornerRadius,
                borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth
            )
        }
    }

    @Parcelize
    class Typography(
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

        fun copy(
            sizeScaleFactor: Float = this.sizeScaleFactor,
            fontResId: Int? = this.fontResId,
        ): Typography {
            return Typography(sizeScaleFactor, fontResId)
        }

        override fun hashCode(): Int {
            return Objects.hash(sizeScaleFactor, fontResId)
        }

        override fun equals(other: Any?): Boolean {
            return other is Typography &&
                sizeScaleFactor == other.sizeScaleFactor &&
                fontResId == other.fontResId
        }

        override fun toString(): String {
            return "PaymentSheet.Typography(" +
                "sizeScaleFactor=$sizeScaleFactor, fontResId=$fontResId)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Float = sizeScaleFactor

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): Int? = fontResId

        companion object {
            val default = Typography(
                sizeScaleFactor = StripeThemeDefaults.typography.fontSizeMultiplier,
                fontResId = StripeThemeDefaults.typography.fontFamily
            )
        }
    }

    @Parcelize
    class PrimaryButton(
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
    ) : Parcelable {

        fun copy(
            colorsLight: PrimaryButtonColors = this.colorsLight,
            colorsDark: PrimaryButtonColors = this.colorsDark,
            shape: PrimaryButtonShape = this.shape,
            typography: PrimaryButtonTypography = this.typography,
        ): PrimaryButton {
            return PrimaryButton(colorsLight, colorsDark, shape, typography)
        }

        override fun hashCode(): Int {
            return Objects.hash(colorsLight, colorsDark, shape, typography)
        }

        override fun equals(other: Any?): Boolean {
            return other is PrimaryButton &&
                colorsLight == other.colorsLight &&
                colorsDark == other.colorsDark &&
                shape == other.shape &&
                typography == other.typography
        }

        override fun toString(): String {
            return "PaymentSheet.PrimaryButton(" +
                "colorsLight=$colorsLight, " +
                "colorsDark=$colorsDark, " +
                "shape=$shape, " +
                "typography=$typography)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): PrimaryButtonColors = colorsLight

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): PrimaryButtonColors = colorsDark

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): PrimaryButtonShape = shape

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component4(): PrimaryButtonTypography = typography
    }

    @Parcelize
    class PrimaryButtonColors(
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

        fun copy(
            background: Int? = this.background,
            onBackground: Int = this.onBackground,
            border: Int = this.border,
        ): PrimaryButtonColors {
            return PrimaryButtonColors(background, onBackground, border)
        }

        override fun hashCode(): Int {
            return Objects.hash(background, onBackground, border)
        }

        override fun equals(other: Any?): Boolean {
            return other is PrimaryButtonColors &&
                background == other.background &&
                onBackground == other.onBackground &&
                border == other.border
        }

        override fun toString(): String {
            return "PaymentSheet.PrimaryButtonColors(" +
                "background=$background, " +
                "onBackground=$onBackground, " +
                "border=$border)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Int? = background

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): Int = onBackground

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): Int = border

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
    class PrimaryButtonShape(
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

        fun copy(
            cornerRadiusDp: Float? = this.cornerRadiusDp,
            borderStrokeWidthDp: Float? = this.borderStrokeWidthDp,
        ): PrimaryButtonShape {
            return PrimaryButtonShape(cornerRadiusDp, borderStrokeWidthDp)
        }

        override fun hashCode(): Int {
            return Objects.hash(cornerRadiusDp, borderStrokeWidthDp)
        }

        override fun equals(other: Any?): Boolean {
            return other is PrimaryButtonShape &&
                cornerRadiusDp == other.cornerRadiusDp &&
                borderStrokeWidthDp == other.borderStrokeWidthDp
        }

        override fun toString(): String {
            return "PaymentSheet.PrimaryButtonShape(" +
                "cornerRadiusDp=$cornerRadiusDp, " +
                "borderStrokeWidthDp=$borderStrokeWidthDp)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Float? = cornerRadiusDp

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): Float? = borderStrokeWidthDp
    }

    @Parcelize
    class PrimaryButtonTypography(
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

        fun copy(
            fontResId: Int? = this.fontResId,
            fontSizeSp: Float? = this.fontSizeSp,
        ): PrimaryButtonTypography {
            return PrimaryButtonTypography(fontResId, fontSizeSp)
        }

        override fun hashCode(): Int {
            return Objects.hash(fontResId, fontSizeSp)
        }

        override fun equals(other: Any?): Boolean {
            return other is PrimaryButtonTypography &&
                fontResId == other.fontResId &&
                fontSizeSp == other.fontSizeSp
        }

        override fun toString(): String {
            return "PaymentSheet.PrimaryButtonTypography(" +
                "fontResId=$fontResId, fontSizeSp=$fontSizeSp)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Int? = fontResId

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): Float? = fontSizeSp
    }

    @Parcelize
    class Address(
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

        fun copy(
            city: String? = this.city,
            country: String? = this.country,
            line1: String? = this.line1,
            line2: String? = this.line2,
            postalCode: String? = this.postalCode,
            state: String? = this.state,
        ): Address {
            return Address(city, country, line1, line2, postalCode, state)
        }

        override fun hashCode(): Int {
            return Objects.hash(city, country, line1, line2, postalCode, state)
        }

        override fun equals(other: Any?): Boolean {
            return other is Address &&
                city == other.city &&
                country == other.country &&
                line1 == other.line1 &&
                line2 == other.line2 &&
                postalCode == other.postalCode &&
                state == other.state
        }

        override fun toString(): String {
            return "PaymentSheet.Address(" +
                "city=$city, " +
                "country=$country, " +
                "line1=$line1, " +
                "line2=$line2, " +
                "postalCode=$postalCode, " +
                "state=$state)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): String? = city

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): String? = country

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): String? = line1

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component4(): String? = line2

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component5(): String? = postalCode

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component6(): String? = state

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
    class BillingDetails(
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

        fun copy(
            address: Address? = this.address,
            email: String? = this.email,
            name: String? = this.name,
            phone: String? = this.phone,
        ): BillingDetails {
            return BillingDetails(address, email, name, phone)
        }

        override fun hashCode(): Int {
            return Objects.hash(address, email, name, phone)
        }

        override fun equals(other: Any?): Boolean {
            return other is BillingDetails &&
                address == other.address &&
                email == other.email &&
                name == other.name &&
                phone == other.phone
        }

        override fun toString(): String {
            return "PaymentSheet.BillingDetails(" +
                "address=$address, " +
                "email=$email, " +
                "name=$name, " +
                "phone=$phone)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Address? = address

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): String? = email

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): String? = name

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component4(): String? = phone

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
    class CustomerConfiguration(
        /**
         * The identifier of the Stripe Customer object.
         * See [Stripe's documentation](https://stripe.com/docs/api/customers/object#customer_object-id).
         */
        val id: String,

        /**
         * A short-lived token that allows the SDK to access a Customer's payment methods.
         */
        val ephemeralKeySecret: String,
    ) : Parcelable {

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): String = id

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): String = ephemeralKeySecret

        fun copy(
            id: String = this.id,
            ephemeralKeySecret: String = this.ephemeralKeySecret,
        ): CustomerConfiguration {
            return CustomerConfiguration(id, ephemeralKeySecret)
        }

        override fun hashCode(): Int {
            return Objects.hash(id, ephemeralKeySecret)
        }

        override fun equals(other: Any?): Boolean {
            return other is CustomerConfiguration &&
                id == other.id &&
                ephemeralKeySecret == other.ephemeralKeySecret
        }

        override fun toString(): String {
            return "CustomerConfiguration(id=$id, ephemeralKeySecret=$ephemeralKeySecret)"
        }
    }

    @Parcelize
    class GooglePayConfiguration(
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

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): Environment = environment

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): String = countryCode

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): String? = currencyCode

        fun copy(
            environment: Environment = this.environment,
            countryCode: String = this.countryCode,
            currencyCode: String? = this.currencyCode,
        ): GooglePayConfiguration {
            return GooglePayConfiguration(environment, countryCode, currencyCode)
        }

        override fun hashCode(): Int {
            return Objects.hash(environment, countryCode, currencyCode)
        }

        override fun equals(other: Any?): Boolean {
            return other is GooglePayConfiguration &&
                environment == other.environment &&
                countryCode == other.countryCode &&
                currencyCode == other.currencyCode
        }

        override fun toString(): String {
            return "GooglePayConfiguration(" +
                "environment=$environment, " +
                "countryCode=$countryCode, " +
                "currencyCode=$currencyCode)"
        }

        constructor(
            environment: Environment,
            countryCode: String,
        ) : this(environment, countryCode, null)

        enum class Environment {
            Production,
            Test,
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

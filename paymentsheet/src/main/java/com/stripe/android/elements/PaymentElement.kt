package com.stripe.android.elements

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

// TODO-codex: ensure this and its code always uses paymentElementPaymentMethodMetadata
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement @Inject internal constructor(
    private val contentHelper: EmbeddedContentHelper,
) {

    /**
     * A composable function that displays payment methods inline.
     *
     * It can present a sheet to collect more details or display saved payment methods.
     */
    @Composable
    fun Content() {
        val embeddedContent by contentHelper.embeddedContent.collectAsState()
        embeddedContent?.Content()
    }

    /**
     * Presents a sheet for the customer to select or manage their payment method.
     */
    fun present() {
        contentHelper.presentPaymentOptions()
    }

    /**
     * Describes how you handle row selections in [PaymentElement].
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract class RowSelectionBehavior internal constructor() {
        private object Default : RowSelectionBehavior()

        private class ImmediateAction(
            val didSelectPaymentOption: () -> Unit,
        ) : RowSelectionBehavior()

        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            /**
             * When a payment option is selected, the customer taps a button to continue or confirm payment.
             * This is the default recommended integration.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            fun default(): RowSelectionBehavior {
                return Default
            }

            /**
             * When a payment option is selected, [didSelectPaymentOption] is triggered.
             * You can implement this method to immediately perform an action, such as calling confirm.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            fun immediateAction(didSelectPaymentOption: () -> Unit): RowSelectionBehavior {
                return ImmediateAction(didSelectPaymentOption)
            }

            internal fun getImmediateAction(
                rowSelectionBehavior: RowSelectionBehavior,
            ): (() -> Unit)? {
                return (rowSelectionBehavior as? ImmediateAction)?.didSelectPaymentOption
            }
        }
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("TooManyFunctions")
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()
        private var paymentMethodLayout: PaymentMethodLayout = PaymentMethodLayout.Automatic
        private var opensCardScannerAutomatically: Boolean = false
        private var preferredNetworks: List<CardBrand> = emptyList()
        private var paymentMethodOrder: List<String> = emptyList()
        private var cardBrandAcceptance: CardBrandAcceptance = CardBrandAcceptance.All
        private var termsDisplay: Map<PaymentMethod.Type, TermsDisplay> = emptyMap()
        private var appearance: Appearance = Appearance()
        private var googlePayConfiguration: GooglePayConfiguration = GooglePayConfiguration()
        private var linkConfiguration: LinkConfiguration = LinkConfiguration()

        /**
         * Controls whether [Content] displays mandate text below the payment methods.
         * Defaults to `true`.
         *
         * When set to `false`, you must display
         * [CheckoutController.Session.PaymentOptionDisplayData.mandateText] to the customer yourself,
         * near your "Buy" button, to comply with regulations.
         */
        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        /**
         * Sets how billing details are collected when displaying payment methods.
         */
        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        /**
         * The layout of payment methods in the sheet. Defaults to [PaymentMethodLayout.Automatic].
         *
         * Note: Only used if you call [present].
         *
         * @see [PaymentMethodLayout] for the list of available layouts.
         */
        fun paymentMethodLayout(
            paymentMethodLayout: PaymentMethodLayout
        ): Configuration = apply {
            this.paymentMethodLayout = paymentMethodLayout
        }

        /**
         * Controls whether the card scanner opens automatically when the card entry form is shown.
         * Defaults to `false`.
         */
        fun opensCardScannerAutomatically(opensCardScannerAutomatically: Boolean): Configuration = apply {
            this.opensCardScannerAutomatically = opensCardScannerAutomatically
        }

        /**
         * A list of preferred networks that should be used to process payments made with a
         * co-branded card if your user hasn't selected a network themselves.
         *
         * The first preferred network that matches any available network will be used. If no
         * preferred network is applicable, Stripe will select the network.
         */
        fun preferredNetworks(preferredNetworks: List<CardBrand>): Configuration = apply {
            this.preferredNetworks = preferredNetworks
        }

        /**
         * Overrides the default order in which payment methods are displayed.
         *
         * Payment methods omitted from this list are automatically ordered by Stripe after the
         * specified methods. Invalid payment method types are ignored.
         */
        fun paymentMethodOrder(paymentMethodOrder: List<String>): Configuration = apply {
            this.paymentMethodOrder = paymentMethodOrder
        }

        /**
         * Specifies the card brands accepted by the payment element.
         *
         * By default, all card brands are accepted. This is a client-side setting and is not
         * currently supported in Link.
         */
        fun cardBrandAcceptance(cardBrandAcceptance: CardBrandAcceptance): Configuration = apply {
            this.cardBrandAcceptance = cardBrandAcceptance
        }

        /**
         * A map for specifying when legal agreements are displayed for each payment method type.
         * If the payment method is not specified in the list, the TermsDisplay value will default to automatic.
         */
        fun termsDisplay(termsDisplay: Map<PaymentMethod.Type, TermsDisplay>): Configuration = apply {
            this.termsDisplay = termsDisplay
        }

        /** Sets the visual appearance of the payment element. */
        fun appearance(appearance: Appearance): Configuration = apply {
            this.appearance = appearance
        }

        /**
         * Sets the Google Pay configuration for the payment element.
         */
        fun googlePayConfiguration(
            googlePayConfiguration: GooglePayConfiguration
        ): Configuration = apply {
            this.googlePayConfiguration = googlePayConfiguration
        }

        /**
         * Sets the Link configuration for the payment element.
         */
        fun linkConfiguration(configuration: LinkConfiguration): Configuration = apply {
            this.linkConfiguration = configuration
        }

        /**
         * Configuration related to Google Pay.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @CheckoutSessionPreview
        class GooglePayConfiguration {

            /**
             * Display configuration for Google Pay.
             */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class Display {
                /**
                 * Google Pay will be displayed when available.
                 */
                Automatic,

                /**
                 * Google Pay will never be displayed.
                 */
                Never,
            }

            private var display: Display = Display.Automatic
            private var label: String? = null
            private var buttonType: ButtonType = ButtonType.Pay
            private var additionalEnabledNetworks: List<String> = emptyList()

            /**
             * Sets the display configuration for Google Pay.
             *
             * @param display The display configuration for Google Pay.
             */
            fun display(display: Display): GooglePayConfiguration = apply {
                this.display = display
            }

            /**
             * Sets the label displayed with the amount.
             *
             * @param label An optional label to display with the amount. Google Pay may or may not display
             * this label depending on its own internal logic. Defaults to a generic label if none is
             * provided.
             */
            fun label(label: String): GooglePayConfiguration = apply {
                this.label = label
            }

            /**
             * Sets the Google Pay button type.
             *
             * @param buttonType The Google Pay button type to use. Set to "Pay" by default. See
             * [Google's documentation](https://developers.google.com/pay/api/android/reference/request-objects#ButtonOptions)
             * for more information on button types.
             */
            fun buttonType(buttonType: ButtonType): GooglePayConfiguration = apply {
                this.buttonType = buttonType
            }

            /**
             * Sets additional card networks that Google Pay can display.
             *
             * @param additionalEnabledNetworks An optional List<String> to signal GooglePay to
             * display additional enabled networks (e.g. 'INTERAC')
             */
            fun additionalEnabledNetworks(
                additionalEnabledNetworks: List<String>
            ): GooglePayConfiguration = apply {
                this.additionalEnabledNetworks = additionalEnabledNetworks
            }

            @CheckoutSessionPreview
            /**
             * Google Pay button type options
             *
             * See
             * [Google's documentation](https://developers.google.com/pay/api/android/reference/request-objects#ButtonOptions)
             * for more information on button types.
             */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class ButtonType {
                /**
                 * Displays "Buy with" alongside the Google Pay logo.
                 */
                Buy,

                /**
                 * Displays "Book with" alongside the Google Pay logo.
                 */
                Book,

                /**
                 * Displays "Checkout with" alongside the Google Pay logo.
                 */
                Checkout,

                /**
                 * Displays "Donate with" alongside the Google Pay logo.
                 */
                Donate,

                /**
                 * Displays "Order with" alongside the Google Pay logo.
                 */
                Order,

                /**
                 * Displays "Pay with" alongside the Google Pay logo.
                 */
                Pay,

                /**
                 * Displays "Subscribe with" alongside the Google Pay logo.
                 */
                Subscribe,

                /**
                 * Displays only the Google Pay logo.
                 */
                Plain
            }

            internal fun build(): CheckoutGooglePayConfiguration = CheckoutGooglePayConfiguration(
                display = display.asCheckout(),
                label = label,
                buttonType = buttonType.asCheckout(),
                additionalEnabledNetworks = additionalEnabledNetworks,
            )
        }

        /**
         * Builder for Link configuration used by the payment element.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class LinkConfiguration {
            private var display: Display = Display.Automatic

            /**
             * Sets when Link is displayed in the payment element.
             */
            fun display(display: Display): LinkConfiguration = apply {
                this.display = display
            }

            @Parcelize
            internal data class State(
                val display: Display,
            ) : Parcelable

            internal fun build(): State = State(
                display = display,
            )

            /**
             * Display configuration for Link.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class Display {
                /**
                 * Link is displayed when available.
                 */
                Automatic,

                /**
                 * Link is never displayed.
                 */
                Never,

                /**
                 * Link remains enabled but its button or row is hidden from the payment element UI.
                 */
                WalletButtonHidden,
            }
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
            val paymentMethodLayout: PaymentMethodLayout,
            val opensCardScannerAutomatically: Boolean,
            val preferredNetworks: List<CardBrand>,
            val paymentMethodOrder: List<String>,
            val cardBrandAcceptance: CardBrandAcceptance,
            val termsDisplay: Map<PaymentMethod.Type, TermsDisplay>,
            val appearance: Appearance.State,
            val googlePayConfiguration: CheckoutGooglePayConfiguration,
            val linkConfiguration: LinkConfiguration.State,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
            paymentMethodLayout = paymentMethodLayout,
            opensCardScannerAutomatically = opensCardScannerAutomatically,
            preferredNetworks = preferredNetworks,
            paymentMethodOrder = paymentMethodOrder,
            cardBrandAcceptance = cardBrandAcceptance,
            termsDisplay = termsDisplay,
            appearance = appearance.build(),
            googlePayConfiguration = googlePayConfiguration.build(),
            linkConfiguration = linkConfiguration.build(),
        )

        /** Options to allow or disallow card brands. */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        sealed class CardBrandAcceptance : Parcelable {
            /** Card brand categories that can be allowed or disallowed. */
            @Parcelize
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class BrandCategory : Parcelable {
                /** Visa branded cards. */
                Visa,

                /** Mastercard branded cards. */
                Mastercard,

                /** Amex branded cards. */
                Amex,

                /** Discover Global Network branded cards. */
                Discover,
            }

            @Parcelize
            internal data object All : CardBrandAcceptance()

            @Parcelize
            internal data class Allowed(
                val brands: List<BrandCategory>
            ) : CardBrandAcceptance()

            @Parcelize
            internal data class Disallowed(
                val brands: List<BrandCategory>
            ) : CardBrandAcceptance()

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            companion object {
                /** Accepts all card brands supported by Stripe. */
                @JvmStatic
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                fun all(): CardBrandAcceptance = All

                /** Accepts only the specified card brands. */
                @JvmStatic
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                fun allowed(brands: List<BrandCategory>): CardBrandAcceptance = Allowed(brands)

                /** Accepts all card brands except the specified ones. */
                @JvmStatic
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                fun disallowed(brands: List<BrandCategory>): CardBrandAcceptance = Disallowed(brands)
            }
        }

        /**
         * The layout of payment methods.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class PaymentMethodLayout {
            /**
             * Payment methods are arranged horizontally.
             * Users can swipe left or right to navigate through different payment methods.
             */
            Horizontal,

            /**
             * Payment methods are arranged vertically.
             * Users can scroll up or down to navigate through different payment methods.
             */
            Vertical,

            /**
             * This lets Stripe choose the best layout for payment methods.
             */
            Automatic
        }

        /** Appearance configuration for the Payment Element. */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("TooManyFunctions")
        class Appearance {
            private var colorsLight = Colors.light()
            private var colorsDark = Colors.dark()
            private var themeMode = ThemeMode.Automatic
            private var primaryButton = PrimaryButton()
            private var formInsetValues = Insets.defaultFormInsetValues

            /** Sets the colors used in light mode. */
            fun colorsLight(colors: Colors): Appearance = apply { colorsLight = colors }

            /** Sets the colors used in dark mode. */
            fun colorsDark(colors: Colors): Appearance = apply { colorsDark = colors }

            /** Sets the color mode used by the Payment Element. */
            fun themeMode(themeMode: ThemeMode): Appearance = apply { this.themeMode = themeMode }

            /** Sets the appearance of the primary button. */
            fun primaryButton(primaryButton: PrimaryButton): Appearance = apply { this.primaryButton = primaryButton }

            /** Sets the insets used by forms. */
            fun formInsetValues(insets: Insets): Appearance = apply { formInsetValues = insets }

            @Parcelize
            internal data class State(
                val colorsLight: Colors.State,
                val colorsDark: Colors.State,
                val themeMode: ThemeMode,
                val primaryButton: PrimaryButton.State,
                val formInsetValues: Insets.State,
            ) : Parcelable

            internal fun build(): State = State(
                colorsLight = colorsLight.build(),
                colorsDark = colorsDark.build(),
                themeMode = themeMode,
                primaryButton = primaryButton.build(),
                formInsetValues = formInsetValues.build(),
            )

            /** Colors used to render the Payment Element. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @Suppress("TooManyFunctions")
            class Colors private constructor(
                @ColorInt private var primary: Int,
                @ColorInt private var surface: Int,
                @ColorInt private var component: Int,
                @ColorInt private var componentBorder: Int,
                @ColorInt private var componentDivider: Int,
                @ColorInt private var onComponent: Int,
                @ColorInt private var subtitle: Int,
                @ColorInt private var placeholderText: Int,
                @ColorInt private var onSurface: Int,
                @ColorInt private var appBarIcon: Int,
                @ColorInt private var error: Int,
            ) {
                /** Sets the primary color. */
                fun primary(@ColorInt c: Int): Colors = apply { primary = c }

                /** Sets the primary color. */
                fun primary(c: Color): Colors = apply { primary = c.toArgb() }

                /** Sets the surface background color. */
                fun surface(@ColorInt c: Int): Colors = apply { surface = c }

                /** Sets the surface background color. */
                fun surface(c: Color): Colors = apply { surface = c.toArgb() }

                /** Sets the component background color. */
                fun component(@ColorInt c: Int): Colors = apply { component = c }

                /** Sets the component background color. */
                fun component(c: Color): Colors = apply { component = c.toArgb() }

                /** Sets the component border color. */
                fun componentBorder(@ColorInt c: Int): Colors = apply { componentBorder = c }

                /** Sets the component border color. */
                fun componentBorder(c: Color): Colors = apply { componentBorder = c.toArgb() }

                /** Sets the component divider color. */
                fun componentDivider(@ColorInt c: Int): Colors = apply { componentDivider = c }

                /** Sets the component divider color. */
                fun componentDivider(c: Color): Colors = apply { componentDivider = c.toArgb() }

                /** Sets the color of content displayed on components. */
                fun onComponent(@ColorInt c: Int): Colors = apply { onComponent = c }

                /** Sets the color of content displayed on components. */
                fun onComponent(c: Color): Colors = apply { onComponent = c.toArgb() }

                /** Sets the secondary text color. */
                fun subtitle(@ColorInt c: Int): Colors = apply { subtitle = c }

                /** Sets the secondary text color. */
                fun subtitle(c: Color): Colors = apply { subtitle = c.toArgb() }

                /** Sets the placeholder-text color. */
                fun placeholderText(@ColorInt c: Int): Colors = apply { placeholderText = c }

                /** Sets the placeholder-text color. */
                fun placeholderText(c: Color): Colors = apply { placeholderText = c.toArgb() }

                /** Sets the color of content displayed on surfaces. */
                fun onSurface(@ColorInt c: Int): Colors = apply { onSurface = c }

                /** Sets the color of content displayed on surfaces. */
                fun onSurface(c: Color): Colors = apply { onSurface = c.toArgb() }

                /** Sets the app-bar icon color. */
                fun appBarIcon(@ColorInt c: Int): Colors = apply { appBarIcon = c }

                /** Sets the app-bar icon color. */
                fun appBarIcon(c: Color): Colors = apply { appBarIcon = c.toArgb() }

                /** Sets the error color. */
                fun error(@ColorInt c: Int): Colors = apply { error = c }

                /** Sets the error color. */
                fun error(c: Color): Colors = apply { error = c.toArgb() }

                @Parcelize
                internal data class State(
                    @ColorInt val primary: Int,
                    @ColorInt val surface: Int,
                    @ColorInt val component: Int,
                    @ColorInt val componentBorder: Int,
                    @ColorInt val componentDivider: Int,
                    @ColorInt val onComponent: Int,
                    @ColorInt val subtitle: Int,
                    @ColorInt val placeholderText: Int,
                    @ColorInt val onSurface: Int,
                    @ColorInt val appBarIcon: Int,
                    @ColorInt val error: Int,
                ) : Parcelable

                internal fun build(): State = State(
                    primary = primary,
                    surface = surface,
                    component = component,
                    componentBorder = componentBorder,
                    componentDivider = componentDivider,
                    onComponent = onComponent,
                    subtitle = subtitle,
                    placeholderText = placeholderText,
                    onSurface = onSurface,
                    appBarIcon = appBarIcon,
                    error = error,
                )

                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                companion object {
                    /** Creates colors initialized with the default light-mode values. */
                    fun light(): Colors = defaults(StripeThemeDefaults.colorsLight)

                    /** Creates colors initialized with the default dark-mode values. */
                    fun dark(): Colors = defaults(StripeThemeDefaults.colorsDark)
                    private fun defaults(colors: com.stripe.android.uicore.StripeColors): Colors = Colors(
                        primary = colors.materialColors.primary.toArgb(),
                        surface = colors.materialColors.surface.toArgb(),
                        component = colors.component.toArgb(),
                        componentBorder = colors.componentBorder.toArgb(),
                        componentDivider = colors.componentDivider.toArgb(),
                        onComponent = colors.onComponent.toArgb(),
                        subtitle = colors.subtitle.toArgb(),
                        placeholderText = colors.placeholderText.toArgb(),
                        onSurface = colors.materialColors.onSurface.toArgb(),
                        appBarIcon = colors.appBarIcon.toArgb(),
                        error = colors.materialColors.error.toArgb(),
                    )
                }
            }

            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            /** Color modes available for the Payment Element. */
            enum class ThemeMode {
                /** Follow the system's light or dark mode. */
                Automatic,

                /** Always use light-mode colors. */
                AlwaysLight,

                /** Always use dark-mode colors. */
                AlwaysDark,
            }

            /** Insets used to position Payment Element content, in dp. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Insets(
                private val startDp: Float,
                private val topDp: Float,
                private val endDp: Float,
                private val bottomDp: Float,
            ) {
                /** Creates equal start/end and top/bottom insets, in dp. */
                constructor(horizontalDp: Float, verticalDp: Float) : this(
                    horizontalDp,
                    verticalDp,
                    horizontalDp,
                    verticalDp,
                )

                @Parcelize
                internal data class State(
                    val startDp: Float,
                    val topDp: Float,
                    val endDp: Float,
                    val bottomDp: Float,
                ) : Parcelable

                internal fun build(): State = State(startDp, topDp, endDp, bottomDp)

                internal companion object {
                    val defaultFormInsetValues = Insets(20f, 0f, 20f, 40f)
                }
            }

            /** Configures the appearance of the primary button. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class PrimaryButton {
                private var colorsLight = Colors.light()
                private var colorsDark = Colors.dark()
                private var shape = Shape()
                private var typography = Typography()

                /** Sets the primary-button colors used in light mode. */
                fun colorsLight(value: Colors): PrimaryButton = apply { colorsLight = value }

                /** Sets the primary-button colors used in dark mode. */
                fun colorsDark(value: Colors): PrimaryButton = apply { colorsDark = value }

                /** Sets the primary-button shape. */
                fun shape(value: Shape): PrimaryButton = apply { shape = value }

                /** Sets the primary-button typography. */
                fun typography(value: Typography): PrimaryButton = apply { typography = value }

                @Parcelize
                internal data class State(
                    val colorsLight: Colors.State,
                    val colorsDark: Colors.State,
                    val shape: Shape.State,
                    val typography: Typography.State,
                ) : Parcelable

                internal fun build(): State = State(
                    colorsLight.build(),
                    colorsDark.build(),
                    shape.build(),
                    typography.build(),
                )

                /** Colors used to render the primary button. */
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @Suppress("TooManyFunctions")
                class Colors private constructor(
                    @ColorInt private var background: Int?,
                    @ColorInt private var onBackground: Int,
                    @ColorInt private var border: Int,
                    @ColorInt private var successBackgroundColor: Int,
                    @ColorInt private var onSuccessBackgroundColor: Int,
                ) {
                    /** Sets the primary-button background color. */
                    fun background(@ColorInt value: Int?): Colors = apply { background = value }

                    /** Sets the primary-button background color. */
                    fun background(value: Color?): Colors = apply { background = value?.toArgb() }

                    /** Sets the content color used on the primary button. */
                    fun onBackground(@ColorInt value: Int): Colors = apply {
                        onBackground = value
                        onSuccessBackgroundColor = value
                    }

                    /** Sets the content color used on the primary button. */
                    fun onBackground(value: Color): Colors = onBackground(value.toArgb())

                    /** Sets the primary-button border color. */
                    fun border(@ColorInt value: Int): Colors = apply { border = value }

                    /** Sets the primary-button border color. */
                    fun border(value: Color): Colors = border(value.toArgb())

                    /** Sets the primary-button success background color. */
                    fun successBackgroundColor(@ColorInt value: Int): Colors = apply { successBackgroundColor = value }

                    /** Sets the primary-button success background color. */
                    fun successBackgroundColor(value: Color): Colors = successBackgroundColor(value.toArgb())

                    /** Sets the content color used on the success background. */
                    fun onSuccessBackgroundColor(@ColorInt value: Int): Colors = apply {
                        onSuccessBackgroundColor = value
                    }

                    /** Sets the content color used on the success background. */
                    fun onSuccessBackgroundColor(value: Color): Colors = onSuccessBackgroundColor(value.toArgb())

                    @Parcelize
                    internal data class State(
                        @ColorInt val background: Int?,
                        @ColorInt val onBackground: Int,
                        @ColorInt val border: Int,
                        @ColorInt val successBackgroundColor: Int,
                        @ColorInt val onSuccessBackgroundColor: Int,
                    ) : Parcelable

                    internal fun build(): State = State(
                        background,
                        onBackground,
                        border,
                        successBackgroundColor,
                        onSuccessBackgroundColor,
                    )

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    companion object {
                        fun light(): Colors = defaults(StripeThemeDefaults.primaryButtonStyle.colorsLight)
                        fun dark(): Colors = defaults(StripeThemeDefaults.primaryButtonStyle.colorsDark)
                        private fun defaults(colors: com.stripe.android.uicore.PrimaryButtonColors): Colors = Colors(
                            background = null,
                            onBackground = colors.onBackground.toArgb(),
                            border = colors.border.toArgb(),
                            successBackgroundColor = colors.successBackground.toArgb(),
                            onSuccessBackgroundColor = colors.onSuccessBackground.toArgb(),
                        )
                    }
                }

                /** Configures the shape of the primary button. */
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                class Shape {
                    private var cornerRadiusDp: Float? = null
                    private var borderStrokeWidthDp: Float? = null
                    private var heightDp: Float? = null

                    /** Sets the corner radius, in dp. */
                    fun cornerRadiusDp(value: Float?): Shape = apply { cornerRadiusDp = value }

                    /** Sets the border stroke width, in dp. */
                    fun borderStrokeWidthDp(value: Float?): Shape = apply { borderStrokeWidthDp = value }

                    /** Sets the button height, in dp. */
                    fun heightDp(value: Float?): Shape = apply { heightDp = value }

                    @Parcelize
                    internal data class State(
                        val cornerRadiusDp: Float?,
                        val borderStrokeWidthDp: Float?,
                        val heightDp: Float?,
                    ) : Parcelable

                    internal fun build(): State = State(cornerRadiusDp, borderStrokeWidthDp, heightDp)
                }

                /** Configures the typography of the primary button. */
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                class Typography {
                    @FontRes
                    private var fontResId: Int? = null
                    private var fontSizeSp: Float? = null

                    /** Sets the font resource. */
                    fun fontResId(@FontRes value: Int?): Typography = apply { fontResId = value }

                    /** Sets the font size, in sp. */
                    fun fontSizeSp(value: Float?): Typography = apply { fontSizeSp = value }

                    @Parcelize
                    internal data class State(
                        @FontRes val fontResId: Int?,
                        val fontSizeSp: Float?,
                    ) : Parcelable

                    internal fun build(): State = State(fontResId, fontSizeSp)
                }
            }
        }

        /**
         * [TermsDisplay] controls how mandates and legal agreements are displayed.
         * Use [TermsDisplay.NEVER] to never display legal agreements.
         * The default setting is [TermsDisplay.AUTOMATIC], which causes legal agreements to be shown only when
         * necessary.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class TermsDisplay {
            /** Show legal agreements only when necessary */
            AUTOMATIC,

            /** Never show legal agreements */
            NEVER,
        }

        /**
         * Configuration for how billing details are collected during checkout.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class BillingDetailsCollectionConfiguration {

            private var name: CollectionMode = CollectionMode.Automatic
            private var address: AddressCollectionMode = AddressCollectionMode.Automatic

            /** How to collect the name field. */
            fun name(name: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.name = name
            }

            /** How to collect the billing address. */
            fun address(address: AddressCollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.address = address
            }

            @Parcelize
            internal data class State(
                val name: CollectionMode,
                val phone: CollectionMode,
                val email: CollectionMode,
                val address: AddressCollectionMode,
            ) : Parcelable

            internal fun build(): State = State(
                name = name,
                phone = CollectionMode.Automatic,
                email = CollectionMode.Automatic,
                address = address,
            )

            /**
             * Billing details fields collection options.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class CollectionMode {
                /**
                 * The field will be collected depending on the Payment Method's requirements.
                 */
                Automatic,

                /**
                 * The field will never be collected.
                 * If this field is required by the Payment Method, you must provide it as part of
                 * the default billing details.
                 */
                Never,

                /**
                 * The field will always be collected, even if it isn't required for the Payment
                 * Method.
                 */
                Always,
            }

            /**
             * Billing address collection options.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class AddressCollectionMode {
                /**
                 * Only the fields required by the Payment Method will be collected, this may be
                 * none.
                 */
                Automatic,

                /**
                 * Collect the full billing address, regardless of the Payment Method requirements.
                 */
                Full,

                // Note: a `Never` mode is intentionally omitted for the CheckoutSession private
                // preview — suppressing billing collection is not supported with a CheckoutSession.
                // It can be added at public preview/GA if that use case is supported.
            }
        }
    }
}

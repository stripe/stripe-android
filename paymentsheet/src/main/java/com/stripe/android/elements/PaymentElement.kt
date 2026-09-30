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
                 * Link remains enabled. Its button or row is shown when an existing Link user is
                 * detected and hidden otherwise.
                 */
                WalletButtonHidden,
            }
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
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
            private var shapes = Shapes()
            private var typography = Typography()
            private var primaryButton = PrimaryButton()
            private var embeddedAppearance = Embedded()
            private var formInsetValues = Insets.defaultFormInsetValues

            /** Sets the colors used in light mode. */
            fun colorsLight(colors: Colors): Appearance = apply { colorsLight = colors }

            /** Sets the colors used in dark mode. */
            fun colorsDark(colors: Colors): Appearance = apply { colorsDark = colors }

            /** Sets the color mode used by the Payment Element. */
            fun themeMode(themeMode: ThemeMode): Appearance = apply { this.themeMode = themeMode }

            /** Sets the appearance of shapes. */
            fun shapes(shapes: Shapes): Appearance = apply { this.shapes = shapes }

            /** Sets the typography used for text. */
            fun typography(typography: Typography): Appearance = apply { this.typography = typography }

            /** Sets the appearance of the primary button. */
            fun primaryButton(primaryButton: PrimaryButton): Appearance = apply { this.primaryButton = primaryButton }

            /** Sets the appearance of embedded payment method rows. */
            fun embeddedAppearance(embeddedAppearance: Embedded): Appearance = apply {
                this.embeddedAppearance = embeddedAppearance
            }

            /** Sets the insets used by forms. */
            fun formInsetValues(insets: Insets): Appearance = apply { formInsetValues = insets }

            @Parcelize
            internal data class State(
                val colorsLight: Colors.State,
                val colorsDark: Colors.State,
                val themeMode: ThemeMode,
                val shapes: Shapes.State,
                val typography: Typography.State,
                val primaryButton: PrimaryButton.State,
                val embeddedAppearance: Embedded.State,
                val formInsetValues: Insets.State,
            ) : Parcelable

            internal fun build(): State = State(
                colorsLight = colorsLight.build(),
                colorsDark = colorsDark.build(),
                themeMode = themeMode,
                shapes = shapes.build(),
                typography = typography.build(),
                primaryButton = primaryButton.build(),
                embeddedAppearance = embeddedAppearance.build(),
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

            /** Configures the appearance of shapes. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Shapes {
                private var cornerRadiusDp = StripeThemeDefaults.shapes.cornerRadius
                private var borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth

                /** Sets the corner radius, in dp. */
                fun cornerRadiusDp(value: Float): Shapes = apply { cornerRadiusDp = value }

                /** Sets the border stroke width, in dp. */
                fun borderStrokeWidthDp(value: Float): Shapes = apply { borderStrokeWidthDp = value }

                @Parcelize
                internal data class State(
                    val cornerRadiusDp: Float,
                    val borderStrokeWidthDp: Float,
                ) : Parcelable

                internal fun build(): State = State(cornerRadiusDp, borderStrokeWidthDp)
            }

            /** Configures the typography used for text. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Typography {
                private var sizeScaleFactor = StripeThemeDefaults.typography.fontSizeMultiplier

                @FontRes
                private var fontResId: Int? = StripeThemeDefaults.typography.fontFamily

                /** Sets the scale factor applied to all fonts. */
                fun sizeScaleFactor(value: Float): Typography = apply { sizeScaleFactor = value }

                /** Sets the font resource used for text. */
                fun fontResId(@FontRes value: Int?): Typography = apply { fontResId = value }

                @Parcelize
                internal data class State(
                    val sizeScaleFactor: Float,
                    @FontRes val fontResId: Int?,
                ) : Parcelable

                internal fun build(): State = State(sizeScaleFactor, fontResId)
            }

            /** Configures the appearance of embedded payment method rows. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Embedded {
                private var rowStyle: RowStyle = RowStyle.FlatWithRadio()

                /** Sets the style used for payment method rows. */
                fun rowStyle(value: RowStyle): Embedded = apply { rowStyle = value }

                @Parcelize
                internal data class State(
                    val rowStyle: RowStyle.State,
                ) : Parcelable

                internal fun build(): State = State(rowStyle.build())

                /** Styles available for embedded payment method rows. */
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                sealed class RowStyle {
                    internal abstract fun build(): State

                    internal sealed interface State : Parcelable

                    /** Displays flat rows with radio selection controls. */
                    class FlatWithRadio : RowStyle() {
                        private var separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness
                        private var startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                        private var endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                        private var topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled
                        private var bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled
                        private var additionalVerticalInsetsDp =
                            StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                        private var horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                        private var colorsLight = Colors.light()
                        private var colorsDark = Colors.dark()

                        fun separatorThicknessDp(value: Float): FlatWithRadio = apply {
                            separatorThicknessDp = value
                        }
                        fun startSeparatorInsetDp(value: Float): FlatWithRadio = apply {
                            startSeparatorInsetDp = value
                        }
                        fun endSeparatorInsetDp(value: Float): FlatWithRadio = apply { endSeparatorInsetDp = value }
                        fun topSeparatorEnabled(value: Boolean): FlatWithRadio = apply { topSeparatorEnabled = value }
                        fun bottomSeparatorEnabled(value: Boolean): FlatWithRadio = apply {
                            bottomSeparatorEnabled = value
                        }
                        fun additionalVerticalInsetsDp(value: Float): FlatWithRadio = apply {
                            additionalVerticalInsetsDp = value
                        }
                        fun horizontalInsetsDp(value: Float): FlatWithRadio = apply { horizontalInsetsDp = value }
                        fun colorsLight(value: Colors): FlatWithRadio = apply { colorsLight = value }
                        fun colorsDark(value: Colors): FlatWithRadio = apply { colorsDark = value }

                        internal override fun build(): FlatWithRadioState = FlatWithRadioState(
                            separatorThicknessDp = separatorThicknessDp,
                            startSeparatorInsetDp = startSeparatorInsetDp,
                            endSeparatorInsetDp = endSeparatorInsetDp,
                            topSeparatorEnabled = topSeparatorEnabled,
                            bottomSeparatorEnabled = bottomSeparatorEnabled,
                            additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                            horizontalInsetsDp = horizontalInsetsDp,
                            colorsLight = colorsLight.build(),
                            colorsDark = colorsDark.build(),
                        )

                        class Colors private constructor(
                            @ColorInt private var separatorColor: Int,
                            @ColorInt private var selectedColor: Int,
                            @ColorInt private var unselectedColor: Int,
                        ) {
                            fun separatorColor(@ColorInt value: Int): Colors = apply { separatorColor = value }
                            fun separatorColor(value: Color): Colors = separatorColor(value.toArgb())
                            fun selectedColor(@ColorInt value: Int): Colors = apply { selectedColor = value }
                            fun selectedColor(value: Color): Colors = selectedColor(value.toArgb())
                            fun unselectedColor(@ColorInt value: Int): Colors = apply { unselectedColor = value }
                            fun unselectedColor(value: Color): Colors = unselectedColor(value.toArgb())

                            @Parcelize
                            internal data class State(
                                @ColorInt val separatorColor: Int,
                                @ColorInt val selectedColor: Int,
                                @ColorInt val unselectedColor: Int,
                            ) : Parcelable

                            internal fun build() = State(separatorColor, selectedColor, unselectedColor)

                            companion object {
                                fun light() = Colors(
                                    separatorColor = StripeThemeDefaults.radioColorsLight.separatorColor.toArgb(),
                                    selectedColor = StripeThemeDefaults.radioColorsLight.selectedColor.toArgb(),
                                    unselectedColor = StripeThemeDefaults.radioColorsLight.unselectedColor.toArgb(),
                                )

                                fun dark() = Colors(
                                    separatorColor = StripeThemeDefaults.radioColorsDark.separatorColor.toArgb(),
                                    selectedColor = StripeThemeDefaults.radioColorsDark.selectedColor.toArgb(),
                                    unselectedColor = StripeThemeDefaults.radioColorsDark.unselectedColor.toArgb(),
                                )
                            }
                        }
                    }

                    @Parcelize
                    internal data class FlatWithRadioState(
                        val separatorThicknessDp: Float,
                        val startSeparatorInsetDp: Float,
                        val endSeparatorInsetDp: Float,
                        val topSeparatorEnabled: Boolean,
                        val bottomSeparatorEnabled: Boolean,
                        val additionalVerticalInsetsDp: Float,
                        val horizontalInsetsDp: Float,
                        val colorsLight: FlatWithRadio.Colors.State,
                        val colorsDark: FlatWithRadio.Colors.State,
                    ) : State

                    /** Displays flat rows with checkmark selection controls. */
                    class FlatWithCheckmark : RowStyle() {
                        private var separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness
                        private var startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                        private var endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                        private var topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled
                        private var bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled
                        private var checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp
                        private var additionalVerticalInsetsDp =
                            StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                        private var horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                        private var colorsLight = Colors.light()
                        private var colorsDark = Colors.dark()

                        fun separatorThicknessDp(value: Float): FlatWithCheckmark = apply {
                            separatorThicknessDp = value
                        }
                        fun startSeparatorInsetDp(value: Float): FlatWithCheckmark = apply {
                            startSeparatorInsetDp = value
                        }
                        fun endSeparatorInsetDp(value: Float): FlatWithCheckmark = apply {
                            endSeparatorInsetDp = value
                        }
                        fun topSeparatorEnabled(value: Boolean): FlatWithCheckmark = apply {
                            topSeparatorEnabled = value
                        }
                        fun bottomSeparatorEnabled(value: Boolean): FlatWithCheckmark = apply {
                            bottomSeparatorEnabled = value
                        }
                        fun checkmarkInsetDp(value: Float): FlatWithCheckmark = apply { checkmarkInsetDp = value }
                        fun additionalVerticalInsetsDp(value: Float): FlatWithCheckmark = apply {
                            additionalVerticalInsetsDp = value
                        }
                        fun horizontalInsetsDp(value: Float): FlatWithCheckmark = apply { horizontalInsetsDp = value }
                        fun colorsLight(value: Colors): FlatWithCheckmark = apply { colorsLight = value }
                        fun colorsDark(value: Colors): FlatWithCheckmark = apply { colorsDark = value }

                        internal override fun build(): FlatWithCheckmarkState = FlatWithCheckmarkState(
                            separatorThicknessDp = separatorThicknessDp,
                            startSeparatorInsetDp = startSeparatorInsetDp,
                            endSeparatorInsetDp = endSeparatorInsetDp,
                            topSeparatorEnabled = topSeparatorEnabled,
                            bottomSeparatorEnabled = bottomSeparatorEnabled,
                            checkmarkInsetDp = checkmarkInsetDp,
                            additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                            horizontalInsetsDp = horizontalInsetsDp,
                            colorsLight = colorsLight.build(),
                            colorsDark = colorsDark.build(),
                        )

                        class Colors private constructor(
                            @ColorInt private var separatorColor: Int,
                            @ColorInt private var checkmarkColor: Int,
                        ) {
                            fun separatorColor(@ColorInt value: Int): Colors = apply { separatorColor = value }
                            fun separatorColor(value: Color): Colors = separatorColor(value.toArgb())
                            fun checkmarkColor(@ColorInt value: Int): Colors = apply { checkmarkColor = value }
                            fun checkmarkColor(value: Color): Colors = checkmarkColor(value.toArgb())

                            @Parcelize
                            internal data class State(
                                @ColorInt val separatorColor: Int,
                                @ColorInt val checkmarkColor: Int,
                            ) : Parcelable

                            internal fun build() = State(separatorColor, checkmarkColor)

                            companion object {
                                fun light() = Colors(
                                    separatorColor = StripeThemeDefaults.checkmarkColorsLight.separatorColor.toArgb(),
                                    checkmarkColor = StripeThemeDefaults.checkmarkColorsLight.checkmarkColor.toArgb(),
                                )

                                fun dark() = Colors(
                                    separatorColor = StripeThemeDefaults.checkmarkColorsDark.separatorColor.toArgb(),
                                    checkmarkColor = StripeThemeDefaults.checkmarkColorsDark.checkmarkColor.toArgb(),
                                )
                            }
                        }
                    }

                    @Parcelize
                    internal data class FlatWithCheckmarkState(
                        val separatorThicknessDp: Float,
                        val startSeparatorInsetDp: Float,
                        val endSeparatorInsetDp: Float,
                        val topSeparatorEnabled: Boolean,
                        val bottomSeparatorEnabled: Boolean,
                        val checkmarkInsetDp: Float,
                        val additionalVerticalInsetsDp: Float,
                        val horizontalInsetsDp: Float,
                        val colorsLight: FlatWithCheckmark.Colors.State,
                        val colorsDark: FlatWithCheckmark.Colors.State,
                    ) : State

                    /** Displays rows as separate floating buttons. */
                    class FloatingButton : RowStyle() {
                        private var spacingDp = StripeThemeDefaults.floating.spacing
                        private var additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp

                        fun spacingDp(value: Float): FloatingButton = apply { spacingDp = value }
                        fun additionalInsetsDp(value: Float): FloatingButton = apply { additionalInsetsDp = value }

                        internal override fun build(): FloatingButtonState = FloatingButtonState(
                            spacingDp,
                            additionalInsetsDp,
                        )
                    }

                    @Parcelize
                    internal data class FloatingButtonState(
                        val spacingDp: Float,
                        val additionalInsetsDp: Float,
                    ) : State

                    /** Displays flat rows with disclosure controls. */
                    class FlatWithDisclosure : RowStyle() {
                        private var separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness
                        private var startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                        private var endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                        private var topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled
                        private var bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled
                        private var additionalVerticalInsetsDp =
                            StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                        private var horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                        private var colorsLight = Colors.light()
                        private var colorsDark = Colors.dark()

                        fun separatorThicknessDp(value: Float): FlatWithDisclosure = apply {
                            separatorThicknessDp = value
                        }
                        fun startSeparatorInsetDp(value: Float): FlatWithDisclosure = apply {
                            startSeparatorInsetDp = value
                        }
                        fun endSeparatorInsetDp(value: Float): FlatWithDisclosure = apply {
                            endSeparatorInsetDp = value
                        }
                        fun topSeparatorEnabled(value: Boolean): FlatWithDisclosure = apply {
                            topSeparatorEnabled = value
                        }
                        fun bottomSeparatorEnabled(value: Boolean): FlatWithDisclosure = apply {
                            bottomSeparatorEnabled = value
                        }
                        fun additionalVerticalInsetsDp(value: Float): FlatWithDisclosure = apply {
                            additionalVerticalInsetsDp = value
                        }
                        fun horizontalInsetsDp(value: Float): FlatWithDisclosure = apply { horizontalInsetsDp = value }
                        fun colorsLight(value: Colors): FlatWithDisclosure = apply { colorsLight = value }
                        fun colorsDark(value: Colors): FlatWithDisclosure = apply { colorsDark = value }

                        internal override fun build(): FlatWithDisclosureState = FlatWithDisclosureState(
                            separatorThicknessDp = separatorThicknessDp,
                            startSeparatorInsetDp = startSeparatorInsetDp,
                            endSeparatorInsetDp = endSeparatorInsetDp,
                            topSeparatorEnabled = topSeparatorEnabled,
                            bottomSeparatorEnabled = bottomSeparatorEnabled,
                            additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                            horizontalInsetsDp = horizontalInsetsDp,
                            colorsLight = colorsLight.build(),
                            colorsDark = colorsDark.build(),
                        )

                        class Colors private constructor(
                            @ColorInt private var separatorColor: Int,
                            @ColorInt private var disclosureColor: Int,
                        ) {
                            fun separatorColor(@ColorInt value: Int): Colors = apply { separatorColor = value }
                            fun separatorColor(value: Color): Colors = separatorColor(value.toArgb())
                            fun disclosureColor(@ColorInt value: Int): Colors = apply { disclosureColor = value }
                            fun disclosureColor(value: Color): Colors = disclosureColor(value.toArgb())

                            @Parcelize
                            internal data class State(
                                @ColorInt val separatorColor: Int,
                                @ColorInt val disclosureColor: Int,
                            ) : Parcelable

                            internal fun build() = State(separatorColor, disclosureColor)

                            companion object {
                                fun light() = Colors(
                                    separatorColor = StripeThemeDefaults.disclosureColorsLight.separatorColor.toArgb(),
                                    disclosureColor =
                                        StripeThemeDefaults.disclosureColorsLight.disclosureColor.toArgb(),
                                )

                                fun dark() = Colors(
                                    separatorColor = StripeThemeDefaults.disclosureColorsDark.separatorColor.toArgb(),
                                    disclosureColor = StripeThemeDefaults.disclosureColorsDark.disclosureColor.toArgb(),
                                )
                            }
                        }
                    }

                    @Parcelize
                    internal data class FlatWithDisclosureState(
                        val separatorThicknessDp: Float,
                        val startSeparatorInsetDp: Float,
                        val endSeparatorInsetDp: Float,
                        val topSeparatorEnabled: Boolean,
                        val bottomSeparatorEnabled: Boolean,
                        val additionalVerticalInsetsDp: Float,
                        val horizontalInsetsDp: Float,
                        val colorsLight: FlatWithDisclosure.Colors.State,
                        val colorsDark: FlatWithDisclosure.Colors.State,
                    ) : State
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
    }
}

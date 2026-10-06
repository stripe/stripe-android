package com.stripe.android.elements

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutMandateState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.getRawValueFromDimenResource
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement @Inject internal constructor(
    private val contentHelper: EmbeddedContentHelper,
    private val mandateState: CheckoutMandateState,
    private val stateHolder: CheckoutControllerStateHolder,
) {

    /**
     * A composable function that displays payment methods inline.
     *
     * It can present a sheet to collect more details or display saved payment methods.
     */
    @Composable
    fun Content() {
        val state by stateHolder.stateFlow.collectAsState()
        val mandateAcknowledgementId = state?.mandateAcknowledgementId
        SideEffect {
            mandateAcknowledgementId?.let(mandateState::recordContentAccess)
        }
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
        @OptIn(AppearanceAPIAdditionsPreview::class)
        class Appearance {
            private var colorsLight = Colors.light()
            private var colorsDark = Colors.dark()
            private var themeMode = ThemeMode.Automatic
            private var primaryButton = PrimaryButton()
            private var shapes = Shapes()
            private var typography = Typography()
            private var embeddedAppearance = Embedded()
            private var formInsetValues = Insets.defaultFormInsetValues

            /** Sets the colors used in light mode. */
            fun colorsLight(colors: Colors): Appearance = apply { colorsLight = colors }

            /** Sets the colors used in dark mode. */
            fun colorsDark(colors: Colors): Appearance = apply { colorsDark = colors }

            /** Sets the color mode used by the Payment Element. */
            fun themeMode(themeMode: ThemeMode): Appearance = apply { this.themeMode = themeMode }

            /** Sets the appearance of the primary button. */
            fun primaryButton(primaryButton: PrimaryButton): Appearance = apply { this.primaryButton = primaryButton }

            /** Sets the typography used for text. */
            fun typography(typography: Typography): Appearance = apply { this.typography = typography }

            /** Sets the shape of inputs, tabs, and other components. */
            fun shapes(shapes: Shapes): Appearance = apply { this.shapes = shapes }

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
                val primaryButton: PrimaryButton.State,
                val shapes: Shapes.State,
                val typography: Typography.State,
                val embeddedAppearance: Embedded.State,
                val formInsetValues: Insets.State,
            ) : Parcelable

            internal fun build(): State = State(
                colorsLight = colorsLight.build(),
                colorsDark = colorsDark.build(),
                themeMode = themeMode,
                primaryButton = primaryButton.build(),
                shapes = shapes.build(),
                typography = typography.build(),
                embeddedAppearance = embeddedAppearance.build(),
                formInsetValues = formInsetValues.build(),
            )

            /** Configures embedded payment method rows, margins, and fonts. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Embedded {
                private var rowStyle: RowStyle = RowStyle.FlatWithRadio()
                private var paymentMethodIconMargins: Insets? = null
                private var titleFont: Typography.Font? = null
                private var subtitleFont: Typography.Font? = null

                fun rowStyle(rowStyle: RowStyle): Embedded = apply {
                    this.rowStyle = rowStyle
                }

                @AppearanceAPIAdditionsPreview
                fun paymentMethodIconMargins(margins: Insets?): Embedded = apply {
                    this.paymentMethodIconMargins = margins
                }

                @AppearanceAPIAdditionsPreview
                fun titleFont(font: Typography.Font?): Embedded = apply {
                    this.titleFont = font
                }

                @AppearanceAPIAdditionsPreview
                fun subtitleFont(font: Typography.Font?): Embedded = apply {
                    this.subtitleFont = font
                }

                @Parcelize
                internal data class State(
                    val style: RowStyle.State,
                    val paymentMethodIconMargins: Insets.State?,
                    val titleFont: Typography.Font.State?,
                    val subtitleFont: Typography.Font.State?,
                ) : Parcelable

                internal fun build(): State = State(
                    style = rowStyle.build(),
                    paymentMethodIconMargins = paymentMethodIconMargins?.build(),
                    titleFont = titleFont?.build(),
                    subtitleFont = subtitleFont?.build(),
                )

                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                sealed class RowStyle {
                    internal abstract fun build(): State

                    internal sealed class State : Parcelable

                    @CheckoutSessionPreview
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    class FlatWithRadio : RowStyle() {
                        private var separatorThicknessDp: Float = StripeThemeDefaults.flat.separatorThickness
                        private var startSeparatorInsetDp: Float = StripeThemeDefaults.flat.separatorInsets
                        private var endSeparatorInsetDp: Float = StripeThemeDefaults.flat.separatorInsets
                        private var topSeparatorEnabled: Boolean = StripeThemeDefaults.flat.topSeparatorEnabled
                        private var bottomSeparatorEnabled: Boolean = StripeThemeDefaults.flat.bottomSeparatorEnabled
                        private var additionalVerticalInsetsDp: Float =
                            StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                        private var horizontalInsetsDp: Float = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                        private var colorsLight: Colors = Colors.light()
                        private var colorsDark: Colors = Colors.dark()

                        /**
                         * The thickness of the separator line between rows.
                         */
                        fun separatorThicknessDp(thickness: Float): FlatWithRadio = apply {
                            this.separatorThicknessDp = thickness
                        }

                        /**
                         * The start inset of the separator line between rows.
                         */
                        fun startSeparatorInsetDp(inset: Float): FlatWithRadio = apply {
                            this.startSeparatorInsetDp = inset
                        }

                        /**
                         * The end inset of the separator line between rows.
                         */
                        fun endSeparatorInsetDp(inset: Float): FlatWithRadio = apply {
                            this.endSeparatorInsetDp = inset
                        }

                        /**
                         * Determines if the top separator is visible at the top of
                         * the Embedded Mobile Payment Element.
                         */
                        fun topSeparatorEnabled(enabled: Boolean): FlatWithRadio = apply {
                            this.topSeparatorEnabled = enabled
                        }

                        /**
                         * Determines if the bottom separator is visible at the
                         * bottom of the Embedded Mobile Payment
                         * Element.
                         */
                        fun bottomSeparatorEnabled(enabled: Boolean): FlatWithRadio = apply {
                            this.bottomSeparatorEnabled = enabled
                        }

                        /**
                         * Additional vertical insets applied to a payment method row.
                         * - Note: Increasing this value increases the height of each row.
                         */
                        fun additionalVerticalInsetsDp(insets: Float): FlatWithRadio = apply {
                            this.additionalVerticalInsetsDp = insets
                        }

                        /**
                         * Horizontal insets applied to a payment method row.
                         */
                        fun horizontalInsetsDp(insets: Float): FlatWithRadio = apply {
                            this.horizontalInsetsDp = insets
                        }

                        /**
                         * Describes the colors used while the system is in light mode.
                         */
                        fun colorsLight(colors: Colors): FlatWithRadio = apply {
                            this.colorsLight = colors
                        }

                        /**
                         * Describes the colors used while the system is in dark mode.
                         */
                        fun colorsDark(colors: Colors): FlatWithRadio = apply {
                            this.colorsDark = colors
                        }

                        @Parcelize
                        internal data class State(
                            val separatorThicknessDp: Float,
                            val startSeparatorInsetDp: Float,
                            val endSeparatorInsetDp: Float,
                            val topSeparatorEnabled: Boolean,
                            val bottomSeparatorEnabled: Boolean,
                            val additionalVerticalInsetsDp: Float,
                            val horizontalInsetsDp: Float,
                            val colorsLight: Colors.State,
                            val colorsDark: Colors.State,
                        ) : RowStyle.State()

                        internal override fun build(): State = State(
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

                        @CheckoutSessionPreview
                        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                        class Colors private constructor(
                            @ColorInt private var separatorColor: Int,
                            @ColorInt private var selectedColor: Int,
                            @ColorInt private var unselectedColor: Int,
                        ) {
                            constructor() : this(
                                separatorColor = StripeThemeDefaults.radioColorsLight.separatorColor.toArgb(),
                                selectedColor = StripeThemeDefaults.radioColorsLight.selectedColor.toArgb(),
                                unselectedColor = StripeThemeDefaults.radioColorsLight.unselectedColor.toArgb(),
                            )

                            /**
                             * The color of the separator line between rows.
                             */
                            fun separatorColor(@ColorInt color: Int): Colors = apply {
                                this.separatorColor = color
                            }

                            /**
                             * The color of the separator line between rows.
                             */
                            fun separatorColor(color: Color): Colors = separatorColor(color.toArgb())

                            /**
                             * The color of the radio button when selected.
                             */
                            fun selectedColor(@ColorInt color: Int): Colors = apply {
                                this.selectedColor = color
                            }

                            /**
                             * The color of the radio button when selected.
                             */
                            fun selectedColor(color: Color): Colors = selectedColor(color.toArgb())

                            /**
                             * The color of the radio button when unselected.
                             */
                            fun unselectedColor(@ColorInt color: Int): Colors = apply {
                                this.unselectedColor = color
                            }

                            /**
                             * The color of the radio button when unselected.
                             */
                            fun unselectedColor(color: Color): Colors = unselectedColor(color.toArgb())

                            @Parcelize
                            internal data class State(
                                @ColorInt val separatorColor: Int,
                                @ColorInt val selectedColor: Int,
                                @ColorInt val unselectedColor: Int,
                            ) : Parcelable

                            internal fun build(): State = State(separatorColor, selectedColor, unselectedColor)

                            @CheckoutSessionPreview
                            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                            companion object {
                                /**
                                 * Creates a [Colors] prepopulated with default light mode values.
                                 */
                                fun light(): Colors = Colors(
                                    separatorColor = StripeThemeDefaults.radioColorsLight.separatorColor.toArgb(),
                                    selectedColor = StripeThemeDefaults.radioColorsLight.selectedColor.toArgb(),
                                    unselectedColor = StripeThemeDefaults.radioColorsLight.unselectedColor.toArgb()
                                )

                                /**
                                 * Creates a [Colors] prepopulated with default dark mode values.
                                 */
                                fun dark(): Colors = Colors(
                                    separatorColor = StripeThemeDefaults.radioColorsDark.separatorColor.toArgb(),
                                    selectedColor = StripeThemeDefaults.radioColorsDark.selectedColor.toArgb(),
                                    unselectedColor = StripeThemeDefaults.radioColorsDark.unselectedColor.toArgb()
                                )
                            }
                        }
                    }

                    @CheckoutSessionPreview
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    class FlatWithCheckmark : RowStyle() {
                        private var separatorThicknessDp: Float = StripeThemeDefaults.flat.separatorThickness
                        private var startSeparatorInsetDp: Float = StripeThemeDefaults.flat.separatorInsets
                        private var endSeparatorInsetDp: Float = StripeThemeDefaults.flat.separatorInsets
                        private var topSeparatorEnabled: Boolean = StripeThemeDefaults.flat.topSeparatorEnabled
                        private var bottomSeparatorEnabled: Boolean = StripeThemeDefaults.flat.bottomSeparatorEnabled
                        private var checkmarkInsetDp: Float = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp
                        private var additionalVerticalInsetsDp: Float =
                            StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                        private var horizontalInsetsDp: Float = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                        private var colorsLight: Colors = Colors.light()
                        private var colorsDark: Colors = Colors.dark()

                        /**
                         * The thickness of the separator line between rows.
                         */
                        fun separatorThicknessDp(thickness: Float): FlatWithCheckmark = apply {
                            this.separatorThicknessDp = thickness
                        }

                        /**
                         * The start inset of the separator line between rows.
                         */
                        fun startSeparatorInsetDp(inset: Float): FlatWithCheckmark = apply {
                            this.startSeparatorInsetDp = inset
                        }

                        /**
                         * The end inset of the separator line between rows.
                         */
                        fun endSeparatorInsetDp(inset: Float): FlatWithCheckmark = apply {
                            this.endSeparatorInsetDp = inset
                        }

                        /**
                         * Determines if the top separator is visible at the top of
                         * the Embedded Mobile Payment Element.
                         */
                        fun topSeparatorEnabled(enabled: Boolean): FlatWithCheckmark = apply {
                            this.topSeparatorEnabled = enabled
                        }

                        /**
                         * Determines if the bottom separator is visible at the
                         * bottom of the Embedded Mobile Payment
                         * Element.
                         */
                        fun bottomSeparatorEnabled(enabled: Boolean): FlatWithCheckmark = apply {
                            this.bottomSeparatorEnabled = enabled
                        }

                        /**
                         * Inset of the checkmark from the end of the row.
                         */
                        fun checkmarkInsetDp(insets: Float): FlatWithCheckmark = apply {
                            this.checkmarkInsetDp = insets
                        }

                        /**
                         * Additional vertical insets applied to a payment method row.
                         * - Note: Increasing this value increases the height of each row.
                         */
                        fun additionalVerticalInsetsDp(insets: Float): FlatWithCheckmark = apply {
                            this.additionalVerticalInsetsDp = insets
                        }

                        /**
                         * Horizontal insets applied to a payment method row.
                         */
                        fun horizontalInsetsDp(insets: Float): FlatWithCheckmark = apply {
                            this.horizontalInsetsDp = insets
                        }

                        /**
                         * Describes the colors used while the system is in light mode.
                         */
                        fun colorsLight(colors: Colors): FlatWithCheckmark = apply {
                            this.colorsLight = colors
                        }

                        /**
                         * Describes the colors used while the system is in dark mode.
                         */
                        fun colorsDark(colors: Colors): FlatWithCheckmark = apply {
                            this.colorsDark = colors
                        }

                        @Parcelize
                        internal data class State(
                            val separatorThicknessDp: Float,
                            val startSeparatorInsetDp: Float,
                            val endSeparatorInsetDp: Float,
                            val topSeparatorEnabled: Boolean,
                            val bottomSeparatorEnabled: Boolean,
                            val checkmarkInsetDp: Float,
                            val additionalVerticalInsetsDp: Float,
                            val horizontalInsetsDp: Float,
                            val colorsLight: Colors.State,
                            val colorsDark: Colors.State,
                        ) : RowStyle.State()

                        internal override fun build(): State = State(
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

                        @CheckoutSessionPreview
                        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                        class Colors private constructor(
                            @ColorInt private var separatorColor: Int,
                            @ColorInt private var checkmarkColor: Int,
                        ) {
                            constructor() : this(
                                separatorColor = StripeThemeDefaults.checkmarkColorsLight.separatorColor.toArgb(),
                                checkmarkColor = StripeThemeDefaults.checkmarkColorsLight.checkmarkColor.toArgb(),
                            )

                            /**
                             * The color of the separator line between rows.
                             */
                            fun separatorColor(@ColorInt color: Int): Colors = apply {
                                this.separatorColor = color
                            }

                            /**
                             * The color of the separator line between rows.
                             */
                            fun separatorColor(color: Color): Colors = separatorColor(color.toArgb())

                            /**
                             * The color of the checkmark.
                             */
                            fun checkmarkColor(@ColorInt color: Int): Colors = apply {
                                this.checkmarkColor = color
                            }

                            /**
                             * The color of the checkmark.
                             */
                            fun checkmarkColor(color: Color): Colors = checkmarkColor(color.toArgb())

                            @Parcelize
                            internal data class State(
                                @ColorInt val separatorColor: Int,
                                @ColorInt val checkmarkColor: Int,
                            ) : Parcelable

                            internal fun build(): State = State(separatorColor, checkmarkColor)

                            @CheckoutSessionPreview
                            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                            companion object {
                                /**
                                 * Creates a [Colors] prepopulated with default light mode values.
                                 */
                                fun light(): Colors = Colors(
                                    separatorColor =
                                        StripeThemeDefaults.checkmarkColorsLight.separatorColor.toArgb(),
                                    checkmarkColor =
                                        StripeThemeDefaults.checkmarkColorsLight.checkmarkColor.toArgb()
                                )

                                /**
                                 * Creates a [Colors] prepopulated with default dark mode values.
                                 */
                                fun dark(): Colors = Colors(
                                    separatorColor =
                                        StripeThemeDefaults.checkmarkColorsDark.separatorColor.toArgb(),
                                    checkmarkColor = StripeThemeDefaults.checkmarkColorsDark.checkmarkColor.toArgb()
                                )
                            }
                        }
                    }

                    @CheckoutSessionPreview
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    class FloatingButton : RowStyle() {
                        private var spacingDp: Float = StripeThemeDefaults.floating.spacing
                        private var additionalInsetsDp: Float =
                            StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp

                        /**
                         * The spacing between payment method rows.
                         */
                        fun spacingDp(spacing: Float): FloatingButton = apply {
                            this.spacingDp = spacing
                        }

                        /**
                         * Additional vertical insets applied to a payment method row.
                         * - Note: Increasing this value increases the height of each row.
                         */
                        fun additionalInsetsDp(insets: Float): FloatingButton = apply {
                            this.additionalInsetsDp = insets
                        }

                        @Parcelize
                        internal data class State(
                            val spacingDp: Float,
                            val additionalInsetsDp: Float,
                        ) : RowStyle.State()

                        internal override fun build(): State = State(
                            spacingDp = spacingDp,
                            additionalInsetsDp = additionalInsetsDp,
                        )
                    }

                    @CheckoutSessionPreview
                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    class FlatWithDisclosure : RowStyle() {
                        private var separatorThicknessDp: Float = StripeThemeDefaults.flat.separatorThickness
                        private var startSeparatorInsetDp: Float = StripeThemeDefaults.flat.separatorInsets
                        private var endSeparatorInsetDp: Float = StripeThemeDefaults.flat.separatorInsets
                        private var topSeparatorEnabled: Boolean = StripeThemeDefaults.flat.topSeparatorEnabled
                        private var bottomSeparatorEnabled: Boolean = StripeThemeDefaults.flat.bottomSeparatorEnabled
                        private var additionalVerticalInsetsDp: Float =
                            StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                        private var horizontalInsetsDp: Float = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                        private var colorsLight: Colors = Colors.light()
                        private var colorsDark: Colors = Colors.dark()
                        private var disclosureIconRes: Int = R.drawable.stripe_ic_chevron_right

                        /**
                         * The thickness of the separator line between rows.
                         */
                        fun separatorThicknessDp(thickness: Float): FlatWithDisclosure = apply {
                            this.separatorThicknessDp = thickness
                        }

                        /**
                         * The start inset of the separator line between rows.
                         */
                        fun startSeparatorInsetDp(inset: Float): FlatWithDisclosure = apply {
                            this.startSeparatorInsetDp = inset
                        }

                        /**
                         * The end inset of the separator line between rows.
                         */
                        fun endSeparatorInsetDp(inset: Float): FlatWithDisclosure = apply {
                            this.endSeparatorInsetDp = inset
                        }

                        /**
                         * Determines if the top separator is visible at the top of
                         * the Embedded Mobile Payment Element.
                         */
                        fun topSeparatorEnabled(enabled: Boolean): FlatWithDisclosure = apply {
                            this.topSeparatorEnabled = enabled
                        }

                        /**
                         * Determines if the bottom separator is visible at the
                         * bottom of the Embedded Mobile Payment
                         * Element.
                         */
                        fun bottomSeparatorEnabled(enabled: Boolean): FlatWithDisclosure = apply {
                            this.bottomSeparatorEnabled = enabled
                        }

                        /**
                         * Additional vertical insets applied to a payment method row.
                         * - Note: Increasing this value increases the height of each row.
                         */
                        fun additionalVerticalInsetsDp(insets: Float): FlatWithDisclosure = apply {
                            this.additionalVerticalInsetsDp = insets
                        }

                        /**
                         * Horizontal insets applied to a payment method row.
                         */
                        fun horizontalInsetsDp(insets: Float): FlatWithDisclosure = apply {
                            this.horizontalInsetsDp = insets
                        }

                        /**
                         * Describes the colors used while the system is in light mode.
                         */
                        fun colorsLight(colors: Colors): FlatWithDisclosure = apply {
                            this.colorsLight = colors
                        }

                        /**
                         * Describes the colors used while the system is in dark mode.
                         */
                        fun colorsDark(colors: Colors): FlatWithDisclosure = apply {
                            this.colorsDark = colors
                        }

                        /**
                         * The drawable displayed on the end of the row - typically, a chevron. This should be
                         * a resource ID value.
                         * - Note: If not set, uses a default chevron.
                         */
                        @AppearanceAPIAdditionsPreview
                        fun disclosureIconRes(@DrawableRes iconRes: Int): FlatWithDisclosure = apply {
                            this.disclosureIconRes = iconRes
                        }

                        @Parcelize
                        internal data class State(
                            val separatorThicknessDp: Float,
                            val startSeparatorInsetDp: Float,
                            val endSeparatorInsetDp: Float,
                            val topSeparatorEnabled: Boolean,
                            val bottomSeparatorEnabled: Boolean,
                            val additionalVerticalInsetsDp: Float,
                            val horizontalInsetsDp: Float,
                            val colorsLight: Colors.State,
                            val colorsDark: Colors.State,
                            val disclosureIconRes: Int,
                        ) : RowStyle.State()

                        internal override fun build(): State = State(
                            separatorThicknessDp = separatorThicknessDp,
                            startSeparatorInsetDp = startSeparatorInsetDp,
                            endSeparatorInsetDp = endSeparatorInsetDp,
                            topSeparatorEnabled = topSeparatorEnabled,
                            bottomSeparatorEnabled = bottomSeparatorEnabled,
                            additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                            horizontalInsetsDp = horizontalInsetsDp,
                            colorsLight = colorsLight.build(),
                            colorsDark = colorsDark.build(),
                            disclosureIconRes = disclosureIconRes,
                        )

                        @CheckoutSessionPreview
                        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                        class Colors private constructor(
                            @ColorInt private var separatorColor: Int,
                            @ColorInt private var disclosureColor: Int,
                        ) {
                            constructor() : this(
                                separatorColor = StripeThemeDefaults.disclosureColorsLight.separatorColor.toArgb(),
                                disclosureColor = StripeThemeDefaults.disclosureColorsLight.disclosureColor.toArgb(),
                            )

                            /**
                             * The color of the separator line between rows.
                             */
                            fun separatorColor(@ColorInt color: Int): Colors = apply {
                                this.separatorColor = color
                            }

                            /**
                             * The color of the separator line between rows.
                             */
                            fun separatorColor(color: Color): Colors = separatorColor(color.toArgb())

                            /**
                             * The color of the disclosure icon.
                             */
                            fun disclosureColor(@ColorInt color: Int): Colors = apply {
                                this.disclosureColor = color
                            }

                            /**
                             * The color of the disclosure icon.
                             */
                            fun disclosureColor(color: Color): Colors = disclosureColor(color.toArgb())

                            @Parcelize
                            internal data class State(
                                @ColorInt val separatorColor: Int,
                                @ColorInt val disclosureColor: Int,
                            ) : Parcelable

                            internal fun build(): State = State(separatorColor, disclosureColor)

                            @CheckoutSessionPreview
                            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                            companion object {
                                /**
                                 * Creates a [Colors] prepopulated with default light mode values.
                                 */
                                fun light(): Colors = Colors(
                                    separatorColor =
                                        StripeThemeDefaults.disclosureColorsLight.separatorColor.toArgb(),
                                    disclosureColor =
                                        StripeThemeDefaults.disclosureColorsLight.disclosureColor.toArgb()
                                )

                                /**
                                 * Creates a [Colors] prepopulated with default dark mode values.
                                 */
                                fun dark(): Colors = Colors(
                                    separatorColor =
                                        StripeThemeDefaults.disclosureColorsDark.separatorColor.toArgb(),
                                    disclosureColor =
                                        StripeThemeDefaults.disclosureColorsDark.disclosureColor.toArgb()
                                )
                            }
                        }
                    }
                }
            }

            /** Shapes used for inputs, tabs, buttons, and sheets. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Shapes {
                private var cornerRadiusDp: Float = StripeThemeDefaults.shapes.cornerRadius
                private var borderStrokeWidthDp: Float = StripeThemeDefaults.shapes.borderStrokeWidth
                private var bottomSheetCornerRadiusDp: Float? = null

                /**
                 * The corner radius used for tabs, inputs, buttons, and other components in Payment Element.
                 *
                 * @param cornerRadiusDp The corner radius in dp.
                 */
                fun cornerRadiusDp(cornerRadiusDp: Float): Shapes = apply {
                    this.cornerRadiusDp = cornerRadiusDp
                }

                /**
                 * The corner radius used for tabs, inputs, buttons, and other components in Payment Element.
                 *
                 * @param cornerRadiusRes The corner radius resource ID.
                 */
                fun cornerRadiusDp(context: Context, @DimenRes cornerRadiusRes: Int): Shapes = apply {
                    this.cornerRadiusDp = context.getRawValueFromDimenResource(cornerRadiusRes)
                }

                /**
                 * The border used for inputs, tabs, and other components in Payment Element.
                 *
                 * @param borderStrokeWidthDp The border width in dp.
                 */
                fun borderStrokeWidthDp(borderStrokeWidthDp: Float): Shapes = apply {
                    this.borderStrokeWidthDp = borderStrokeWidthDp
                }

                /**
                 * The border used for inputs, tabs, and other components in Payment Element.
                 *
                 * @param borderStrokeWidthRes The border width resource ID.
                 */
                fun borderStrokeWidthDp(context: Context, @DimenRes borderStrokeWidthRes: Int): Shapes = apply {
                    this.borderStrokeWidthDp = context.getRawValueFromDimenResource(borderStrokeWidthRes)
                }

                /**
                 * The corner radius used for sheets displayed by Payment Element. By default, this is
                 * set to the same value as [cornerRadiusDp].
                 */
                fun bottomSheetCornerRadiusDp(bottomSheetCornerRadiusDp: Float): Shapes = apply {
                    this.bottomSheetCornerRadiusDp = bottomSheetCornerRadiusDp
                }

                /**
                 * The corner radius used for sheets displayed by Payment Element. By default, this is
                 * set to the same value as [cornerRadiusDp].
                 *
                 * @param bottomSheetCornerRadiusRes The bottom sheet corner radius resource ID.
                 */
                fun bottomSheetCornerRadiusDp(
                    context: Context,
                    @DimenRes bottomSheetCornerRadiusRes: Int
                ): Shapes = apply {
                    this.bottomSheetCornerRadiusDp =
                        context.getRawValueFromDimenResource(bottomSheetCornerRadiusRes)
                }

                @Parcelize
                internal data class State(
                    val cornerRadiusDp: Float,
                    val borderStrokeWidthDp: Float,
                    val bottomSheetCornerRadiusDp: Float,
                ) : Parcelable

                internal fun build(): State = State(
                    cornerRadiusDp = cornerRadiusDp,
                    borderStrokeWidthDp = borderStrokeWidthDp,
                    bottomSheetCornerRadiusDp = bottomSheetCornerRadiusDp ?: cornerRadiusDp,
                )
            }

            /** Typography used for Payment Element text. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Typography {
                private var sizeScaleFactor: Float = StripeThemeDefaults.typography.fontSizeMultiplier

                @FontRes
                private var fontResId: Int? = StripeThemeDefaults.typography.fontFamily

                private var custom: Custom = Custom()

                /**
                 * The scale factor for all fonts in Payment Element, the default value is 1.0.
                 * When this value increases fonts will increase in size and decrease when this value is lowered.
                 */
                fun sizeScaleFactor(sizeScaleFactor: Float): Typography = apply {
                    this.sizeScaleFactor = sizeScaleFactor
                }

                /**
                 * The font used in text. This should be a resource ID value.
                 */
                fun fontResId(@FontRes fontResId: Int?): Typography = apply {
                    this.fontResId = fontResId
                }

                /**
                 * Custom font configuration for specific text styles
                 * Note: When set, these fonts override the default font calculations for
                 * their respective text styles
                 */
                @OptIn(AppearanceAPIAdditionsPreview::class)
                fun custom(custom: Custom): Typography = apply {
                    this.custom = custom
                }

                @Parcelize
                internal data class State(
                    val sizeScaleFactor: Float,
                    @FontRes val fontResId: Int?,
                    val custom: Custom.State,
                ) : Parcelable

                internal fun build(): State = State(
                    sizeScaleFactor = sizeScaleFactor,
                    fontResId = fontResId,
                    custom = custom.build(),
                )

                @AppearanceAPIAdditionsPreview
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                class Custom {
                    private var h1: Font? = null

                    /** Sets the headline font override. */
                    fun h1(font: Font?): Custom = apply { h1 = font }

                    @Parcelize
                    internal data class State(
                        val h1: Font.State?,
                    ) : Parcelable

                    internal fun build(): State = State(
                        h1 = h1?.build(),
                    )
                }

                @AppearanceAPIAdditionsPreview
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                class Font {
                    @FontRes
                    private var fontFamily: Int? = null
                    private var fontSizeSp: Float? = null
                    private var fontWeight: Int? = null
                    private var letterSpacingSp: Float? = null

                    /** Sets the font resource. */
                    fun fontFamily(@FontRes value: Int?): Font = apply { fontFamily = value }

                    /** Sets the font size in sp. */
                    fun fontSizeSp(value: Float?): Font = apply { fontSizeSp = value }

                    /** Sets the font weight. */
                    fun fontWeight(value: Int?): Font = apply { fontWeight = value }

                    /** Sets the letter spacing in sp. */
                    fun letterSpacingSp(value: Float?): Font = apply { letterSpacingSp = value }

                    @Parcelize
                    internal data class State(
                        @FontRes val fontFamily: Int?,
                        val fontSizeSp: Float?,
                        val fontWeight: Int?,
                        val letterSpacingSp: Float?,
                    ) : Parcelable

                    internal fun build(): State = State(
                        fontFamily = fontFamily,
                        fontSizeSp = fontSizeSp,
                        fontWeight = fontWeight,
                        letterSpacingSp = letterSpacingSp,
                    )
                }
            }

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
    }
}

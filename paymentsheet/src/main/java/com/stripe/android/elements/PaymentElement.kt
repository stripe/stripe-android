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
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
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

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(CardFundingFilteringPrivatePreview::class)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()
        private var paymentMethodLayout: PaymentMethodLayout = PaymentMethodLayout.Automatic
        private var opensCardScannerAutomatically: Boolean = false
        private var preferredNetworks: List<CardBrand> = emptyList()
        private var paymentMethodOrder: List<String> = emptyList()
        private var cardBrandAcceptance: CardBrandAcceptance = CardBrandAcceptance.All
        private var allowedCardFundingTypes: List<CardFundingType> = CardFundingType.entries
        private var termsDisplay: Map<PaymentMethod.Type, TermsDisplay> = emptyMap()
        private var appearance: Appearance = Appearance()

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
         * Specifies the card funding types accepted by the payment element.
         *
         * By default, all card funding types are accepted. This is a client-side setting and is
         * not currently supported in Link.
         */
        @CardFundingFilteringPrivatePreview
        fun allowedCardFundingTypes(
            allowedCardFundingTypes: List<CardFundingType>
        ): Configuration = apply {
            this.allowedCardFundingTypes = allowedCardFundingTypes
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

        @Parcelize
        @OptIn(CardFundingFilteringPrivatePreview::class)
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
            val paymentMethodLayout: PaymentMethodLayout,
            val opensCardScannerAutomatically: Boolean,
            val preferredNetworks: List<CardBrand>,
            val paymentMethodOrder: List<String>,
            val cardBrandAcceptance: CardBrandAcceptance,
            val allowedCardFundingTypes: List<CardFundingType>,
            val termsDisplay: Map<PaymentMethod.Type, TermsDisplay>,
            val appearance: Appearance.State,
        ) : Parcelable

        @OptIn(CardFundingFilteringPrivatePreview::class)
        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
            paymentMethodLayout = paymentMethodLayout,
            opensCardScannerAutomatically = opensCardScannerAutomatically,
            preferredNetworks = preferredNetworks,
            paymentMethodOrder = paymentMethodOrder,
            cardBrandAcceptance = cardBrandAcceptance,
            allowedCardFundingTypes = allowedCardFundingTypes,
            termsDisplay = termsDisplay,
            appearance = appearance.build(),
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
         * Card funding categories that can be filtered.
         */
        @CheckoutSessionPreview
        @CardFundingFilteringPrivatePreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        enum class CardFundingType : Parcelable {
            /** Debit cards. */
            Debit,

            /** Credit cards. */
            Credit,

            /** Prepaid cards. */
            Prepaid,

            /** Unknown funding type. */
            Unknown,
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
            private var shapes = Shapes()
            private var typography = Typography()
            private var primaryButton = PrimaryButton()
            private var formInsetValues = Insets.defaultFormInsetValues
            private var sectionSpacing = Spacing(-1f)
            private var textFieldInsets = Insets.defaultTextFieldInsets
            private var iconStyle = IconStyle.Filled
            private var verticalModeRowPadding = StripeThemeDefaults.verticalModeRowPadding

            /** Sets the colors used in light mode. */
            fun colorsLight(colors: Colors): Appearance = apply { colorsLight = colors }

            /** Sets the colors used in dark mode. */
            fun colorsDark(colors: Colors): Appearance = apply { colorsDark = colors }

            /** Sets the color mode used by the Payment Element. */
            fun themeMode(themeMode: ThemeMode): Appearance = apply { this.themeMode = themeMode }

            /** Sets the appearance of shapes. */
            fun shapes(shapes: Shapes): Appearance = apply { this.shapes = shapes }

            /** Sets the typography used by the Payment Element. */
            fun typography(typography: Typography): Appearance = apply { this.typography = typography }

            /** Sets the appearance of the primary button. */
            fun primaryButton(primaryButton: PrimaryButton): Appearance = apply { this.primaryButton = primaryButton }

            /** Sets the insets used by forms. */
            fun formInsetValues(insets: Insets): Appearance = apply { formInsetValues = insets }

            /** Sets the spacing between form sections. */
            @AppearanceAPIAdditionsPreview
            fun sectionSpacing(spacing: Spacing): Appearance = apply { sectionSpacing = spacing }

            /** Sets the insets inside form fields. */
            @AppearanceAPIAdditionsPreview
            fun textFieldInsets(insets: Insets): Appearance = apply { textFieldInsets = insets }

            /** Sets the visual style of Payment Element icons. */
            @AppearanceAPIAdditionsPreview
            fun iconStyle(iconStyle: IconStyle): Appearance = apply { this.iconStyle = iconStyle }

            /** Sets the vertical padding of payment-method rows, in dp. */
            @AppearanceAPIAdditionsPreview
            fun verticalModeRowPadding(value: Float): Appearance = apply { verticalModeRowPadding = value }

            @Parcelize
            internal data class State(
                val colorsLight: Colors.State,
                val colorsDark: Colors.State,
                val themeMode: ThemeMode,
                val shapes: Shapes.State,
                val typography: Typography.State,
                val primaryButton: PrimaryButton.State,
                val formInsetValues: Insets.State,
                val sectionSpacing: Spacing.State,
                val textFieldInsets: Insets.State,
                val iconStyle: IconStyle,
                val verticalModeRowPadding: Float,
            ) : Parcelable

            internal fun build(): State = State(
                colorsLight = colorsLight.build(),
                colorsDark = colorsDark.build(),
                themeMode = themeMode,
                shapes = shapes.build(),
                typography = typography.build(),
                primaryButton = primaryButton.build(),
                formInsetValues = formInsetValues.build(),
                sectionSpacing = sectionSpacing.build(),
                textFieldInsets = textFieldInsets.build(),
                iconStyle = iconStyle,
                verticalModeRowPadding = verticalModeRowPadding,
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

            /** Configures the shapes used by the Payment Element. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @AppearanceAPIAdditionsPreview
            class Shapes {
                private var cornerRadiusDp = StripeThemeDefaults.shapes.cornerRadius
                private var borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth
                private var bottomSheetCornerRadiusDp: Float? = null

                /** Sets the corner radius, in dp. */
                fun cornerRadiusDp(value: Float): Shapes = apply { cornerRadiusDp = value }

                /** Sets the border stroke width, in dp. */
                fun borderStrokeWidthDp(value: Float): Shapes = apply { borderStrokeWidthDp = value }

                /** Sets the bottom-sheet corner radius, in dp. */
                fun bottomSheetCornerRadiusDp(value: Float): Shapes = apply { bottomSheetCornerRadiusDp = value }

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

            /** Configures typography used by the Payment Element. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @AppearanceAPIAdditionsPreview
            class Typography {
                private var sizeScaleFactor = StripeThemeDefaults.typography.fontSizeMultiplier

                @FontRes
                private var fontResId: Int? = StripeThemeDefaults.typography.fontFamily
                private var custom = Custom()

                /** Sets the scale factor applied to text sizes. */
                fun sizeScaleFactor(value: Float): Typography = apply { sizeScaleFactor = value }

                /** Sets the font resource used by the Payment Element. */
                fun fontResId(@FontRes value: Int?): Typography = apply { fontResId = value }

                /** Sets custom typography for individual text styles. */
                fun custom(value: Custom): Typography = apply { custom = value }

                @Parcelize
                internal data class State(
                    val sizeScaleFactor: Float,
                    @FontRes val fontResId: Int?,
                    val custom: Custom.State,
                ) : Parcelable

                internal fun build(): State = State(sizeScaleFactor, fontResId, custom.build())

                /** Configures custom typography for individual Payment Element text styles. */
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @AppearanceAPIAdditionsPreview
                class Custom {
                    private var h1: Font? = null

                    /** Sets the typography for first-level headings. */
                    fun h1(value: Font?): Custom = apply { h1 = value }

                    @Parcelize
                    internal data class State(val h1: Font.State?) : Parcelable

                    internal fun build(): State = State(h1?.build())
                }

                /** Describes the typography of a text style. */
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @AppearanceAPIAdditionsPreview
                class Font {
                    @FontRes
                    private var fontFamily: Int? = null
                    private var fontSizeSp: Float? = null
                    private var fontWeight: Int? = null
                    private var letterSpacingSp: Float? = null

                    /** Sets the font resource. */
                    fun fontFamily(@FontRes value: Int?): Font = apply { fontFamily = value }

                    /** Sets the font size, in sp. */
                    fun fontSizeSp(value: Float?): Font = apply { fontSizeSp = value }

                    /** Sets the font weight. */
                    fun fontWeight(value: Int?): Font = apply { fontWeight = value }

                    /** Sets the letter spacing, in sp. */
                    fun letterSpacingSp(value: Float?): Font = apply { letterSpacingSp = value }

                    @Parcelize
                    internal data class State(
                        @FontRes val fontFamily: Int?,
                        val fontSizeSp: Float?,
                        val fontWeight: Int?,
                        val letterSpacingSp: Float?,
                    ) : Parcelable

                    internal fun build(): State = State(fontFamily, fontSizeSp, fontWeight, letterSpacingSp)
                }
            }

            /** Defines spacing between form sections. */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @AppearanceAPIAdditionsPreview
            class Spacing(private val spacingDp: Float) {
                @Parcelize
                internal data class State(val spacingDp: Float) : Parcelable

                internal fun build(): State = State(spacingDp)
            }

            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @AppearanceAPIAdditionsPreview
            /** Visual styles available for Payment Element icons. */
            enum class IconStyle {
                /** Use filled icons. */
                Filled,

                /** Use outlined icons. */
                Outlined,
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
                    val defaultTextFieldInsets = Insets(
                        StripeThemeDefaults.textFieldInsets.start,
                        StripeThemeDefaults.textFieldInsets.top,
                        StripeThemeDefaults.textFieldInsets.end,
                        StripeThemeDefaults.textFieldInsets.bottom,
                    )
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
            private var email: CollectionMode = CollectionMode.Automatic
            private var address: AddressCollectionMode = AddressCollectionMode.Automatic

            /** How to collect the name field. */
            fun name(name: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.name = name
            }

            /** How to collect the email field. */
            fun email(email: CollectionMode): BillingDetailsCollectionConfiguration = apply {
                this.email = email
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
                email = email,
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

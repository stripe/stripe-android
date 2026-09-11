package com.stripe.android.elements

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.ShippingAddressElementStateHolder
import com.stripe.android.checkout.asPaymentSheet
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.uicore.StripeThemeDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider

@OptIn(CheckoutSessionPreview::class)
internal fun interface CommitShippingAddress {
    suspend operator fun invoke(
        name: String?,
        address: CheckoutController.Address.State,
    ): Result<Unit>
}

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShippingAddressElement internal constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val commitShippingAddress: CommitShippingAddress,
    private val stateHolder: CheckoutControllerStateHolder,
    private val shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
    private val errorReporter: ErrorReporter,
) {
    @Inject
    internal constructor(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        paymentConfiguration: Provider<PaymentConfiguration>,
        @ViewModelScope coroutineScope: CoroutineScope,
        checkoutController: CheckoutController,
        stateHolder: CheckoutControllerStateHolder,
        shippingAddressElementStateHolder: ShippingAddressElementStateHolder,
        errorReporter: ErrorReporter,
    ) : this(
        activityResultCaller = activityResultCaller,
        lifecycleOwner = lifecycleOwner,
        paymentConfiguration = paymentConfiguration,
        coroutineScope = coroutineScope,
        commitShippingAddress = CommitShippingAddress(checkoutController::commitShippingAddress),
        stateHolder = stateHolder,
        shippingAddressElementStateHolder = shippingAddressElementStateHolder,
        errorReporter = errorReporter,
    )

    private val activityLauncher:
        ActivityResultLauncher<AddressElementActivityContract.Args.CheckoutShipping> =
        activityResultCaller.registerForActivityResult(
            AddressElementActivityContract.CheckoutShipping
        ) { result ->
            when (result) {
                is AddressElementActivityContract.Result.CheckoutShippingSucceeded -> {
                    val address = result.address.address?.toCheckoutAddress()
                    if (address == null) {
                        shippingAddressElementStateHolder.isPresenting = false
                    } else {
                        coroutineScope.launch {
                            try {
                                commitShippingAddress(
                                    result.address.name,
                                    address,
                                )
                            } finally {
                                shippingAddressElementStateHolder.isPresenting = false
                            }
                        }
                    }
                }
                AddressElementActivityContract.Result.Canceled -> {
                    shippingAddressElementStateHolder.isPresenting = false
                }
            }
        }

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityLauncher.unregister()
                    super.onDestroy(owner)
                }
            }
        )
    }

    fun present() {
        val checkoutState = stateHolder.state
        if (checkoutState == null) {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.CHECKOUT_SHIPPING_ADDRESS_ELEMENT_PRESENT_NOT_CONFIGURED
            )
            return
        }

        if (shippingAddressElementStateHolder.isPresenting) {
            return
        }

        shippingAddressElementStateHolder.isPresenting = true
        val configuration = checkoutState.configuration.shippingAddressElementConfiguration
        activityLauncher.launch(
            AddressElementActivityContract.Args.CheckoutShipping(
                publishableKey = paymentConfiguration.get().publishableKey,
                config = AddressLauncher.Configuration(
                    appearance = configuration?.appearance?.asPaymentSheet() ?: PaymentSheet.Appearance(),
                    buttonTitle = configuration?.buttonTitle,
                    title = configuration?.title,
                    additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                        phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                    ),
                    billingAddress = null,
                    useStripeHostedAutocomplete = true,
                ),
            )
        )
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("TooManyFunctions")
    class Configuration {
        private var title: String? = null
        private var buttonTitle: String? = null
        private var appearance = Appearance()

        /** Sets the title displayed at the top of the address form. */
        fun title(title: String): Configuration = apply {
            this.title = title
        }

        /** Sets the title displayed on the primary button. */
        fun buttonTitle(buttonTitle: String): Configuration = apply {
            this.buttonTitle = buttonTitle
        }

        /** Sets the appearance of the address form. */
        fun appearance(appearance: Appearance): Configuration = apply {
            this.appearance = appearance
        }

        @Parcelize
        internal data class State(
            val title: String?,
            val buttonTitle: String?,
            val appearance: Appearance.State,
        ) : Parcelable

        internal fun build(): State = State(
            title = title,
            buttonTitle = buttonTitle,
            appearance = appearance.build(),
        )

        /** Appearance configuration for the address form. */
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

            /** Sets the color mode used by the address form. */
            fun themeMode(themeMode: ThemeMode): Appearance = apply { this.themeMode = themeMode }

            /** Sets the appearance of the primary button. */
            fun primaryButton(primaryButton: PrimaryButton): Appearance = apply {
                this.primaryButton = primaryButton
            }

            /** Sets the insets used by the address form. */
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

            /** Colors used to render the address form. */
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
            /** Color modes available for the address form. */
            enum class ThemeMode {
                /** Follow the system's light or dark mode. */
                Automatic,

                /** Always use light-mode colors. */
                AlwaysLight,

                /** Always use dark-mode colors. */
                AlwaysDark,
            }

            /** Insets used to position address form content, in dp. */
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

            /** Configures the primary button. */
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
                    colorsLight = colorsLight.build(),
                    colorsDark = colorsDark.build(),
                    shape = shape.build(),
                    typography = typography.build(),
                )

                /** Colors used to render the primary button. */
                @CheckoutSessionPreview
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @Suppress("TooManyFunctions")
                class Colors private constructor(
                    @ColorInt private var background: Int?,
                    @ColorInt private var onBackground: Int,
                    @ColorInt private var border: Int,
                ) {
                    /** Sets the primary-button background color. */
                    fun background(@ColorInt value: Int?): Colors = apply { background = value }

                    /** Sets the primary-button background color. */
                    fun background(value: Color?): Colors = apply { background = value?.toArgb() }

                    /** Sets the content color used on the primary button. */
                    fun onBackground(@ColorInt value: Int): Colors = apply { onBackground = value }

                    /** Sets the content color used on the primary button. */
                    fun onBackground(value: Color): Colors = onBackground(value.toArgb())

                    /** Sets the primary-button border color. */
                    fun border(@ColorInt value: Int): Colors = apply { border = value }

                    /** Sets the primary-button border color. */
                    fun border(value: Color): Colors = border(value.toArgb())

                    @Parcelize
                    internal data class State(
                        @ColorInt val background: Int?,
                        @ColorInt val onBackground: Int,
                        @ColorInt val border: Int,
                    ) : Parcelable

                    internal fun build(): State = State(background, onBackground, border)

                    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                    companion object {
                        fun light(): Colors = defaults(StripeThemeDefaults.primaryButtonStyle.colorsLight)
                        fun dark(): Colors = defaults(StripeThemeDefaults.primaryButtonStyle.colorsDark)

                        private fun defaults(colors: com.stripe.android.uicore.PrimaryButtonColors): Colors = Colors(
                            background = null,
                            onBackground = colors.onBackground.toArgb(),
                            border = colors.border.toArgb(),
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
    }
}

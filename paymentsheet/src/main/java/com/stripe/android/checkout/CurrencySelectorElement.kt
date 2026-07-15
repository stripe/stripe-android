package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorToggle
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CurrencySelectorElement @Inject internal constructor(
    private val checkoutController: CheckoutController,
    private val viewModelFactory: CurrencySelectorViewModel.Factory,
) {
    private val viewModelKey = "CurrencySelectorViewModel:${System.identityHashCode(checkoutController)}"

    @Composable
    fun Content() {
        val appearanceState = Checkout.CurrencySelectorContentAppearance().build()
        val viewModel: CurrencySelectorViewModel = viewModel(
            key = viewModelKey,
            factory = viewModelFactory,
        )
        val isLoading by checkoutController.isLoading.collectAsState()
        val checkoutSession by checkoutController.checkoutSession.collectAsState()
        val currencySelectorOptions = checkoutSession?.currencySelectorOptions ?: return
        val showCurrencyCode =
            appearanceState.labelContent == Checkout.CurrencySelectorContentAppearance.LabelContent.CURRENCY_CODE
        val errorMessage by viewModel.errorMessage.collectAsState()
        CurrencySelectorToggle(
            options = currencySelectorOptions,
            onCurrencySelected = { currencyOption ->
                viewModel.onCurrencySelected(currencyOption.code)
            },
            isEnabled = !isLoading,
            showCurrencyCode = showCurrencyCode,
            errorMessage = errorMessage?.resolve(),
            appearance = appearanceState,
        )
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var appearance: Appearance = Appearance()

        fun appearance(
            appearance: Appearance
        ): Configuration = apply {
            this.appearance = appearance
        }

        @Parcelize
        internal data class State(
            val appearance: Appearance.State,
        ) : Parcelable

        internal fun build(): State = State(
            appearance = appearance.build(),
        )
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("TooManyFunctions")
    class Appearance {

        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class LabelContent {
            AUTOMATIC,
            CURRENCY_CODE,
            AMOUNT,
        }

        private var contentVerticalPaddingDp: Float = DEFAULT_VERTICAL_PADDING_DP
        private var cornerRadiusDp: Float? = null
        private var borderWidthDp: Float? = null
        private var borderColor: Color? = null
        private var background: Color? = null
        private var selectedBackground: Color? = null
        private var textColor: Color? = null
        private var selectedTextColor: Color? = null
        private var textSecondaryColor: Color? = null
        private var dangerColor: Color? = null

        @FontRes
        private var fontResId: Int? = null
        private var sizeScaleFactor: Float = DEFAULT_SIZE_SCALE_FACTOR
        private var labelContent: LabelContent = LabelContent.AUTOMATIC

        fun contentVerticalPaddingDp(contentVerticalPaddingDp: Float): Appearance = apply {
            require(contentVerticalPaddingDp.isFinite() && contentVerticalPaddingDp >= 0f) {
                "contentVerticalPaddingDp must be finite and non-negative"
            }
            this.contentVerticalPaddingDp = contentVerticalPaddingDp
        }

        fun cornerRadiusDp(cornerRadiusDp: Float): Appearance = apply {
            require(cornerRadiusDp.isFinite() && cornerRadiusDp >= 0f) {
                "cornerRadiusDp must be finite and non-negative"
            }
            this.cornerRadiusDp = cornerRadiusDp
        }

        fun borderWidthDp(borderWidthDp: Float): Appearance = apply {
            require(borderWidthDp.isFinite() && borderWidthDp >= 0f) {
                "borderWidthDp must be finite and non-negative"
            }
            this.borderWidthDp = borderWidthDp
        }

        fun borderColor(borderColor: Color): Appearance = apply {
            this.borderColor = borderColor
        }

        fun background(background: Color): Appearance = apply {
            this.background = background
        }

        fun selectedBackground(selectedBackground: Color): Appearance = apply {
            this.selectedBackground = selectedBackground
        }

        fun textColor(textColor: Color): Appearance = apply {
            this.textColor = textColor
        }

        fun selectedTextColor(selectedTextColor: Color): Appearance = apply {
            this.selectedTextColor = selectedTextColor
        }

        fun textSecondaryColor(textSecondaryColor: Color): Appearance = apply {
            this.textSecondaryColor = if (textSecondaryColor.alpha < MIN_SECONDARY_ALPHA) {
                textSecondaryColor.copy(alpha = MIN_SECONDARY_ALPHA)
            } else {
                textSecondaryColor
            }
        }

        fun dangerColor(dangerColor: Color): Appearance = apply {
            this.dangerColor = dangerColor
        }

        fun fontResId(@FontRes fontResId: Int?): Appearance = apply {
            this.fontResId = fontResId
        }

        fun sizeScaleFactor(sizeScaleFactor: Float): Appearance = apply {
            require(sizeScaleFactor.isFinite() && sizeScaleFactor > 0f) {
                "sizeScaleFactor must be finite and greater than zero"
            }
            this.sizeScaleFactor = sizeScaleFactor
        }

        fun labelContent(labelContent: LabelContent): Appearance = apply {
            this.labelContent = labelContent
        }

        @Parcelize
        internal data class State(
            val contentVerticalPaddingDp: Float,
            val cornerRadiusDp: Float?,
            val borderWidthDp: Float?,
            @ColorInt val borderColor: Int?,
            @ColorInt val background: Int?,
            @ColorInt val selectedBackground: Int?,
            @ColorInt val textColor: Int?,
            @ColorInt val selectedTextColor: Int?,
            @ColorInt val textSecondaryColor: Int?,
            @ColorInt val dangerColor: Int?,
            @FontRes val fontResId: Int?,
            val sizeScaleFactor: Float,
            val labelContent: LabelContent,
        ) : Parcelable

        internal fun build(): State = State(
            contentVerticalPaddingDp = contentVerticalPaddingDp,
            cornerRadiusDp = cornerRadiusDp,
            borderWidthDp = borderWidthDp,
            borderColor = borderColor?.toArgb(),
            background = background?.toArgb(),
            selectedBackground = selectedBackground?.toArgb(),
            textColor = textColor?.toArgb(),
            selectedTextColor = selectedTextColor?.toArgb(),
            textSecondaryColor = textSecondaryColor?.toArgb(),
            dangerColor = dangerColor?.toArgb(),
            fontResId = fontResId,
            sizeScaleFactor = sizeScaleFactor,
            labelContent = labelContent,
        )

        internal companion object {
            const val DEFAULT_VERTICAL_PADDING_DP = 4f
            const val DEFAULT_SIZE_SCALE_FACTOR = 1.0f
            const val MIN_SECONDARY_ALPHA = 0.5f
        }
    }
}

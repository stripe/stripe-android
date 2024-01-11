package com.stripe.android.lpmfoundations

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.PaymentMethodUI
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve

/**
 * Defines the required information for displaying a local payment method for selection.
 * Once the LPM is selected, the [AddPaymentMethodUiDefinition] will be used to display the fields of the LPM.
 */
internal class AddPaymentMethodSelectorUiDefinition(
    /** This describes the name that appears under the selector. */
    val displayName: ResolvableString,

    /**
     * This describes the image in the LPM selector.
     *
     * These can be found internally [here](http://go/elements-mobile-pm-icons).
     */
    @DrawableRes val iconResource: Int,

    /** An optional light theme icon url if it's supported. */
    val lightThemeIconUrl: String?,

    /** An optional dark theme icon url if it's supported. */
    val darkThemeIconUrl: String?,

    /** Indicates if the lpm icon in the selector is a single color and should be tinted
     * on selection.
     */
    val tintIconOnSelection: Boolean,
)

internal class AddPaymentMethodSelectorUiDefinitionBuilder(
    /** This describes the name that appears under the selector. */
    var displayName: ResolvableString? = null,

    /**
     * This describes the image in the LPM selector.
     *
     * These can be found internally [here](http://go/elements-mobile-pm-icons).
     */
    @DrawableRes var iconResource: Int? = null,

    /** An optional light theme icon url if it's supported. */
    var lightThemeIconUrl: String? = null,

    /** An optional dark theme icon url if it's supported. */
    var darkThemeIconUrl: String? = null,

    /** Indicates if the lpm icon in the selector is a single color and should be tinted
     * on selection.
     */
    var tintIconOnSelection: Boolean = false,
) {
    fun build(): AddPaymentMethodSelectorUiDefinition {
        return AddPaymentMethodSelectorUiDefinition(
            displayName = displayName!!,
            iconResource = iconResource!!,
            lightThemeIconUrl = lightThemeIconUrl,
            darkThemeIconUrl = darkThemeIconUrl,
            tintIconOnSelection = tintIconOnSelection,
        )
    }
}

internal class AddPaymentMethodSelectorUiRenderer(private val definition: AddPaymentMethodUiDefinition) {
    @Composable
    fun Content(
        imageLoader: StripeImageLoader,
        isEnabled: Boolean,
        uiState: UiState,
        minViewWidth: Dp,
        modifier: Modifier,
    ) {
        val selectedUiDefinition by uiState.selected.collectAsState()
        val selectorUiDefinition = definition.addPaymentMethodSelectorUiDefinition

        PaymentMethodUI(
            minViewWidth = minViewWidth,
            iconRes = selectorUiDefinition.iconResource,
            iconUrl = selectorUiDefinition.lightThemeIconUrl,
            imageLoader = imageLoader,
            title = selectorUiDefinition.displayName.resolve(),
            isSelected = selectedUiDefinition.addPaymentMethodSelectorUiDefinition == selectorUiDefinition,
            isEnabled = isEnabled,
            tintOnSelected = selectorUiDefinition.tintIconOnSelection,
            modifier = modifier,
            onItemSelectedListener = { uiState.updateSelected(definition) },
        )
    }
}

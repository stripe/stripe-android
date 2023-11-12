package com.stripe.android.paymentsheet.prototype

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

internal class AddPaymentMethodSelectorUiDefinition(
    /** This describes the name that appears under the selector. */
    val displayName: ResolvableString,

    /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
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

    /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
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

internal class SelectorUiRenderer(private val definition: AddPaymentMethodUiDefinition) {
    @Composable
    fun Render(
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

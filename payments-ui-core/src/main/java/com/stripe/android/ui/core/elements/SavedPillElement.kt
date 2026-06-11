package com.stripe.android.ui.core.elements

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.R as StripeUiCoreR
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldComposable
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionFieldValidationController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

private val BrandLogoWidth = 40.dp
private val BrandLogoHeight = 24.dp
private val RowMinHeight = 52.dp
private val HorizontalPadding = 16.dp
private val VerticalPadding = 4.dp
private val BrandToTextSpacing = 12.dp

/**
 * Holds display state for [SavedPillElement]. Call [updateDisplay] when the last four digits or
 * brand artwork change.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SavedPillController(
    val last4: String,
    val cardBrand: CardBrand,
    val onRemoveClicked: () -> Unit,
)

@Preview
@Composable
fun SavedPillElementPreview() {
    SavedPillElementUI(
        enabled = true,
        controller = SavedPillController(
            last4 = "4242",
            cardBrand = CardBrand.Visa,
            onRemoveClicked = {},
        )
    )
}

/**
 * Top row of the add-card form: brand logo, masked last four, and remove control. Outer borders
 * and section chrome are owned by the parent; this composable only applies inner padding and
 * layout.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun SavedPillElementUI(
    enabled: Boolean,
    controller: SavedPillController,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(
                top = VerticalPadding,
                bottom = VerticalPadding,
                start = HorizontalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(controller.cardBrand.icon),
            contentDescription = null,
            modifier = Modifier.size(width = BrandLogoWidth, height = BrandLogoHeight),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(BrandToTextSpacing))

        Text(
            text = buildMaskedLastFour(controller.last4),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colors.onSurface,
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = controller.onRemoveClicked,
            enabled = enabled,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                painter = painterResource(StripeUiCoreR.drawable.stripe_ic_material_close),
                contentDescription = stringResource(R.string.stripe_saved_pill_remove_card),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.stripeColors.appBarIcon,
            )
        }
    }
}

private fun buildMaskedLastFour(last4: String): String {
    val digitsOnly = last4.filter { it.isDigit() }
    val display = digitsOnly.takeLast(4).ifEmpty {
        last4.filterNot { it.isWhitespace() }.takeLast(4)
    }
    return if (display.isEmpty()) {
        "••••"
    } else {
        "•••• $display"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SavedPillElement(
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("_saved_pill_"),
    val controller: SavedPillController,
) : SectionFieldElement {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return stateFlowOf(emptyList())
    }

    override fun sectionFieldErrorController(): SectionFieldValidationController {
        val pillController = controller
        return object : SectionFieldValidationController, SectionFieldComposable {
            override val validationMessage: StateFlow<FieldValidationMessage?> = stateFlowOf(null)

            @Composable
            @Suppress("UNUSED_PARAMETER")
            override fun ComposeUI(
                enabled: Boolean,
                field: SectionFieldElement,
                modifier: Modifier,
                hiddenIdentifiers: Set<IdentifierSpec>,
                lastTextFieldIdentifier: IdentifierSpec?,
            ) {
                SavedPillElementUI(
                    enabled = enabled,
                    controller = pillController,
                    modifier = modifier,
                )
            }
        }
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from FormArguments to populate
    }

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> {
        return stateFlowOf(emptyList())
    }
}

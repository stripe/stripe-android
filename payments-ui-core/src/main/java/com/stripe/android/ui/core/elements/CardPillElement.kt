package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldComposable
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionFieldValidationController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.R as PaymentsCoreR

internal class CardPillElement(
    val controller: CardPillController,
) : SectionFieldElement {
    override val identifier: IdentifierSpec = IdentifierSpec.Generic("card_scanned_pill")
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        stateFlowOf(emptyList())

    override fun sectionFieldErrorController(): SectionFieldValidationController = controller

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // No-op
    }

    override fun getTextFieldIdentifiers(): StateFlow<List<IdentifierSpec>> =
        stateFlowOf(emptyList())

    override fun onValidationStateChanged(isValidating: Boolean) {
        controller.onValidationStateChanged(isValidating)
    }
}

internal class CardPillController(
    val cardNumber: String,
    private val onDismissPill: () -> Unit,
) : SectionFieldValidationController, SectionFieldComposable {
    override val validationMessage: StateFlow<FieldValidationMessage?> = stateFlowOf(null)

    override fun onValidationStateChanged(isValidating: Boolean) {
        // No-op
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
    ) {
        CardPillElementUI(
            enabled = enabled,
            cardBrand = CardBrand.fromCardNumber(cardNumber),
            lastFourDigits = cardNumber.takeLast(TAKE_LAST_4),
            onDismiss = onDismissPill,
            modifier = modifier,
        )
    }

    private companion object {
        const val TAKE_LAST_4 = 4
    }
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CardPillElementUI(
    enabled: Boolean,
    cardBrand: CardBrand,
    lastFourDigits: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardBrandIcon(cardBrand)

        Spacer(modifier = Modifier.width(12.dp))

        CardNumberText(
            lastFourDigits = lastFourDigits,
            cardBrand = cardBrand,
        )

        Spacer(modifier = Modifier.width(4.dp))

        DismissButton(enabled, onDismiss)
    }
}

@Composable
private fun CardBrandIcon(
    cardBrand: CardBrand
) {
    Image(
        painter = painterResource(cardBrand.icon),
        contentDescription = null,
        modifier = Modifier
            .height(22.dp)
            .width(34.dp),
    )
}

@Composable
private fun RowScope.CardNumberText(
    cardBrand: CardBrand,
    lastFourDigits: String,
) {
    val summaryDescription = stringResource(
        PaymentsCoreR.string.stripe_card_ending_in,
        cardBrand.displayName,
        lastFourDigits,
    )
    val maskedPan = stringResource(R.string.stripe_scanned_card_pill_masked_last4, lastFourDigits)

    Text(
        text = maskedPan,
        style = MaterialTheme.typography.body1,
        color = MaterialTheme.stripeColors.onComponent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .weight(1f)
            .semantics {
                contentDescription = summaryDescription
            },
    )
}

@Composable
private fun DismissButton(
    enabled: Boolean,
    onDismiss: () -> Unit,
) {
    val dismissLabel = stringResource(R.string.stripe_scanned_card_pill_clear_content_description)
    val dismissInteractionSource = remember { MutableInteractionSource() }
    val dismissColor = MaterialTheme.stripeColors.onComponent.copy(
        alpha = if (enabled) LocalContentColor.current.alpha else ContentAlpha.disabled,
    )

    Icon(
        painter = painterResource(R.drawable.stripe_ic_rounded_close),
        contentDescription = null,
        tint = dismissColor,
        modifier = Modifier
            .size(8.dp)
            .clearAndSetSemantics {
                contentDescription = dismissLabel
                role = Role.Button
            }
            .clickable(
                enabled = enabled,
                interactionSource = dismissInteractionSource,
                indication = null,
                onClick = onDismiss,
            ),
    )
}

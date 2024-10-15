package com.stripe.android.ui.core.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldComposable
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionFieldErrorController
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import kotlin.coroutines.CoroutineContext

internal class CardDetailsController(
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    initialValues: Map<IdentifierSpec, String?>,
    collectName: Boolean = false,
    cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    uiContext: CoroutineContext = Dispatchers.Main,
    workContext: CoroutineContext = Dispatchers.IO,
    cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter()
) : SectionFieldErrorController, SectionFieldComposable {

    val nameElement = if (collectName) {
        SimpleTextElement(
            controller = SimpleTextFieldController(
                textFieldConfig = SimpleTextFieldConfig(
                    label = R.string.stripe_name_on_card,
                    capitalization = KeyboardCapitalization.Words,
                    keyboard = androidx.compose.ui.text.input.KeyboardType.Text
                ),
                initialValue = initialValues[IdentifierSpec.Name],
            ),
            identifier = IdentifierSpec.Name,
        )
    } else {
        null
    }

    val label: Int? = null
    val numberElement = CardNumberElement(
        IdentifierSpec.CardNumber,
        DefaultCardNumberController(
            cardTextFieldConfig = CardNumberConfig(),
            cardAccountRangeRepository = cardAccountRangeRepositoryFactory.create(),
            uiContext = uiContext,
            workContext = workContext,
            initialValue = initialValues[IdentifierSpec.CardNumber],
            cardBrandChoiceConfig = when (cbcEligibility) {
                is CardBrandChoiceEligibility.Eligible -> CardBrandChoiceConfig.Eligible(
                    preferredBrands = cbcEligibility.preferredNetworks,
                    initialBrand = initialValues[
                        IdentifierSpec.PreferredCardBrand
                    ]?.let { value ->
                        CardBrand.fromCode(value)
                    }
                )
                is CardBrandChoiceEligibility.Ineligible -> CardBrandChoiceConfig.Ineligible
            },
            cardBrandFilter = cardBrandFilter
        )
    )

    val cvcElement = CvcElement(
        IdentifierSpec.CardCvc,
        CvcController(
            CvcConfig(),
            numberElement.controller.cardBrandFlow,
            initialValue = initialValues[IdentifierSpec.CardCvc]
        )
    )

    val expirationDateElement = SimpleTextElement(
        IdentifierSpec.Generic("date"),
        SimpleTextFieldController(
            DateConfig(),
            initialValue = initialValues[IdentifierSpec.CardExpMonth] +
                initialValues[IdentifierSpec.CardExpYear]?.takeLast(2)
        )
    )

    private val rowFields = listOf(expirationDateElement, cvcElement)
    val fields = listOfNotNull(
        nameElement,
        numberElement,
        RowElement(
            IdentifierSpec.Generic("row_" + UUID.randomUUID().leastSignificantBits),
            rowFields,
            RowController(rowFields)
        )
    )

    override val error = combineAsStateFlow(
        listOfNotNull(
            nameElement,
            numberElement,
            expirationDateElement,
            cvcElement
        )
            .map { it.controller }
            .map { it.error }
    ) {
        it.filterNotNull().firstOrNull()
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    ) {
        CardDetailsElementUI(
            enabled,
            this,
            hiddenIdentifiers,
            lastTextFieldIdentifier
        )
    }
}

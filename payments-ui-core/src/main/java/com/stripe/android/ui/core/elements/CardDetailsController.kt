package com.stripe.android.ui.core.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.DefaultFieldValidationMessageComparator
import com.stripe.android.uicore.elements.FieldValidationMessageComparator
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldComposable
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionFieldValidationController
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

internal class CardDetailsController(
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    initialValues: Map<IdentifierSpec, String?>,
    collectName: Boolean = false,
    cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    uiContext: CoroutineContext = Dispatchers.Main,
    workContext: CoroutineContext = Dispatchers.IO,
    cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter,
    cardDetailsTextFieldConfig: CardNumberTextFieldConfig = CardNumberConfig(
        isCardBrandChoiceEligible = cbcEligibility != CardBrandChoiceEligibility.Ineligible,
        cardBrandFilter = cardBrandFilter,
        cardFundingFilter = cardFundingFilter
    ),
    cvcTextFieldConfig: CvcTextFieldConfig = CvcConfig(),
    dateConfig: TextFieldConfig = DateConfig(),
    private val validationMessageComparator: FieldValidationMessageComparator = DefaultFieldValidationMessageComparator
) : SectionFieldValidationController, SectionFieldComposable {
    val cardPillElement = MutableStateFlow<CardPillElement?>(null)

    val nameElement = if (collectName) {
        SimpleTextElement(
            controller = SimpleTextFieldController(
                textFieldConfig = SimpleTextFieldConfig(
                    label = resolvableString(R.string.stripe_name_on_card),
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
            cardTextFieldConfig = cardDetailsTextFieldConfig,
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
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter
        )
    )

    val cvcElement = CvcElement(
        IdentifierSpec.CardCvc,
        CvcController(
            cvcTextFieldConfig,
            numberElement.controller.cardBrandFlow,
            initialValue = initialValues[IdentifierSpec.CardCvc]
        )
    )

    val expirationDateElement = SimpleTextElement(
        IdentifierSpec.Generic("date"),
        SimpleTextFieldController(
            textFieldConfig = dateConfig,
            initialValue = initialValues[IdentifierSpec.CardExpMonth] +
                initialValues[IdentifierSpec.CardExpYear]?.takeLast(2),
            overrideContentDescriptionProvider = ::formatExpirationDateForAccessibility
        )
    )

    fun onScannedCard(scannedCardDetails: ScannedCardDetails) {
        if (scannedCardDetails is ScannedCardDetails.Validated) {
            cardPillElement.value = CardPillElement(
                controller = CardPillController(
                    cardNumber = scannedCardDetails.cardNumber,
                    onDismissPill = ::dismissCardPill,
                )
            )
            numberElement.controller.onRawValueChange(scannedCardDetails.cardNumber)
            expirationDateElement.controller.onRawValueChange(
                formatExpirationDate(
                    scannedCardDetails.expirationMonth,
                    scannedCardDetails.expirationYear,
                )
            )
        } else {
            numberElement.controller.onRawValueChange(scannedCardDetails.cardNumber)

            val expirationMonth = scannedCardDetails.expirationMonth
            val expirationYear = scannedCardDetails.expirationYear

            val newDate = if (
                expirationMonth != null &&
                expirationYear != null &&
                DateUtils.isExpiryDataValid(
                    expiryMonth = expirationMonth,
                    expiryYear = expirationYear
                )
            ) {
                @Suppress("MagicNumber")
                formatExpirationDate(expirationMonth, expirationYear)
            } else {
                ""
            }
            expirationDateElement.controller.onRawValueChange(newDate)
        }
        cvcElement.controller.onRawValueChange("")
    }

    val fields: StateFlow<List<SectionFieldElement>> = cardPillElement.mapAsStateFlow { cardPillElement ->
        buildList {
            nameElement?.let { add(it) }

            cardPillElement?.let {
                add(it)
                add(cvcElement)
            } ?: run {
                add(numberElement)

                val fields = listOf(expirationDateElement, cvcElement)

                add(
                    RowElement(
                        IdentifierSpec.Generic("card_details_row"),
                        fields,
                        RowController(fields)
                    )
                )
            }
        }
    }

    override val validationMessage = combineAsStateFlow(
        listOfNotNull(
            nameElement,
            numberElement,
            expirationDateElement,
            cvcElement
        )
            .map { it.controller }
            .map { it.validationMessage }
    ) {
        it.sortedWith(validationMessageComparator).filterNotNull().firstOrNull()
    }

    override fun onValidationStateChanged(isValidating: Boolean) {
        fields.value.forEach {
            it.onValidationStateChanged(isValidating)
        }
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?
    ) {
        CardDetailsElementUI(
            enabled,
            this,
            hiddenIdentifiers,
            lastTextFieldIdentifier,
            modifier = modifier,
        )
    }

    private fun dismissCardPill() {
        numberElement.controller.onRawValueChange("")
        expirationDateElement.controller.onRawValueChange("")
        cardPillElement.value = null
    }

    private fun formatExpirationDate(
        expirationMonth: Int,
        expirationYear: Int,
    ): String {
        return "%02d%02d".format(expirationMonth, expirationYear % YEAR_REMAINDER)
    }

    private companion object {
        const val YEAR_REMAINDER = 100
    }
}

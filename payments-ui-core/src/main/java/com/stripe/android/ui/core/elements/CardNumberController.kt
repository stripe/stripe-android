package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.cards.DefaultStaticCardAccountRanges
import com.stripe.android.cards.StaticCardAccountRanges
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.asIndividualDigits
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.SectionFieldErrorController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import com.stripe.android.R as PaymentsCoreR

internal sealed class CardNumberController : TextFieldController, SectionFieldErrorController {
    abstract val cardBrandFlow: Flow<CardBrand>

    abstract val selectedCardBrandFlow: Flow<CardBrand>

    abstract val cardScanEnabled: Boolean

    @OptIn(ExperimentalComposeUiApi::class)
    override val autofillType: AutofillType = AutofillType.CreditCardNumber

    fun onCardScanResult(cardScanSheetResult: CardScanSheetResult) {
        // Don't need to populate the card number if the result is Canceled or Failed
        if (cardScanSheetResult is CardScanSheetResult.Completed) {
            onRawValueChange(cardScanSheetResult.scannedCard.pan)
        }
    }
}

/*
 * TODO(samer-stripe): There is a lot of merging of card brand logic with `AccountRangeService` &
 *  `CardBrand.getCardBrands`. Look into merging Account Service and Card Brand logic.
 */
internal class DefaultCardNumberController(
    private val cardTextFieldConfig: CardNumberConfig,
    cardAccountRangeRepository: CardAccountRangeRepository,
    uiContext: CoroutineContext,
    workContext: CoroutineContext,
    staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges(),
    initialValue: String?,
    override val showOptionalLabel: Boolean = false,
    private val cardBrandChoiceConfig: CardBrandChoiceConfig = CardBrandChoiceConfig.Ineligible,
) : CardNumberController() {
    constructor(
        cardTextFieldConfig: CardNumberConfig,
        context: Context,
        initialValue: String?,
        cardBrandChoiceConfig: CardBrandChoiceConfig,
    ) : this(
        cardTextFieldConfig,
        DefaultCardAccountRangeRepositoryFactory(context).create(),
        Dispatchers.Main,
        Dispatchers.IO,
        initialValue = initialValue,
        cardBrandChoiceConfig = cardBrandChoiceConfig,
    )

    override val capitalization: KeyboardCapitalization = cardTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = cardTextFieldConfig.keyboard
    override val visualTransformation = cardTextFieldConfig.visualTransformation
    override val debugLabel = cardTextFieldConfig.debugLabel

    override val label: Flow<Int> = MutableStateFlow(cardTextFieldConfig.label)

    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: Flow<String> = _fieldValue

    override val rawFieldValue: Flow<String> =
        _fieldValue.map { cardTextFieldConfig.convertToRaw(it) }

    // This makes the screen reader read out numbers digit by digit
    override val contentDescription: Flow<String> = _fieldValue.map { it.asIndividualDigits() }

    private val isEligibleForCardBrandChoice = cardBrandChoiceConfig is CardBrandChoiceConfig.Eligible
    private val brandChoices = MutableStateFlow<List<CardBrand>>(listOf())

    private val preferredBrands = when (cardBrandChoiceConfig) {
        is CardBrandChoiceConfig.Eligible -> cardBrandChoiceConfig.preferredBrands
        is CardBrandChoiceConfig.Ineligible -> listOf()
    }

    /*
     * This flow is keeping track of whatever brand the user had selected from the dropdown menu
     * or from their most recent selection through `initialBrand` regardless of whether their
     * card number has changed.
     *
     * This will allow us re-reference the previously selected choice if the user changes the card
     * number.
     */
    private val mostRecentUserSelectedBrand = MutableStateFlow(
        when (cardBrandChoiceConfig) {
            is CardBrandChoiceConfig.Eligible -> cardBrandChoiceConfig.initialBrand
            is CardBrandChoiceConfig.Ineligible -> null
        }
    )

    override val selectedCardBrandFlow: Flow<CardBrand> = mostRecentUserSelectedBrand.combine(
        brandChoices
    ) { previous, choices ->
        when (previous) {
            CardBrand.Unknown -> previous
            in choices -> previous ?: CardBrand.Unknown
            else -> {
                val firstAvailablePreferred = preferredBrands.firstOrNull { it in choices }

                firstAvailablePreferred ?: CardBrand.Unknown
            }
        }
    }

    /*
     * In state validation, we check that the card number itself is valid and do not care about
     * the card's co-brands. If a session is card brand choice eligible however, there is now the
     * option  of not determining the card brand unless the user selects one. We use an implied
     * card brand (VISA, Mastercard) internally to pass state validation.
     */
    private val impliedCardBrand = _fieldValue.map {
        accountRangeService.accountRange?.brand
            ?: CardBrand.getCardBrands(it).firstOrNull()
            ?: CardBrand.Unknown
    }

    override val cardBrandFlow = if (isEligibleForCardBrandChoice) {
        combine(
            brandChoices,
            selectedCardBrandFlow
        ) { choices, selected ->
            choices.singleOrNull() ?: selected
        }
    } else {
        impliedCardBrand
    }

    override val cardScanEnabled = true

    @VisibleForTesting
    val accountRangeService = CardAccountRangeService(
        cardAccountRangeRepository,
        uiContext,
        workContext,
        staticCardAccountRanges,
        object : CardAccountRangeService.AccountRangeResultListener {
            override fun onAccountRangesResult(accountRanges: List<AccountRange>) {
                val newAccountRange = accountRanges.firstOrNull()
                newAccountRange?.panLength?.let { panLength ->
                    (visualTransformation as CardNumberVisualTransformation).binBasedMaxPan =
                        panLength
                }

                val newBrandChoices = accountRanges.map { it.brand }.distinct()

                brandChoices.value = newBrandChoices
            }
        },
        isCbcEligible = { isEligibleForCardBrandChoice },
    )

    override val trailingIcon: Flow<TextFieldIcon?> = combine(
        _fieldValue,
        brandChoices,
        selectedCardBrandFlow
    ) { number, brands, chosen ->
        if (isEligibleForCardBrandChoice && number.isNotEmpty()) {
            val noSelection = TextFieldIcon.Dropdown.Item(
                id = CardBrand.Unknown.code,
                label = resolvableString(PaymentsCoreR.string.stripe_card_brand_choice_no_selection),
                icon = CardBrand.Unknown.icon
            )

            val selected = if (brands.size == 1) {
                val onlyAvailableBrand = brands[0]

                TextFieldIcon.Dropdown.Item(
                    id = onlyAvailableBrand.code,
                    label = resolvableString(onlyAvailableBrand.displayName),
                    icon = onlyAvailableBrand.icon
                )
            } else {
                when (chosen) {
                    CardBrand.Unknown -> null
                    else -> TextFieldIcon.Dropdown.Item(
                        id = chosen.code,
                        label = resolvableString(chosen.displayName),
                        icon = chosen.icon
                    )
                }
            }

            val items = brands.map { brand ->
                TextFieldIcon.Dropdown.Item(
                    id = brand.code,
                    label = resolvableString(brand.displayName),
                    icon = brand.icon
                )
            }

            TextFieldIcon.Dropdown(
                title = resolvableString(PaymentsCoreR.string.stripe_card_brand_choice_selection_header),
                currentItem = selected ?: noSelection,
                items = items,
                hide = brands.size < 2
            )
        } else if (accountRangeService.accountRange != null) {
            TextFieldIcon.Trailing(accountRangeService.accountRange!!.brand.icon, isTintable = false)
        } else {
            val cardBrands = CardBrand.getCardBrands(number)

            val staticIcons = cardBrands.map { cardBrand ->
                TextFieldIcon.Trailing(cardBrand.icon, isTintable = false)
            }.take(STATIC_ICON_COUNT)

            val animatedIcons = cardBrands.map { cardBrand ->
                TextFieldIcon.Trailing(cardBrand.icon, isTintable = false)
            }.drop(STATIC_ICON_COUNT)

            TextFieldIcon.MultiTrailing(
                staticIcons = staticIcons,
                animatedIcons = animatedIcons
            )
        }
    }.distinctUntilChanged()

    private val _fieldState = combine(impliedCardBrand, _fieldValue) { brand, fieldValue ->
        cardTextFieldConfig.determineState(
            brand,
            fieldValue,
            accountRangeService.accountRange?.panLength ?: brand.getMaxLengthForCardNumber(
                fieldValue
            )
        )
    }
    override val fieldState: Flow<TextFieldState> = _fieldState

    private val _hasFocus = MutableStateFlow(false)

    override val loading: Flow<Boolean> = accountRangeService.isLoading

    override val visibleError: Flow<Boolean> =
        combine(_fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }

    /**
     * An error must be emitted if it is visible or not visible.
     **/
    override val error: Flow<FieldError?> =
        combine(visibleError, _fieldState) { visibleError, fieldState ->
            fieldState.getError()?.takeIf { visibleError }
        }

    override val isComplete: Flow<Boolean> = _fieldState.map { it.isValid() }

    override val formFieldValue: Flow<FormFieldEntry> =
        combine(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    init {
        onRawValueChange(initialValue ?: "")
    }

    /**
     * This is called when the value changed to is a display value.
     */
    override fun onValueChange(displayFormatted: String): TextFieldState? {
        _fieldValue.value = cardTextFieldConfig.filter(displayFormatted)
        val cardNumber = CardNumber.Unvalidated(displayFormatted)
        accountRangeService.onCardNumberChanged(cardNumber)

        return null
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        onValueChange(cardTextFieldConfig.convertFromRaw(rawValue))
    }

    override fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }

    override fun onDropdownItemClicked(item: TextFieldIcon.Dropdown.Item) {
        mostRecentUserSelectedBrand.value = CardBrand.fromCode(item.id)
    }

    private companion object {
        const val STATIC_ICON_COUNT = 3
    }
}

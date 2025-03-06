package com.stripe.android.ui.core.elements

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultStaticCardAccountRanges
import com.stripe.android.cards.StaticCardAccountRanges
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.events.LocalCardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.asIndividualDigits
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.coroutines.CoroutineContext
import com.stripe.android.R as PaymentsCoreR

internal sealed class CardNumberController : TextFieldController {
    abstract val cardBrandFlow: StateFlow<CardBrand>

    abstract val selectedCardBrandFlow: StateFlow<CardBrand>

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
    override val initialValue: String?,
    override val showOptionalLabel: Boolean = false,
    private val cardBrandChoiceConfig: CardBrandChoiceConfig = CardBrandChoiceConfig.Ineligible,
    private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
) : CardNumberController() {
    override val capitalization: KeyboardCapitalization = cardTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = cardTextFieldConfig.keyboard
    override val debugLabel = cardTextFieldConfig.debugLabel

    override val label: StateFlow<Int> = stateFlowOf(cardTextFieldConfig.label)

    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: StateFlow<String> = _fieldValue.asStateFlow()

    private val latestBinBasedPanLength = MutableStateFlow<Int?>(null)

    override val visualTransformation = combineAsStateFlow(
        fieldValue,
        latestBinBasedPanLength
    ) { number, latestBinBasedPanLength ->
        val panLength = latestBinBasedPanLength ?: CardBrand
            .fromCardNumber(number)
            .getMaxLengthForCardNumber(number)

        cardTextFieldConfig.determineVisualTransformation(number, panLength)
    }

    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override val rawFieldValue: StateFlow<String> =
        _fieldValue.mapAsStateFlow { cardTextFieldConfig.convertToRaw(it) }

    // This makes the screen reader read out numbers digit by digit
    override val contentDescription: StateFlow<ResolvableString> = _fieldValue.mapAsStateFlow {
        it.asIndividualDigits().resolvableString
    }

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

    override val selectedCardBrandFlow: StateFlow<CardBrand> = combineAsStateFlow(
        mostRecentUserSelectedBrand,
        brandChoices,
    ) { previous, allChoices ->
        determineSelectedBrand(previous, allChoices, cardBrandFilter, preferredBrands)
    }

    /*
     * In state validation, we check that the card number itself is valid and do not care about
     * the card's co-brands. If a session is card brand choice eligible however, there is now the
     * option  of not determining the card brand unless the user selects one. We use an implied
     * card brand (VISA, Mastercard) internally to pass state validation.
     */
    private val impliedCardBrand = _fieldValue.mapAsStateFlow {
        accountRangeService.accountRange?.brand
            ?: CardBrand.getCardBrands(it).firstOrNull()
            ?: CardBrand.Unknown
    }

    override val cardBrandFlow = if (isEligibleForCardBrandChoice) {
        combineAsStateFlow(
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
            override fun onAccountRangesResult(
                accountRanges: List<AccountRange>,
                unfilteredAccountRanges: List<AccountRange>
            ) {
                val newAccountRange = accountRanges.firstOrNull()
                newAccountRange?.panLength?.let { panLength ->
                    latestBinBasedPanLength.value = panLength
                }

                val newBrandChoices = unfilteredAccountRanges.map { it.brand }.distinct()

                brandChoices.value = newBrandChoices
            }
        },
        isCbcEligible = { isEligibleForCardBrandChoice },
        cardBrandFilter = cardBrandFilter
    )

    override val trailingIcon: StateFlow<TextFieldIcon?> = combineAsStateFlow(
        _fieldValue,
        brandChoices,
        selectedCardBrandFlow
    ) { number, brands, chosen ->
        if (isEligibleForCardBrandChoice && number.isNotEmpty()) {
            val noSelection = TextFieldIcon.Dropdown.Item(
                id = CardBrand.Unknown.code,
                label = PaymentsCoreR.string.stripe_card_brand_choice_no_selection.resolvableString,
                icon = CardBrand.Unknown.icon
            )

            val selected = if (brands.size == 1) {
                val onlyAvailableBrand = brands[0]

                TextFieldIcon.Dropdown.Item(
                    id = onlyAvailableBrand.code,
                    label = onlyAvailableBrand.displayName.resolvableString,
                    icon = onlyAvailableBrand.icon
                )
            } else {
                when (chosen) {
                    CardBrand.Unknown -> null
                    else -> TextFieldIcon.Dropdown.Item(
                        id = chosen.code,
                        label = chosen.displayName.resolvableString,
                        icon = chosen.icon
                    )
                }
            }

            val items = brands.map { brand ->
                val enabled = cardBrandFilter.isAccepted(brand)
                TextFieldIcon.Dropdown.Item(
                    id = brand.code,
                    label = if (enabled) {
                        brand.displayName.resolvableString
                    } else {
                        resolvableString(
                            R.string.stripe_card_brand_not_accepted_with_brand,
                            brand.displayName
                        )
                    },
                    icon = brand.icon,
                    enabled = enabled
                )
            }

            TextFieldIcon.Dropdown(
                title = PaymentsCoreR.string.stripe_card_brand_choice_selection_header.resolvableString,
                currentItem = selected ?: noSelection,
                items = items,
                hide = brands.size < 2
            )
        } else if (accountRangeService.accountRange != null) {
            TextFieldIcon.Trailing(accountRangeService.accountRange!!.brand.icon, isTintable = false)
        } else {
            val cardBrands = CardBrand.getCardBrands(number).filter { cardBrandFilter.isAccepted(it) }

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
    }

    private val _fieldState = combineAsStateFlow(impliedCardBrand, _fieldValue) { brand, fieldValue ->
        cardTextFieldConfig.determineState(
            brand,
            fieldValue,
            accountRangeService.accountRange?.panLength ?: brand.getMaxLengthForCardNumber(
                fieldValue
            )
        )
    }
    override val fieldState: StateFlow<TextFieldState> = _fieldState

    private val _hasFocus = MutableStateFlow(false)

    override val loading: StateFlow<Boolean> = accountRangeService.isLoading

    override val visibleError: StateFlow<Boolean> =
        combineAsStateFlow(_fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }

    /**
     * An error must be emitted if it is visible or not visible.
     **/
    override val error: StateFlow<FieldError?> =
        combineAsStateFlow(visibleError, _fieldState) { visibleError, fieldState ->
            fieldState.getError()?.takeIf { visibleError }
        }

    override val isComplete: StateFlow<Boolean> = _fieldState.mapAsStateFlow { it.isValid() }

    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
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

    fun determineSelectedBrand(
        previous: CardBrand?,
        allChoices: List<CardBrand>,
        cardBrandFilter: CardBrandFilter,
        preferredBrands: List<CardBrand>
    ): CardBrand {
        // Determine which of the available brands are not blocked
        val allowedChoices = allChoices.filter { cardBrandFilter.isAccepted(it) }

        return if (allowedChoices.size == 1 && allChoices.size > 1) {
            allowedChoices.single()
        } else {
            when (previous) {
                CardBrand.Unknown -> previous
                in allChoices -> previous ?: CardBrand.Unknown
                else -> {
                    val firstAvailablePreferred = preferredBrands.firstOrNull { it in allChoices }
                    firstAvailablePreferred ?: CardBrand.Unknown
                }
            }
        }
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
        val reporter = LocalCardNumberCompletedEventReporter.current
        val disallowedBrandReporter = LocalCardBrandDisallowedReporter.current

        // Remember the last state indicating whether it was a disallowed card brand error
        var lastLoggedCardBrand by rememberSaveable { mutableStateOf<CardBrand?>(null) }

        LaunchedEffect(Unit) {
            // Drop the set empty value & initial value
            fieldState.drop(1).collectLatest { state ->
                when (state) {
                    is TextFieldStateConstants.Valid.Full -> {
                        reporter.onCardNumberCompleted()
                        lastLoggedCardBrand = null // Reset when valid
                    }
                    is TextFieldStateConstants.Error.Invalid -> {
                        val error = state.getError()
                        val isDisallowedError = error?.errorMessage == PaymentsCoreR.string.stripe_disallowed_card_brand
                        if (isDisallowedError && lastLoggedCardBrand != impliedCardBrand.value) {
                            disallowedBrandReporter.onDisallowedCardBrandEntered(impliedCardBrand.value)
                            lastLoggedCardBrand = impliedCardBrand.value
                        }
                    }
                    else -> {
                        lastLoggedCardBrand = null // Reset for other states
                    }
                }
            }
        }

        super.ComposeUI(
            enabled,
            field,
            modifier,
            hiddenIdentifiers,
            lastTextFieldIdentifier,
            nextFocusDirection,
            previousFocusDirection
        )
    }

    private companion object {
        const val STATIC_ICON_COUNT = 3
    }
}

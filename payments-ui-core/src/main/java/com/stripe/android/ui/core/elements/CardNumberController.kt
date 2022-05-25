package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.cards.DefaultStaticCardAccountRanges
import com.stripe.android.cards.StaticCardAccountRanges
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

internal class CardNumberController constructor(
    private val cardTextFieldConfig: CardNumberConfig,
    cardAccountRangeRepository: CardAccountRangeRepository,
    workContext: CoroutineContext,
    staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges(),
    initialValue: String?,
    override val showOptionalLabel: Boolean = false
) : TextFieldController, SectionFieldErrorController {

    @JvmOverloads
    constructor(
        cardTextFieldConfig: CardNumberConfig,
        context: Context,
        initialValue: String?
    ) : this(
        cardTextFieldConfig,
        DefaultCardAccountRangeRepositoryFactory(context).create(),
        Dispatchers.IO,
        initialValue = initialValue
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

    override val contentDescription: Flow<String> = _fieldValue

    internal val cardBrandFlow = _fieldValue.map {
        accountRangeService.accountRange?.brand ?: CardBrand.getCardBrands(it).firstOrNull()
            ?: CardBrand.Unknown
    }

    override val trailingIcon: Flow<TextFieldIcon?> = _fieldValue.map {
        val cardBrands = CardBrand.getCardBrands(it)
        if (accountRangeService.accountRange != null) {
            TextFieldIcon.Trailing(accountRangeService.accountRange!!.brand.icon, isIcon = false)
        } else {
            val staticIcons = cardBrands.map { cardBrand ->
                TextFieldIcon.Trailing(cardBrand.icon, isIcon = false)
            }.filterIndexed { index, _ -> index < 3 }

            val animatedIcons = cardBrands.map { cardBrand ->
                TextFieldIcon.Trailing(cardBrand.icon, isIcon = false)
            }.filterIndexed { index, _ -> index > 2 }

            TextFieldIcon.MultiTrailing(
                staticIcons = staticIcons,
                animatedIcons = animatedIcons
            )
        }
    }

    private val _fieldState = combine(cardBrandFlow, _fieldValue) { brand, fieldValue ->
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

    @VisibleForTesting
    val accountRangeService = CardAccountRangeService(
        cardAccountRangeRepository,
        workContext,
        staticCardAccountRanges,
        object : CardAccountRangeService.AccountRangeResultListener {
            override fun onAccountRangeResult(newAccountRange: AccountRange?) {
                newAccountRange?.panLength?.let { panLength ->
                    (visualTransformation as CardNumberVisualTransformation).binBasedMaxPan =
                        panLength
                }
            }
        }
    )

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

    internal fun onCardScanResult(cardScanSheetResult: CardScanSheetResult) {
        // Don't need to populate the card number if the result is Canceled or Failed
        if (cardScanSheetResult is CardScanSheetResult.Completed) {
            onRawValueChange(cardScanSheetResult.scannedCard.pan)
        }
    }
}

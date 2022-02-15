package com.stripe.android.ui.core.elements

import android.content.Context
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
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

internal class CardNumberController constructor(
    private val cardTextFieldConfig: CardNumberConfig,
    override val cardAccountRangeRepository: CardAccountRangeRepository,
    override val workContext: CoroutineContext,
    private val staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges(),
    override val showOptionalLabel: Boolean = false
) : TextFieldController, SectionFieldErrorController, CardAccountRangeService {

    @JvmOverloads
    constructor(
        cardTextFieldConfig: CardNumberConfig,
        context: Context
    ) : this(
        cardTextFieldConfig,
        DefaultCardAccountRangeRepositoryFactory(context).create(),
        Dispatchers.IO
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

    internal val cardBrandFlow = _fieldValue.map {
        CardBrand.getCardBrands(it).firstOrNull() ?: CardBrand.Unknown
    }

    private val _fieldState = combine(cardBrandFlow, _fieldValue) { brand, fieldValue ->
        cardTextFieldConfig.determineState(
            brand,
            fieldValue,
            accountRange?.panLength ?: brand.getMaxLengthForCardNumber(fieldValue)
        )
    }
    override val fieldState: Flow<TextFieldState> = _fieldState

    private val _hasFocus = MutableStateFlow(false)

    override var accountRange: AccountRange? = null
    override var accountRangeRepositoryJob: Job? = null

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
        onValueChange("")
    }

    /**
     * This is called when the value changed to is a display value.
     */
    override fun onValueChange(displayFormatted: String) {
        _fieldValue.value = cardTextFieldConfig.filter(displayFormatted)
        val cardNumber = CardNumber.Unvalidated(displayFormatted)
        val staticAccountRange = staticCardAccountRanges.filter(cardNumber)
            .let { accountRanges ->
                if (accountRanges.size == 1) {
                    accountRanges.first()
                } else {
                    null
                }
            }
        if (staticAccountRange == null || shouldQueryRepository(staticAccountRange)) {
            // query for AccountRange data
            queryAccountRangeRepository(cardNumber)
        } else {
            // use static AccountRange data
            onAccountRangeResult(staticAccountRange)
        }
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

    override fun onAccountRangeResult(newAccountRange: AccountRange?) {
        super.onAccountRangeResult(newAccountRange)
        (visualTransformation as CardNumberVisualTransformation).binBasedMaxPan = 19
    }
}

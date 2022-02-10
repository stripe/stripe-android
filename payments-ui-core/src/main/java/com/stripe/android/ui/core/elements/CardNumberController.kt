package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.cards.DefaultStaticCardAccountRanges
import com.stripe.android.cards.StaticCardAccountRanges
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CardNumberController constructor(
    private val cardTextFieldConfig: CardNumberConfig,
    context: Context,
    override val showOptionalLabel: Boolean = false
) : TextFieldController, SectionFieldErrorController {
    private val cardAccountRangeRepository = DefaultCardAccountRangeRepositoryFactory(context).create()
    private val staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges()
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

    private var accountRange: AccountRange? = null

    @VisibleForTesting
    internal var accountRangeRepositoryJob: Job? = null

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

    private fun shouldQueryRepository(
        accountRange: AccountRange
    ) = when (accountRange.brand) {
        CardBrand.Unknown,
        CardBrand.UnionPay -> true
        else -> false
    }

    @JvmSynthetic
    internal fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) {
        if (shouldQueryAccountRange(cardNumber)) {
            // cancel in-flight job
            cancelAccountRangeRepositoryJob()

            // invalidate accountRange before fetching
            accountRange = null

            accountRangeRepositoryJob = CoroutineScope(Dispatchers.IO).launch {
                val bin = cardNumber.bin
                val accountRange = if (bin != null) {
                    cardAccountRangeRepository.getAccountRange(cardNumber)
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    onAccountRangeResult(accountRange)
                }
            }
        }
    }

    private fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    @JvmSynthetic
    internal fun onAccountRangeResult(
        newAccountRange: AccountRange?
    ) {
        accountRange = newAccountRange
        (visualTransformation as CardNumberVisualTransformation).maxPanLength = 19
    }

    private fun shouldQueryAccountRange(cardNumber: CardNumber.Unvalidated): Boolean {
        return accountRange == null ||
            cardNumber.bin == null ||
            accountRange?.binRange?.matches(cardNumber) == false
    }
}

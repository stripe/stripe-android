package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.elements.CardDetailsUtil
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.uicore.elements.canAcceptInput
import com.stripe.android.uicore.forms.FormFieldEntry

@Immutable
internal data class ExpiryDateState(
    val text: String,
    val enabled: Boolean,
    private val dateConfig: DateConfig = DateConfig()
) {

    private val textFieldState: TextFieldState = run {
        if (text == CARD_EDIT_UI_FALLBACK_EXPIRY_DATE) {
            TextFieldStateConstants.Error.Blank
        } else {
            dateConfig.determineState(text)
        }
    }

    private val formFieldValues = run {
        if (textFieldState.isValid()) {
            CardDetailsUtil.createExpiryDateFormFieldValues(FormFieldEntry(text))
        } else {
            null
        }
    }

    val expiryMonth: Int?
        get() = formFieldValues?.toIntOrNull(
            key = IdentifierSpec.CardExpMonth,
            min = JANUARY,
            max = DECEMBER,
        )

    val expiryYear: Int?
        get() = formFieldValues?.toIntOrNull(
            key = IdentifierSpec.CardExpYear,
            min = YEAR_2000,
            max = YEAR_2100,
        )

    fun shouldShowError(): Boolean {
        return textFieldState.shouldShowError(hasFocus = true)
    }

    @SuppressWarnings("SpreadOperator")
    fun sectionError(): ResolvableString? {
        val hasError = textFieldState.shouldShowError(hasFocus = true)
        return textFieldState.getError()?.takeIf {
            hasError && enabled
        }?.let { error ->
            resolvableString(
                id = error.errorMessage,
                formatArgs = error.formatArgs.orEmpty()
            )
        }
    }

    fun onDateChanged(proposedValue: String): ExpiryDateState {
        val canAcceptInput = textFieldState.canAcceptInput(
            currentValue = text,
            proposedValue = proposedValue
        )
        if (canAcceptInput.not()) return this
        return copy(text = proposedValue)
    }

    private fun Map<IdentifierSpec, FormFieldEntry>.toIntOrNull(
        key: IdentifierSpec,
        min: Int,
        max: Int
    ): Int? {
        return get(key)?.value?.toIntOrNull()?.takeIf {
            it in min..max
        }
    }

    companion object {
        fun create(
            card: PaymentMethod.Card,
            enabled: Boolean
        ): ExpiryDateState {
            val text = formattedExpiryDate(
                expiryMonth = card.expiryMonth,
                expiryYear = card.expiryYear,
                enabled = enabled
            )
            return ExpiryDateState(
                text = text,
                enabled = enabled
            )
        }
    }
}

private fun formattedExpiryDate(
    expiryMonth: Int?,
    expiryYear: Int?,
    enabled: Boolean
): String {
    @Suppress("ComplexCondition")
    if (
        enabled.not() &&
        (monthIsInvalid(expiryMonth) || yearIsInvalid(expiryYear))
    ) {
        return CARD_EDIT_UI_FALLBACK_EXPIRY_DATE
    }

    val formattedExpiryMonth = when {
        expiryMonth == null || monthIsInvalid(expiryMonth) -> {
            "00"
        }
        expiryMonth < OCTOBER -> {
            "0$expiryMonth"
        }
        else -> {
            expiryMonth.toString()
        }
    }

    val formattedExpiryYear = when {
        expiryYear == null || yearIsInvalid(expiryYear) -> {
            "00"
        }
        else -> {
            @Suppress("MagicNumber")
            expiryYear.toString().substring(2, 4)
        }
    }

    return "$formattedExpiryMonth$formattedExpiryYear"
}

private fun monthIsInvalid(expiryMonth: Int?): Boolean {
    return expiryMonth == null || expiryMonth < JANUARY || expiryMonth > DECEMBER
}

private fun yearIsInvalid(expiryYear: Int?): Boolean {
    // Since we use 2-digit years to represent the expiration year, we should keep dates to
    // this century.
    return expiryYear == null || expiryYear < YEAR_2000 || expiryYear > YEAR_2100
}

private const val JANUARY = 1
private const val OCTOBER = 10
private const val DECEMBER = 12
private const val YEAR_2000 = 2000
private const val YEAR_2100 = 2100

package com.stripe.form.fields.card

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.stripe.android.model.CardBrand
import com.stripe.form.ContentBox
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.ValidationResult
import com.stripe.form.ValueChange
import com.stripe.form.fields.TextFieldSpec
import com.stripe.form.parcelableKey
import com.stripe.form.text.ResolvableTextSpec
import com.stripe.android.R as StripeR

data class CvcSpec(
    override val state: State
) : FormFieldSpec<String> {

    @Composable
    override fun Content(modifier: Modifier) {
        ContentBox(
            modifier = modifier,
            spec = TextFieldSpec(
                state = TextFieldSpec.TextFieldState(
                    key = state.key,
                    label = when (state.cardBrand) {
                        CardBrand.AmericanExpress -> {
                            ResolvableTextSpec(StripeR.string.stripe_cvc_amex_hint)
                        }
                        else -> {
                            ResolvableTextSpec(StripeR.string.stripe_cvc_number_hint)
                        }
                    },
                    initialValue = TextFieldValue(state.initialValue),
                    validator = {
                        state.validator(it.text)
                    },
                    onValueChange = { change ->
                        state.onValueChange(
                            ValueChange(
                                key = change.key,
                                value = change.value.text,
                                isComplete = change.isComplete
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    maxLength = state.cardBrand.maxCvcLength
                )
            )
        )
    }

    data class State(
        val cardBrand: CardBrand,
        val initialValue: String = "",
        override val onValueChange: (ValueChange<String>) -> Unit,
        override val validator: (String) -> ValidationResult,
    ) : FormFieldState<String> {
        override val key = KEY
    }

    companion object {
        val KEY = parcelableKey("cvc")
    }
}

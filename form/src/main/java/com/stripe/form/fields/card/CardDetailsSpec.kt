package com.stripe.form.fields.card

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.android.CardUtils
import com.stripe.android.model.CardBrand
import com.stripe.form.ContentBox
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.Key
import com.stripe.form.ValidationResult
import com.stripe.form.Validator
import com.stripe.form.ValueChange
import kotlinx.parcelize.Parcelize

class CardDetailsSpec(
    override val state: State
) : FormFieldSpec<CardDetailsSpec.Output> {

    @Composable
    override fun Content(modifier: Modifier) {
        var cardBrand by remember(state) { mutableStateOf(CardBrand.Unknown) }
        var output by remember { mutableStateOf(Output()) }
        Column(
            modifier = modifier
        ) {
            ContentBox(
                modifier = Modifier
                    .fillMaxWidth(),
                spec = CardNumberSpec(
                    state = CardNumberSpec.State(
                        initialValue = state.cardNumber,
                        onValueChange = {
                            cardBrand = CardUtils.getPossibleCardBrand(it.value)
                            output = output.copy(cardNumberChange = it)
                        },
                        getCardBrand = {
                            cardBrand
                        }
                    )
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                ContentBox(
                    modifier = Modifier
                        .weight(1f),
                    spec = ExpiryDateSpec(
                        state = ExpiryDateSpec.State(
                            initialValue = state.expiryDate,
                            onValueChange = {
                                output = output.copy(expiryDateChange = it)
                            }
                        )
                    )
                )

                ContentBox(
                    modifier = Modifier
                        .weight(1f),
                    spec = CvcSpec(
                        state = CvcSpec.State(
                            initialValue = state.cvc,
                            cardBrand = cardBrand,
                            onValueChange = {
                                output = output.copy(cvcChange = it)
                            },
                            validator = { ValidationResult.Valid }
                        )
                    )
                )
            }
        }

        LaunchedEffect(output) {
            state.onValueChange(
                ValueChange(
                    key = state.key,
                    value = output,
                    isComplete = state.validator(output).isValid
                )
            )
        }
    }

    data class State(
        val cardNumber: String = "",
        val expiryDate: String = "",
        val cvc: String = "",
        override val key: Key<Output>,
        override val onValueChange: (ValueChange<Output>) -> Unit,
        override val validator: (Output) -> ValidationResult = { CardDetailsValidator.validateResult(it) },
    ) : FormFieldState<Output>

    @Parcelize
    data class Output(
        val cardNumberChange: ValueChange<String>? = null,
        val cvcChange: ValueChange<String>? = null,
        val expiryDateChange: ValueChange<String>? = null
    ) : Parcelable
}

private object CardDetailsValidator : Validator<CardDetailsSpec.Output> {
    override fun validateResult(value: CardDetailsSpec.Output): ValidationResult {
        val isValid =
            value.cardNumberChange?.isComplete == true && value.expiryDateChange?.isComplete == true && value.cvcChange?.isComplete == true
        if (isValid) return ValidationResult.Valid
        return ValidationResult.Invalid()
    }

}

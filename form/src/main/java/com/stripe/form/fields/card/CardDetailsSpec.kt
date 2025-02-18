package com.stripe.form.fields.card

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
import com.stripe.form.ValidationResult
import com.stripe.form.ValueChange
import com.stripe.form.parcelableKey

class CardDetailsSpec(
    override val state: State
) : FormFieldSpec<Any> {

    @Composable
    override fun Content(modifier: Modifier) {
        var cardBrand by remember(state) { mutableStateOf(CardBrand.Unknown) }
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
                            state.onValueChange(it as ValueChange<Any>)
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
                                state.onValueChange(it as ValueChange<Any>)
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
                                state.onValueChange(it as ValueChange<Any>)
                            },
                            validator = { ValidationResult.Valid }
                        )
                    )
                )
            }
        }
    }

    data class State(
        val cardNumber: String = "",
        val expiryDate: String = "",
        val cvc: String = "",
        override val key: Parcelable = parcelableKey(""),
        override val onValueChange: (ValueChange<Any>) -> Unit,
        override val validator: (Any) -> ValidationResult = { ValidationResult.Valid },
    ): FormFieldState<Any>
}

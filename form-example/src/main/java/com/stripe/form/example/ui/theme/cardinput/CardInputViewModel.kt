package com.stripe.form.example.ui.theme.cardinput

import androidx.lifecycle.ViewModel
import com.stripe.form.Key
import com.stripe.form.ValueChange
import com.stripe.form.buildForm
import com.stripe.form.fields.card.CardDetailsSpec
import com.stripe.form.find
import com.stripe.form.key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CardInputViewModel: ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    val key = key<CardDetailsSpec.Output>()

    init {
        val form = buildForm(
            onValuesChanged = ::onFormValuesChanged
        ) {
            cardDetails(
                key = key,
            )
        }
        _state.update {
            it.copy(form = form)
        }
    }

    private fun onFormValuesChanged(data: Map<Key<*>, ValueChange<*>?>) {
        val output = data.find(key)

        _state.update {
            it.copy(
                valid = output?.isComplete ?: false,
            )
        }

    }
}
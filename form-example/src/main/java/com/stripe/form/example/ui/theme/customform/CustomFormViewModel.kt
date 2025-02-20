package com.stripe.form.example.ui.theme.customform

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.stripe.form.Key
import com.stripe.form.ValueChange
import com.stripe.form.buildForm
import com.stripe.form.fields.DropdownSpec
import com.stripe.form.fields.card.CardDetailsSpec
import com.stripe.form.find
import com.stripe.form.key
import com.stripe.form.text.TextSpec
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CustomFormViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    val nameKey = key<TextFieldValue>("name")
    val sliderKey = key<Float>("slider")
    val checkboxKey = key<Boolean>("check")
    val dropdownKey = key<String>("drop")

    init {
        val form = buildForm(
            onValuesChanged = ::onFormValuesChanged
        ) {
            addSpec {
                WelcomeSpec(
                    names = persistentListOf(
                        "Stripe",
                        "Mobile",
                        "Elements"
                    )
                )
            }

            addSpec {
                TextSpec("What is your name?")
            }

            textField(
                key = nameKey
            )

            checkbox(
                key = checkboxKey,
                label = TextSpec("Do you like this API?")
            )

            addSpec {
                TextSpec("Where are you from?")
            }

            dropdown(
                key = dropdownKey,
                options = persistentListOf(
                    DropdownSpec.Option(
                        rawValue = "CA",
                        displayValue = "Canada 🇨🇦"
                    ),
                    DropdownSpec.Option(
                        rawValue = "US",
                        displayValue = "United States of America"
                    ),
                )
            )

            addSpec {
                TextSpec("On a scale of 1 - 10, 1 being ugly, 10 being very cute, rate moo deng")
            }

            addSpec { onValueChange ->
                CustomSliderSpec(
                    state = CustomSliderSpec.State(
                        key = sliderKey,
                        onValueChange = onValueChange
                    )
                )
            }

            addSpec {
                MooDengSpec()
            }
        }
        _state.update {
            it.copy(form = form)
        }
    }

    private fun onFormValuesChanged(data: Map<Key<*>, ValueChange<*>?>) {
        val nameChange = data.find(nameKey)
        val sliderChange = data.find(sliderKey)
        val checkChange = data.find(checkboxKey)
        val dropdownChange = data.find(dropdownKey)

        _state.update {
            it.copy(
                valid = (sliderChange?.isComplete ?: false) && (checkChange?.isComplete
                    ?: false) && (dropdownChange?.isComplete ?: false) && (nameChange?.isComplete ?: false),
            )
        }

    }
}
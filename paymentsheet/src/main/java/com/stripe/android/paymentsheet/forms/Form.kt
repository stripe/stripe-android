package com.stripe.android.paymentsheet.forms

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.common.Section
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// TODO: Country element type
// TODO: Manadate type
// TODO: Save for future usage.

@ExperimentalAnimationApi
@Composable
fun Form(
    formViewModel: FormViewModel,
) {
    val form = formViewModel.formDataObject
    val focusRequesters = List(form.sections.size) { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(16.dp)
    ) {
        form.sections.forEachIndexed { index, section ->
            val element = formViewModel.getElement(section.field)
            val error by element.errorMessage.asLiveData().observeAsState(null)
            val sectionErrorString =
                error?.let { stringResource(it, stringResource(element.label)) }

            Section(sectionErrorString) {
                TextField(
                    label = element.label,
                    textFieldElement = element,
                    myFocus = focusRequesters[index],
                    nextFocus = if (index == form.sections.size - 1) {
                        null
                    } else {
                        focusRequesters[index + 1]
                    },
                )
            }
        }
    }
}

class FormViewModel(
    val formDataObject: FormDataObject
) : ViewModel() {
    class Factory(
        val formDataObject: FormDataObject
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FormViewModel(formDataObject) as T
        }
    }

    private val paramKey: MutableMap<String, Any?> = formDataObject.paramKey
    private val types: List<Field> = formDataObject.allTypes
    private val elementMap = mutableMapOf<Field, TextFieldElement>()

    internal fun getElement(type: Field) = requireNotNull(elementMap[type])

    private val isComplete: Flow<Boolean>
    private val params: Flow<MutableMap<String, Any?>>
    val paramMapFlow: Flow<MutableMap<String, Any?>?>

    init {
        val listCompleteFlows = mutableListOf<Flow<Boolean>>()
        val listOfPairs = mutableListOf<Flow<Pair<String, String?>>>()
        types.forEach { type ->
            val element = when (type) {
                Field.NameInput -> TextFieldElement(NameConfig()) // All configs should have the label passed in for consistency
                Field.EmailInput -> TextFieldElement(EmailConfig())
            }
            listCompleteFlows.add(element.isComplete)
            listOfPairs.add(element.input.map { Pair(type.paymentMethodCreateParamsKey, it) })
            elementMap[type] = element
        }

        isComplete = combine(listCompleteFlows) { elementCompleteState ->
            elementCompleteState.none { complete -> !complete }
        }

        params = combine(listOfPairs) { allPairs ->
            val destMap = mutableMapOf<String, Any?>()
            createMap(paramKey, destMap, allPairs.toMap())
            destMap
        }

        paramMapFlow = combine(isComplete, params) { isComplete, params ->
            params.takeIf { isComplete }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createMap(
        source: Map<String, Any?>,
        dest: MutableMap<String, Any?>,
        elementKeys: Map<String, String?>
    ) {
        source.keys.forEach { key ->
            if (source[key] == null) {
                dest[key] = elementKeys[key]
            } else if (source[key] is MutableMap<*, *>) {
                val newDestMap = mutableMapOf<String, Any?>()
                dest[key] = newDestMap
                createMap(source[key] as MutableMap<String, Any?>, newDestMap, elementKeys)
            }
        }
    }
}

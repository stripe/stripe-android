package com.stripe.android.paymentsheet.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Composable
internal fun FormDataObjectReader(
    form: FormDataObject,
    formViewModel: FormViewModel,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(16.dp)
    ) {

        form.elements.forEachIndexed { index, element ->
            SingleField(
                formViewModel.getElement(element),
            )
        }
    }
}

@Composable
internal fun SingleField(
    element: TextFieldElement,
) {
    val label = element.label
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = 8.dp
    ) {
        TextField(
            label = label,
            textFieldElement = element,
            myFocus = FocusRequester(),
            nextFocus = null,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}


// TODO: Convert this to flows
// TODO: Country element type
// TODO: Manadate type
// TODO: Next focus
// TODO: Save for future usage.
internal class FormViewModel(
    private val paramKey: MutableMap<String, Any?>,
    types: List<Field>
) : ViewModel() {
    private val elementMap = mutableMapOf<Field, TextFieldElement>()

    fun getElement(type: Field) = requireNotNull(elementMap[type])

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

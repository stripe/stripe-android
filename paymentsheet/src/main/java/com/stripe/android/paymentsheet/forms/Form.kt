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
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// TODO: Convert this to flows
// TODO: Country element type
// TODO: Manadate type
// TODO: Next focus
// TODO: Save for future usage.

@ExperimentalAnimationApi
@Composable
internal fun Form(
    form: FormDataObject,
    formViewModel: FormViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(16.dp)
    ) {
        form.elements.forEach {
            when (it) {
                is Section -> {
                    val numberItemsInSection = it.fields.size
                    if (numberItemsInSection == 1) {
                        val contentType = it.fields[0]
                        SectionOneField(formViewModel.getElement(contentType))
                    } else {
                        SectionMultipleFields(
                            formViewModel,
                            it.labelResId,
                            it.fields
                        )
                    }
                }
                is Field -> {
                    SectionOneField(formViewModel.getElement(it))
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
internal fun SectionMultipleFields(
    formViewModel: FormViewModel,
    labelResId: Int,
    fields: List<Field>
) {
    com.stripe.android.paymentsheet.elements.Section(stringResource(labelResId), null) {
        Column {
            fields.forEach { contentType ->
                val element = formViewModel.getElement(contentType)
                val label = element.label

                TextField(
                    label = label,
                    textFieldElement = formViewModel.getElement(contentType),
                    myFocus = FocusRequester(),
                    nextFocus = null,
                )
            }
        }
    }
}

@Composable
internal fun SectionOneField(
    element: TextFieldElement,
) {
    val label = element.label
    val error by element.errorMessage.asLiveData().observeAsState(null)

    com.stripe.android.paymentsheet.elements.Section(stringResource(element.label), error) {
        Column {
            TextField(
                label = label,
                textFieldElement = element,
                myFocus = FocusRequester(),
                nextFocus = null,
            )
        }
    }
}

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

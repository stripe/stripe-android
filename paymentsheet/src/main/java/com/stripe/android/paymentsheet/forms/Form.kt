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
import com.stripe.android.paymentsheet.elements.common.DropDown
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.common.FieldController
import com.stripe.android.paymentsheet.elements.common.Section
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// TODO: Manadate type
// TODO: Save for future usage.

@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val form = formViewModel.formSpec

    // There is only a single field in a section, some of those might not require focus like
    // country so we will create an extra one
    var focusRequesterIndex = 0
    val focusRequesters = List(formViewModel.getNumberTextFieldElements()) { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(16.dp)
    ) {
        form.sectionSpecs.forEach { section ->
            val element = formViewModel.getController(section.sectionField)
            val error by element.errorMessage.asLiveData().observeAsState(null)
            val sectionErrorString =
                error?.let { stringResource(it, stringResource(element.label)) }

            Section(sectionErrorString) {
                if (element is TextFieldController) {
                    TextField(
                        textFieldController = element,
                        myFocus = focusRequesters[focusRequesterIndex],
                        nextFocus = if (focusRequesterIndex == focusRequesters.size - 1) {
                            null
                        } else {
                            focusRequesters[focusRequesterIndex + 1]
                        },
                    )
                    focusRequesterIndex++
                } else if (element is DropdownFieldController) {
                    DropDown(element)
                }
            }
        }
    }
}

class FormViewModel(
    val formSpec: FormSpec
) : ViewModel() {
    class Factory(
        val formSpec: FormSpec
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FormViewModel(formSpec) as T
        }
    }

    private val paramKey: MutableMap<String, Any?> = formSpec.paramKey
    private val types: List<SectionSpec.SectionFieldSpec> = formSpec.allTypes
    private val typeControllerMap = mutableMapOf<SectionSpec.SectionFieldSpec, FieldController>()
    fun getNumberTextFieldElements() = typeControllerMap.count { it.value is TextFieldController }

    internal fun getController(type: SectionSpec.SectionFieldSpec) =
        requireNotNull(typeControllerMap[type])

    private val isComplete: Flow<Boolean>
    val populatedFormData: Flow<FormData?>

    init {
        val listCompleteFlows = mutableListOf<Flow<Boolean>>()
        val listOfPairs = mutableListOf<Flow<Pair<String, String?>>>()
        types.forEach { type ->
            val element = when (type) {
                SectionSpec.SectionFieldSpec.Name -> TextFieldController(NameConfig()) // All configs should have the label passed in for consistency
                SectionSpec.SectionFieldSpec.Email -> TextFieldController(EmailConfig())
                SectionSpec.SectionFieldSpec.Country -> DropdownFieldController(CountryConfig())
            }
            listCompleteFlows.add(element.isComplete)
            listOfPairs.add(element.paymentMethodParams.map {
                Pair(
                    type.paymentMethodCreateParamsKey,
                    it
                )
            })
            typeControllerMap[type] = element
        }

        isComplete = combine(listCompleteFlows) { elementCompleteState ->
            elementCompleteState.none { complete -> !complete }
        }

        val formData = combine(listOfPairs) { allPairs ->
            FormData(paramKey, allPairs.toMap())
        }

        populatedFormData = combine(formData, isComplete) { populatedData, isComplete ->
            populatedData.takeIf { isComplete }
        }
    }
}

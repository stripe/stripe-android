package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import com.stripe.android.uicore.R
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckboxFieldController constructor(
    val labelResource: LabelResource? = null,
    val debugTag: String = DEFAULT_CHECKBOX_TEST_TAG,
    initialValue: Boolean = false,
) : SectionFieldErrorController, SectionFieldComposable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class LabelResource(
        @StringRes val labelId: Int,
        vararg args: Any
    ) {
        val formatArgs = args
    }

    private var hasBeenEdited = false
    private val _isChecked = MutableStateFlow(initialValue)
    val isChecked: StateFlow<Boolean>
        get() = _isChecked

    override val error: StateFlow<FieldError?> = _isChecked.mapAsStateFlow { value ->
        when {
            !value && hasBeenEdited -> FieldError(errorMessage = R.string.stripe_field_required)
            else -> null
        }
    }

    fun onValueChange(value: Boolean) {
        if (!hasBeenEdited) {
            hasBeenEdited = true
        }

        _isChecked.value = value
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    ) {
        CheckboxFieldUI(modifier = modifier, controller = this, enabled = enabled)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_CHECKBOX_TEST_TAG = "CHECKBOX_FIELD"

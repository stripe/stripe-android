package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldController(
    label: ResolvableString,
    private val onNavigation: (() -> Unit)? = null,
    val inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState>? = null,
    private val onInlinePredictionSelected: ((String) -> Unit)? = null,
    private val onInlineDismissed: (() -> Unit)? = null,
    private val onInlineEnterManually: (() -> Unit)? = null,
    private val getAttributionDrawable: ((Boolean) -> Int?)? = null,
) : InputController, SectionFieldValidationController, SectionFieldComposable {
    private val _isValidating = MutableStateFlow(false)

    val inlineQuery = MutableStateFlow("")

    override val showOptionalLabel: Boolean = false
    override val label = stateFlowOf(label)
    override val fieldValue: StateFlow<String> = stateFlowOf("")
    override val rawFieldValue: StateFlow<String> = stateFlowOf("")
    override val isComplete: StateFlow<Boolean> = stateFlowOf(false)

    override val validationMessage: StateFlow<FieldValidationMessage?> = _isValidating.mapAsStateFlow { isValidating ->
        FieldValidationMessage.Error(R.string.stripe_blank_and_required).takeIf { isValidating }
    }

    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    override fun onRawValueChange(rawValue: String) {
        // No-op, this field does not support direct input manipulation
    }

    fun onInlineQueryChanged(query: String) {
        inlineQuery.value = query
    }

    override fun onValidationStateChanged(isValidating: Boolean) {
        _isValidating.value = isValidating
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?
    ) {
        if (inlinePredictionsState != null) {
            var fieldWidthDp by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            androidx.compose.foundation.layout.Box(
                modifier = modifier
                    .wrapContentSize(Alignment.TopStart)
                    .onSizeChanged { size ->
                        fieldWidthDp = with(density) { size.width.toDp() }
                    }
            ) {
                AddressTextFieldUI(controller = this@AddressTextFieldController, enabled = enabled)
                val predictionsState by inlinePredictionsState.collectAsState()
                val isDarkTheme = isSystemInDarkTheme()
                val attributionDrawable = remember(isDarkTheme) {
                    getAttributionDrawable?.invoke(isDarkTheme)
                }
                InlineAddressPredictionsUI(
                    state = predictionsState,
                    attributionDrawable = attributionDrawable,
                    fieldWidthDp = fieldWidthDp,
                    onPredictionSelected = onInlinePredictionSelected ?: {},
                    onDismiss = onInlineDismissed ?: {},
                    onClear = { inlineQuery.value = "" },
                    onEnterManually = onInlineEnterManually,
                )
            }
        } else {
            AddressTextFieldUI(controller = this, enabled = enabled, modifier = modifier)
        }
    }

    fun launchAutocompleteScreen() {
        onNavigation?.invoke()
    }
}

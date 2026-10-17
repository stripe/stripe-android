package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.LocalFormScrollContext
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldController(
    label: ResolvableString,
    addressInputMode: AddressInputMode,
    private val inlineAutocompleteHandler: InlineAutocompleteHandler?,
    private val reportsFormValue: Boolean,
    initialQuery: String,
    private val showEnterManually: Boolean,
) : InputController, SectionFieldValidationController, SectionFieldComposable {
    private val _isValidating = MutableStateFlow(false)
    private val _inlineQuery = MutableStateFlow(initialQuery)
    private val onNavigation = (addressInputMode as? AddressInputMode.AutocompleteCondensed)?.onNavigation

    val isEditable = inlineAutocompleteHandler != null
    val inlineQuery: StateFlow<String> = _inlineQuery.asStateFlow()

    override val showOptionalLabel: Boolean = false
    override val label = stateFlowOf(label)
    override val fieldValue: StateFlow<String> =
        if (reportsFormValue) inlineQuery else stateFlowOf("")
    override val rawFieldValue: StateFlow<String> = fieldValue
    override val isComplete: StateFlow<Boolean> =
        if (reportsFormValue) _inlineQuery.mapAsStateFlow { it.isNotBlank() } else stateFlowOf(false)

    override val validationMessage: StateFlow<FieldValidationMessage?> =
        combineAsStateFlow(_isValidating, _inlineQuery) { isValidating, query ->
            FieldValidationMessage.Error(R.string.stripe_blank_and_required).takeIf {
                isValidating && (!reportsFormValue || query.isBlank())
            }
        }

    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    override fun onRawValueChange(rawValue: String) {
        if (reportsFormValue) {
            _inlineQuery.value = rawValue
        }
    }

    fun onInlineQueryChanged(query: String) {
        if (isEditable) {
            _inlineQuery.value = query
        }
    }

    override fun onValidationStateChanged(isValidating: Boolean) {
        _isValidating.value = isValidating
    }

    private fun onPredictionsDismissed() {
        // Inline mode clears the field on dismiss; expanded mode keeps the typed
        // text so the user doesn't lose their in-progress input.
        if (!reportsFormValue) {
            _inlineQuery.value = ""
        }
        inlineAutocompleteHandler?.onDismissed()
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<FormFieldId>,
        lastTextFieldIdentifier: FormFieldId?
    ) {
        if (inlineAutocompleteHandler != null) {
            val predictionsState by inlineAutocompleteHandler.predictionsState.collectAsState()
            val isDarkTheme = isSystemInDarkTheme()

            val predictions = @Composable {
                InlineAddressPredictionsUI(
                    state = predictionsState,
                    attributionDrawable = inlineAutocompleteHandler.getAttributionDrawable(isDarkTheme),
                    onPredictionSelected = inlineAutocompleteHandler::onPredictionSelected,
                    onClear = ::onPredictionsDismissed,
                    onEnterManually = if (showEnterManually) {
                        inlineAutocompleteHandler::onEnterManually
                    } else {
                        null
                    },
                )
            }

            val scrollContext = LocalFormScrollContext.current
            var elementScreenY by remember { mutableIntStateOf(0) }
            val showingPredictions = shouldShowPredictionsDropdown(predictionsState)
            LaunchedEffect(showingPredictions) {
                if (showingPredictions && scrollContext != null) {
                    snapshotFlow { elementScreenY }.filter { it > 0 }.first()
                    val target = scrollContext.scrollState.value +
                        elementScreenY - scrollContext.viewportTopY
                    scrollContext.scrollState.animateScrollTo(target)
                }
            }

            Column(
                modifier = modifier
                    .onGloballyPositioned { coordinates ->
                        elementScreenY = coordinates.positionInRoot().y.toInt()
                    }
                    .onFocusChanged { state ->
                        if (state.hasFocus) {
                            inlineAutocompleteHandler.onFocusGained()
                        } else {
                            inlineAutocompleteHandler.onFocusLost()
                        }
                    }
            ) {
                AddressTextFieldUI(
                    controller = this@AddressTextFieldController,
                    enabled = enabled,
                    onSearchActivated = if (reportsFormValue) inlineAutocompleteHandler::onSearchActivated else null,
                )
                predictions()
            }
        } else {
            AddressTextFieldUI(controller = this, enabled = enabled, onSearchActivated = null, modifier = modifier)
        }
    }

    fun launchAutocompleteScreen() {
        onNavigation?.invoke()
    }
}

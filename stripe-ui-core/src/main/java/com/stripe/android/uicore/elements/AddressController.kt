package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This is the controller for a section with a changing number and set of fields.
 * This is in contrast to the [SectionController] which is a section in which the fields
 * in it do not change.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressController(
    val fieldsFlowable: StateFlow<List<SectionFieldElement>>
) : SectionFieldErrorController, SectionFieldComposable {
    @StringRes
    val label: Int? = null

    override val error = fieldsFlowable.flatMapLatestAsStateFlow { sectionFieldElements ->
        combineAsStateFlow(
            sectionFieldElements.map { it.sectionFieldErrorController().error }
        ) { fieldErrors ->
            fieldErrors.filterNotNull().firstOrNull()
        }
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
        AddressElementUI(
            enabled,
            this,
            hiddenIdentifiers,
            lastTextFieldIdentifier
        )
    }
}

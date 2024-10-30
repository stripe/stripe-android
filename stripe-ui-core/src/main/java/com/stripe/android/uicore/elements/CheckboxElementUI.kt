package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.elements.menu.Checkbox
import androidx.compose.ui.R as ComposeUiR

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CheckboxElementUI(
    modifier: Modifier = Modifier,
    automationTestTag: String = "",
    isChecked: Boolean = false,
    label: String? = null,
    isEnabled: Boolean = false,
    onValueChange: (Boolean) -> Unit,
) {
    val accessibilityDescription = stringResource(
        if (isChecked) {
            ComposeUiR.string.selected
        } else {
            ComposeUiR.string.not_selected
        }
    )

    Row(
        modifier = modifier
            .semantics {
                testTag = automationTestTag
                stateDescription = accessibilityDescription
            }
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = onValueChange,
                enabled = isEnabled
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null, // needs to be null for accessibility on row click to work
            enabled = isEnabled
        )
        label?.let {
            H6Text(
                text = label,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

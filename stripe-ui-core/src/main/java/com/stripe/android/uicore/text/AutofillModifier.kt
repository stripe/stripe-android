package com.stripe.android.uicore.text

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.autofill(
    types: List<AutofillType>,
    onFill: (String) -> Unit,
): Modifier {
    val currentOnFill by rememberUpdatedState(onFill)

    val autofillNode = remember(types) {
        AutofillNode(
            autofillTypes = types,
            onFill = currentOnFill,
        )
    }

    val autofill = LocalAutofill.current
    LocalAutofillTree.current += autofillNode

    return onGloballyPositioned {
        autofillNode.boundingBox = it.boundsInWindow()
    }.onFocusChanged { focusState ->
        if (autofillNode.boundingBox != null) {
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
    }
}

package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColorScheme
import com.stripe.android.uicore.utils.collectAsState
import androidx.compose.ui.R as ComposeR

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CheckboxFieldUI(
    modifier: Modifier = Modifier,
    controller: CheckboxFieldController,
    enabled: Boolean = true
) {
    val isChecked by controller.isChecked.collectAsState()
    val validationMessage by controller.validationMessage.collectAsState()

    CheckboxFieldUIView(
        modifier = modifier,
        isChecked = isChecked,
        enabled = enabled,
        debugTag = controller.debugTag,
        onValueChange = controller::onValueChange,
        label = @ReadOnlyComposable {
            controller.labelResource?.let { resource ->
                stringResource(id = resource.labelId, formatArgs = resource.formatArgs)
            } ?: ""
        },
        error = @ReadOnlyComposable {
            validationMessage?.resolvable?.resolve()
        }
    )
}

@Composable
internal fun CheckboxFieldUIView(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    enabled: Boolean,
    debugTag: String,
    onValueChange: (value: Boolean) -> Unit,
    label: @Composable () -> String,
    error: (@Composable () -> String?)?,
) {
    val accessibilityDescription = stringResource(
        if (isChecked) {
            ComposeR.string.selected
        } else {
            ComposeR.string.not_selected
        }
    )

    val errorMessage = error?.invoke()
    val errorColor = MaterialTheme.stripeColorScheme.materialColorScheme.error

    val checkboxColors = error?.run {
        CheckboxDefaults.colors(
            checkedColor = errorColor,
            uncheckedColor = errorColor,
            checkmarkColor = errorColor
        )
    } ?: run {
        CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.stripeColorScheme.subtitle,
            checkmarkColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier.semantics {
            stateDescription = accessibilityDescription
        }
    ) {
        Row(
            modifier = Modifier
                .toggleable(
                    value = isChecked,
                    role = Role.Checkbox,
                    onValueChange = onValueChange,
                    enabled = enabled
                )
                .testTag(debugTag)
                .fillMaxWidth(),
        ) {
            Checkbox(
                modifier = Modifier.padding(end = 8.dp),
                checked = isChecked,
                // Needs to be null for accessibility on row click to work
                onCheckedChange = null,
                enabled = enabled,
                colors = checkboxColors
            )
            Text(
                text = label(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.stripeColorScheme.placeholderText
            )
        }
        errorMessage?.let { error ->
            Error(error = error, color = errorColor)
        }
    }
}

@Composable
private fun Error(
    error: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_material_info),
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
            tint = color
        )
        Text(
            text = error,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckboxFieldUIViewPreview() {
    val (value, updateValue) = remember {
        mutableStateOf(false)
    }

    StripeTheme {
        CheckboxFieldUIView(
            modifier = Modifier.padding(vertical = 8.dp),
            enabled = true,
            isChecked = value,
            debugTag = "",
            onValueChange = updateValue,
            label = @ReadOnlyComposable {
                "I understand that Stripe will be collecting Direct Debits on behalf of " +
                    "Test Business Name and confirm that I am the account holder and the only " +
                    "person required to authorise debits from this account."
            },
            error = null
        )
    }
}

package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.paymentsheet.ui.PromoBadge
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_HEADER_TITLE = "TEST_TAG_HEADER_TITLE"

@Composable
internal fun VerticalModeFormUI(
    interactor: VerticalModeFormInteractor,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = StripeTheme.getOuterFormInsets()

    var hasSentInteractionEvent by remember { mutableStateOf(false) }
    val state by interactor.state.collectAsState()

    Column(modifier) {
        val headerInformation = state.formHeader
        val enabled = !state.isProcessing
        if (headerInformation != null) {
            VerticalModeFormHeaderUI(isEnabled = enabled, formHeaderInformation = headerInformation)
        }

        FormElement(
            enabled = enabled,
            selectedPaymentMethodCode = state.selectedPaymentMethodCode,
            formElements = state.formUiElements,
            formArguments = state.formArguments,
            usBankAccountFormArguments = state.usBankAccountFormArguments,
            horizontalPaddingValues = horizontalPadding,
            onFormFieldValuesChanged = { formValues ->
                interactor.handleViewAction(
                    VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged(formValues)
                )
            },
            onInteractionEvent = {
                if (!hasSentInteractionEvent) {
                    interactor.handleViewAction(VerticalModeFormInteractor.ViewAction.FieldInteraction)
                    hasSentInteractionEvent = true
                }
            },
        )
    }
}

@Composable
internal fun VerticalModeFormHeaderUI(
    isEnabled: Boolean,
    formHeaderInformation: FormHeaderInformation,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context)
    }

    Row(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .padding(StripeTheme.getOuterFormInsets()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (formHeaderInformation.shouldShowIcon) {
            PaymentMethodIcon(
                iconRes = formHeaderInformation.icon(),
                iconUrl = formHeaderInformation.iconUrl(),
                imageLoader = imageLoader,
                iconRequiresTinting = formHeaderInformation.iconRequiresTinting,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(20.dp),
                contentAlignment = Alignment.Center,
            )
        }

        val textColor = MaterialTheme.colors.onSurface
        Text(
            text = formHeaderInformation.displayName.resolve(),
            style = MaterialTheme.typography.h4,
            color = if (isEnabled) textColor else textColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(TEST_TAG_HEADER_TITLE)
        )

        if (formHeaderInformation.promoBadge != null) {
            PromoBadge(
                text = formHeaderInformation.promoBadge,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

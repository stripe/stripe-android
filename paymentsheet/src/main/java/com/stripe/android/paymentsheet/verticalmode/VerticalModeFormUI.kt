package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.paymentsheet.ui.PromoBadge
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState

internal const val TEST_TAG_HEADER_TITLE = "TEST_TAG_HEADER_TITLE"

@Composable
internal fun VerticalModeFormUI(
    interactor: VerticalModeFormInteractor,
    showsWalletHeader: Boolean,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    var hasSentInteractionEvent by remember { mutableStateOf(false) }
    val state by interactor.state.collectAsState()

    Column(modifier) {
        val headerInformation = state.headerInformation
        val enabled = !state.isProcessing
        if (headerInformation != null && !showsWalletHeader) {
            VerticalModeFormHeaderUI(isEnabled = enabled, formHeaderInformation = headerInformation)
        }

        FormElement(
            enabled = enabled,
            selectedPaymentMethodCode = state.selectedPaymentMethodCode,
            formElements = state.formElements,
            formArguments = state.formArguments,
            usBankAccountFormArguments = state.usBankAccountFormArguments,
            horizontalPadding = horizontalPadding,
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
    val iconUrl = if (isSystemInDarkTheme() && formHeaderInformation.darkThemeIconUrl != null) {
        formHeaderInformation.darkThemeIconUrl
    } else {
        formHeaderInformation.lightThemeIconUrl
    }

    Row(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (formHeaderInformation.shouldShowIcon) {
            PaymentMethodIcon(
                iconRes = formHeaderInformation.iconResource,
                iconUrl = iconUrl,
                imageLoader = imageLoader,
                iconRequiresTinting = formHeaderInformation.iconRequiresTinting,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(20.dp),
                contentAlignment = Alignment.Center,
            )
        }

        val textColor = MaterialTheme.stripeColors.onComponent
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

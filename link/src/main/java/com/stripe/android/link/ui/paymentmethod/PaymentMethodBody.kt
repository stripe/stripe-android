package com.stripe.android.link.ui.paymentmethod

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForLinkContract
import com.stripe.android.link.R
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.link.ui.forms.Form

@Preview
@Composable
private fun PaymentMethodBodyPreview() {
    DefaultLinkTheme {
        Surface {
            PaymentMethodBody(
                supportedPaymentMethods = SupportedPaymentMethod.values().toList(),
                selectedPaymentMethod = SupportedPaymentMethod.Card,
                primaryButtonLabel = "Pay $10.99",
                primaryButtonState = PrimaryButtonState.Enabled,
                secondaryButtonLabel = "Cancel",
                errorMessage = null,
                onPaymentMethodSelected = {},
                onPrimaryButtonClick = {},
                onSecondaryButtonClick = {}
            ) {}
        }
    }
}

@Composable
internal fun PaymentMethodBody(
    linkAccount: LinkAccount,
    injector: NonFallbackInjector,
    loadFromArgs: Boolean
) {
    val viewModel: PaymentMethodViewModel = viewModel(
        factory = PaymentMethodViewModel.Factory(linkAccount, injector, loadFromArgs)
    )

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = FinancialConnectionsSheetForLinkContract(),
        onResult = viewModel::onFinancialConnectionsAccountLinked
    )

    val clientSecret by viewModel.financialConnectionsSessionClientSecret.collectAsState()

    clientSecret?.let { secret ->
        LaunchedEffect(secret) {
            activityResultLauncher.launch(
                FinancialConnectionsSheetActivityArgs.ForLink(
                    FinancialConnectionsSheet.Configuration(
                        financialConnectionsSessionClientSecret = secret,
                        publishableKey = viewModel.publishableKey
                    )
                )
            )
        }
    }

    val formController by viewModel.formController.collectAsState()

    formController?.let { controller ->
        val formValues by controller.completeFormValues.collectAsState(null)
        val primaryButtonState by viewModel.primaryButtonState.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()
        val paymentMethod by viewModel.paymentMethod.collectAsState()

        PaymentMethodBody(
            supportedPaymentMethods = viewModel.supportedTypes,
            selectedPaymentMethod = paymentMethod,
            primaryButtonLabel = paymentMethod.primaryButtonLabel(
                viewModel.args.stripeIntent,
                LocalContext.current.resources
            ),
            primaryButtonState = primaryButtonState.takeIf { formValues != null }
                ?: PrimaryButtonState.Disabled,
            secondaryButtonLabel = stringResource(id = viewModel.secondaryButtonLabel),
            errorMessage = errorMessage,
            onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
            onPrimaryButtonClick = {
                formValues?.let {
                    viewModel.startPayment(it)
                }
            },
            onSecondaryButtonClick = viewModel::onSecondaryButtonClick,
            formContent = {
                Form(
                    controller,
                    viewModel.isEnabled
                )
            }
        )
    } ?: run {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
internal fun PaymentMethodBody(
    supportedPaymentMethods: List<SupportedPaymentMethod>,
    selectedPaymentMethod: SupportedPaymentMethod,
    primaryButtonLabel: String,
    primaryButtonState: PrimaryButtonState,
    secondaryButtonLabel: String,
    errorMessage: ErrorMessage?,
    onPaymentMethodSelected: (SupportedPaymentMethod) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
    formContent: @Composable ColumnScope.() -> Unit
) {
    ScrollableTopLevelColumn {
        Text(
            text = stringResource(R.string.stripe_add_payment_method),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary
        )
        if (supportedPaymentMethods.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                supportedPaymentMethods.forEach { paymentMethod ->
                    PaymentMethodTypeCell(
                        paymentMethod = paymentMethod,
                        selected = paymentMethod == selectedPaymentMethod,
                        enabled = !primaryButtonState.isBlocking,
                        onSelected = {
                            onPaymentMethodSelected(paymentMethod)
                        }
                    )
                }
            }
        }
        if (selectedPaymentMethod.showsForm) {
            Spacer(modifier = Modifier.height(4.dp))
            StripeThemeForLink {
                formContent()
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        AnimatedVisibility(visible = errorMessage != null) {
            ErrorText(
                text = errorMessage?.getMessage(LocalContext.current.resources).orEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        PrimaryButton(
            label = primaryButtonLabel,
            state = primaryButtonState,
            onButtonClick = onPrimaryButtonClick,
            iconStart = selectedPaymentMethod.primaryButtonStartIconResourceId,
            iconEnd = selectedPaymentMethod.primaryButtonEndIconResourceId
        )
        SecondaryButton(
            enabled = !primaryButtonState.isBlocking,
            label = secondaryButtonLabel,
            onClick = onSecondaryButtonClick
        )
    }
}

@Composable
private fun RowScope.PaymentMethodTypeCell(
    paymentMethod: SupportedPaymentMethod,
    selected: Boolean,
    enabled: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalContentAlpha provides if (enabled) 1f else 0.6f) {
        Surface(
            modifier = modifier
                .height(56.dp)
                .weight(1f),
            shape = MaterialTheme.linkShapes.small,
            color = MaterialTheme.linkColors.componentBackground,
            border = BorderStroke(
                width = if (selected) {
                    2.dp
                } else {
                    1.dp
                },
                color = if (selected) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.linkColors.componentBorder
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        enabled = enabled,
                        onClick = onSelected
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = paymentMethod.iconResourceId),
                    contentDescription = null,
                    modifier = Modifier
                        .width(50.dp)
                        .padding(horizontal = 16.dp),
                    alpha = LocalContentAlpha.current,
                    colorFilter = ColorFilter.tint(
                        color = if (selected) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSecondary
                        }
                    )
                )
                Text(
                    text = stringResource(id = paymentMethod.nameResourceId),
                    modifier = Modifier.padding(end = 16.dp),
                    color = if (selected) {
                        MaterialTheme.colors.onPrimary
                    } else {
                        MaterialTheme.colors.onSecondary
                    },
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}

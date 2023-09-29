package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.util.VisibleForTesting
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
internal fun BacsMandateConfirmationFormScreen(
    viewModel: BacsMandateConfirmationViewModel = viewModel()
) {
    val viewState by viewModel.viewState.collectAsState()

    BacsMandateConfirmationFormView(viewState, viewModel::handleViewAction)
}

@Composable
@VisibleForTesting
internal fun BacsMandateConfirmationFormView(
    state: BacsMandateConfirmationViewState,
    viewActionHandler: (action: BacsMandateConfirmationViewAction) -> Unit,
    modifier: Modifier = Modifier
) {
    return Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.stripe_paymentsheet_bacs_mandate_title),
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colors.onBackground
        )
        Details(state)
        MandateItem(stringResource(R.string.stripe_paymentsheet_bacs_email_mandate, state.email))
        MandateItem(stringResource(
            R.string.stripe_paymentsheet_bacs_notice_mandate,
            state.payer.resolve()
        ))
        Row {
            MandateItem(
                stringResource(R.string.stripe_paymentsheet_bacs_protection_mandate, state.debitGuaranteeAsHtml.resolve()),
                modifier = Modifier.weight(WEIGHT_60_PERCENT),
                isHtml = true
            )
            Box(modifier = Modifier.weight(WEIGHT_40_PERCENT), contentAlignment = Alignment.CenterEnd) {
                Icon(
                    painterResource(id = R.drawable.stripe_bacs_direct_debit_mark),
                    tint = MaterialTheme.stripeColors.placeholderText,
                    contentDescription = null
                )
            }
        }
        MandateItem(
            state.supportAddressAsHtml.resolve(),
            isHtml = true
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BacsMandateButton(
                type = BacsMandateButtonType.Primary,
                label = stringResource(R.string.stripe_paymentsheet_confirm),
                onClick = {
                    viewActionHandler.invoke((BacsMandateConfirmationViewAction.OnConfirmPressed))
                }
            )
            BacsMandateButton(
                type = BacsMandateButtonType.Secondary,
                label = stringResource(R.string.stripe_paymentsheet_bacs_modify_details_button_label),
                onClick = {
                    viewActionHandler.invoke((BacsMandateConfirmationViewAction.OnCancelPressed))
                }
            )
        }
        PaymentSheetContentPadding()
    }
}

@Composable
private fun Details(state: BacsMandateConfirmationViewState) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.stripeColors.componentBorder,
                ),
                shape = MaterialTheme.shapes.small
            )
            .background(MaterialTheme.stripeColors.component)
            .padding(12.dp)
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailsRow(
                label = stringResource(R.string.stripe_paymentsheet_bacs_mandate_info_email_label),
                value = state.email
            )
            DetailsRow(
                label = stringResource(R.string.stripe_paymentsheet_bacs_mandate_info_name_label),
                value = state.nameOnAccount
            )
            DetailsRow(
                label = stringResource(R.string.stripe_paymentsheet_bacs_mandate_info_sort_code_label),
                value = state.sortCode
            )
            DetailsRow(
                label = stringResource(R.string.stripe_paymentsheet_bacs_mandate_info_account_number_label),
                value = state.accountNumber
            )
        }
    }
}

@Composable
private fun DetailsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.weight(WEIGHT_40_PERCENT),
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.stripeColors.onComponent,
            text = label
        )
        Text(
            modifier = Modifier.weight(WEIGHT_60_PERCENT),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.stripeColors.onComponent,
            text = value
        )
    }
}

@Composable
private fun MandateItem(
    text: String,
    modifier: Modifier = Modifier,
    isHtml: Boolean = false
) {
    when (isHtml) {
        true -> Html(
            modifier = modifier,
            html = text,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.stripeColors.subtitle,
            urlSpanStyle = SpanStyle(
                color = MaterialTheme.colors.primary
            )
        )
        false -> Text(
            modifier = modifier,
            text = text,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.stripeColors.subtitle
        )
    }
}

@Composable
@Preview
fun PreviewThisStuff() {
    StripeTheme {
        BacsMandateConfirmationFormView(
            state = BacsMandateConfirmationViewState(
                accountNumber = "00012345",
                sortCode = "10-88-00",
                email = "email@email.com",
                nameOnAccount = "John Doe",
                payer = resolvableString(R.string.stripe_paymentsheet_bacs_notice_default_payer),
                debitGuaranteeAsHtml = resolvableString(
                    R.string.stripe_paymentsheet_bacs_guarantee_format,
                    resolvableString(R.string.stripe_paymentsheet_bacs_guarantee_url),
                    resolvableString(R.string.stripe_paymentsheet_bacs_guarantee)
                ),
                supportAddressAsHtml = resolvableString(
                    R.string.stripe_paymentsheet_bacs_support_address_format,
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_one),
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_two),
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email),
                    resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email)
                )
            ),
            viewActionHandler = {}
        )
    }
}

private const val WEIGHT_60_PERCENT = 0.6f
private const val WEIGHT_40_PERCENT = 0.4f

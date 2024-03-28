package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.InstitutionIcon
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.BottomSheetContent
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Layout

@Composable
internal fun AccountUpdateRequiredBottomSheetContent(
    content: BottomSheetContent.UpdateRequired,
    modifier: Modifier = Modifier,
    onContinue: (BottomSheetContent.UpdateRequired) -> Unit,
    onCancel: () -> Unit,
) {
    Layout(
        modifier = modifier,
        inModal = true,
        footer = {
            Column {
                Spacer(modifier = Modifier.size(16.dp))
                FinancialConnectionsButton(
                    onClick = { onContinue(content) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.stripe_prepane_continue))
                }
                Spacer(modifier = Modifier.size(8.dp))
                FinancialConnectionsButton(
                    onClick = onCancel,
                    type = FinancialConnectionsButton.Type.Secondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.stripe_prepane_cancel_cta))
                }
            }
        },
        body = {
            content.iconUrl?.let { url ->
                InstitutionIcon(institutionIcon = url)
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_update_required_title),
                defaultStyle = FinancialConnectionsTheme.typography.headingMedium.copy(
                    color = FinancialConnectionsTheme.colors.textDefault
                ),
                onClickableTextClick = {},
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_update_required_desc),
                onClickableTextClick = {},
                defaultStyle = FinancialConnectionsTheme.typography.bodyMedium,
            )
        },
    )
}

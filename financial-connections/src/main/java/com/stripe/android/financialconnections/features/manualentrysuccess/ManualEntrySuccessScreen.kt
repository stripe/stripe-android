@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun ManualEntrySuccessScreen(
    backStackEntry: NavBackStackEntry,
) {
    val parentViewModel = parentViewModel()
    val viewModel: ManualEntrySuccessViewModel = mavericksViewModel()
    BackHandler(true) {}
    val completeAuthSessionAsync = viewModel
        .collectAsState(ManualEntrySuccessState::completeSession)
    ManualEntrySuccessContent(
        microdepositVerificationMethod = Destination.ManualEntrySuccess.microdeposits(backStackEntry),
        last4 = Destination.ManualEntrySuccess.last4(backStackEntry),
        loading = completeAuthSessionAsync.value is Loading,
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.MANUAL_ENTRY_SUCCESS) },
        onDoneClick = viewModel::onSubmit
    )
}

@Composable
internal fun ManualEntrySuccessContent(
    microdepositVerificationMethod: MicrodepositVerificationMethod,
    last4: String?,
    loading: Boolean,
    onCloseClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick,
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 8.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                )
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(R.drawable.stripe_ic_check_circle),
                contentDescription = null,
                tint = FinancialConnectionsTheme.colors.textSuccess
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = when (microdepositVerificationMethod) {
                    MicrodepositVerificationMethod.UNKNOWN,
                    MicrodepositVerificationMethod.AMOUNTS ->
                        stringResource(R.string.stripe_manualentrysuccess_title)
                    MicrodepositVerificationMethod.DESCRIPTOR_CODE ->
                        stringResource(R.string.stripe_manualentrysuccess_title_descriptorcode)
                },
                style = FinancialConnectionsTheme.typography.subtitle.copy(
                    color = FinancialConnectionsTheme.colors.textPrimary
                )
            )
            Text(
                text = resolveText(microdepositVerificationMethod, last4),
                style = FinancialConnectionsTheme.typography.body.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            TransactionHistoryTable(
                microdepositVerificationMethod = microdepositVerificationMethod,
                last4 = last4
            )
            Spacer(modifier = Modifier.weight(1f))
            FinancialConnectionsButton(
                loading = loading,
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.stripe_success_pane_done))
            }
        }
    }
}

@Composable
internal fun resolveText(
    microdepositVerificationMethod: MicrodepositVerificationMethod,
    last4: String?
) = when (microdepositVerificationMethod) {
    MicrodepositVerificationMethod.AMOUNTS -> when {
        last4 != null -> stringResource(R.string.stripe_manualentrysuccess_desc, last4)
        else -> stringResource(R.string.stripe_manualentrysuccess_desc_noaccount)
    }

    MicrodepositVerificationMethod.DESCRIPTOR_CODE -> when {
        last4 != null -> stringResource(
            R.string.stripe_manualentrysuccess_desc_descriptorcode,
            last4
        )

        else -> stringResource(R.string.stripe_manualentrysuccess_desc_noaccount_descriptorcode)
    }

    MicrodepositVerificationMethod.UNKNOWN -> TODO()
}

@Composable
internal fun TransactionHistoryTable(
    last4: String?,
    microdepositVerificationMethod: MicrodepositVerificationMethod
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .clip(shape)
            .background(FinancialConnectionsTheme.colors.backgroundContainer)
            .border(
                border = BorderStroke(1.dp, FinancialConnectionsTheme.colors.borderDefault),
                shape = shape
            )
    ) {
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            val titleColor = FinancialConnectionsTheme.colors.textSecondary
            val tableData =
                buildTableRows(microdepositVerificationMethod)
            last4?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stripe_ic_bank),
                        tint = FinancialConnectionsTheme.colors.textSecondary,
                        contentDescription = "Bank icon"
                    )
                    Text(
                        text = stringResource(R.string.stripe_manualentrysuccess_table_title, it),
                        style = FinancialConnectionsTheme.typography.bodyCode.copy(color = titleColor)
                    )
                }
                Spacer(Modifier.size(8.dp))
            }
            Row {
                TitleCell(text = "Transaction")
                TitleCell(text = "Amount")
                TitleCell(text = "Type")
            }
            Divider(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                color = FinancialConnectionsTheme.colors.borderDefault
            )
            tableData.withIndex().forEach { (index, item) ->
                val (transaction, amount, type) = item
                val highlight = tableData.lastIndex != index
                Row(Modifier.fillMaxWidth()) {
                    TableCell(text = transaction.first, transaction.second, highlight)
                    TableCell(text = amount.first, amount.second, highlight)
                    TableCell(text = type.first, type.second, highlight)
                }
            }
        }
        // Bottom fade.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            FinancialConnectionsTheme.colors.textWhite.copy(alpha = 0f),
                            FinancialConnectionsTheme.colors.textWhite.copy(alpha = 1f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun buildTableRows(
    microdepositVerificationMethod: MicrodepositVerificationMethod
): List<Triple<Pair<String, Color>, Pair<String, Color>, Pair<String, Color>>> {
    val rowColor = FinancialConnectionsTheme.colors.textPrimary
    val highlightColor = FinancialConnectionsTheme.colors.textBrand
    return when (microdepositVerificationMethod) {
        MicrodepositVerificationMethod.DESCRIPTOR_CODE -> listOf(
            Triple("SMXXXX" to highlightColor, "$0.01" to rowColor, "ACH CREDIT" to rowColor),
            Triple("GROCERIES" to rowColor, "$56.12" to rowColor, "VISA" to rowColor)
        )

        MicrodepositVerificationMethod.AMOUNTS -> listOf(
            Triple("AMTS" to rowColor, "$0.XX" to highlightColor, "ACH CREDIT" to rowColor),
            Triple("AMTS" to rowColor, "$0.XX" to highlightColor, "ACH CREDIT" to rowColor),
            Triple("GROCERIES" to rowColor, "$56.12" to rowColor, "VISA" to rowColor)
        )

        MicrodepositVerificationMethod.UNKNOWN -> error("Unknown microdeposits type")
    }
}

@Composable
private fun RowScope.TitleCell(
    text: String,
) {
    Text(
        text = text,
        style = FinancialConnectionsTheme.typography.caption.copy(
            color = FinancialConnectionsTheme.colors.textSecondary
        ),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .weight(1f)
    )
}

@Composable
private fun RowScope.TableCell(
    text: String,
    color: Color,
    highlight: Boolean
) {
    val typography = if (highlight) {
        FinancialConnectionsTheme.typography.captionCodeEmphasized
    } else {
        FinancialConnectionsTheme.typography.captionCode
    }
    Text(
        text = text,
        style = typography.copy(color = color),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .weight(1f)
    )
}

@Preview(
    group = "Manual Entry Success",
    name = "Amount"
)
@Composable
internal fun ManualEntrySuccessScreenPreviewAmount() {
    FinancialConnectionsPreview {
        ManualEntrySuccessContent(
            MicrodepositVerificationMethod.AMOUNTS,
            last4 = "1234",
            loading = false,
            onCloseClick = {}
        ) {}
    }
}

@Preview(
    group = "Manual Entry Success",
    name = "Descriptor"
)
@Composable
internal fun ManualEntrySuccessDescriptor() {
    FinancialConnectionsPreview {
        ManualEntrySuccessContent(
            MicrodepositVerificationMethod.DESCRIPTOR_CODE,
            last4 = "1234",
            loading = false,
            onCloseClick = {}
        ) {}
    }
}

@Preview(
    group = "Manual Entry Success",
    name = "Amount no account"
)
@Composable
internal fun ManualEntrySuccessAmountNoAccount() {
    FinancialConnectionsPreview {
        ManualEntrySuccessContent(
            MicrodepositVerificationMethod.AMOUNTS,
            last4 = null,
            loading = false,
            onCloseClick = {}
        ) {}
    }
}

@Preview(
    group = "Manual Entry Success",
    name = "Descriptor no account"
)
@Composable
internal fun ManualEntrySuccessDescriptorNoAccount() {
    FinancialConnectionsPreview {
        ManualEntrySuccessContent(
            MicrodepositVerificationMethod.DESCRIPTOR_CODE,
            last4 = null,
            loading = false,
            onCloseClick = {}
        ) {}
    }
}

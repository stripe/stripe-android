package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutWithAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    state.value.payload()?.let { payload ->
        SuccessContent(
            accessibleDataModel = payload.accessibleData,
            disconnectUrl = payload.disconnectUrl,
            accounts = payload.accounts.data,
            loading = state.value.completeSession is Loading,
            onDoneClick = viewModel::onDoneClick,
            onLinkAnotherAccountClick = viewModel::onLinkAnotherAccountClick,
            onCloseClick = parentViewModel::onCloseWithConfirmationClick,
            showLinkAnotherAccount = payload.showLinkAnotherAccount,
            institution = payload.institution
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun SuccessContent(
    accessibleDataModel: AccessibleDataCalloutModel,
    disconnectUrl: String,
    accounts: List<PartnerAccount>,
    institution: FinancialConnectionsInstitution,
    loading: Boolean,
    onDoneClick: () -> Unit,
    onLinkAnotherAccountClick: () -> Unit,
    showLinkAnotherAccount: Boolean,
    onCloseClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick,
                showBack = false
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 24.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
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
                text = stringResource(R.string.stripe_success_title),
                style = FinancialConnectionsTheme.typography.subtitle
            )
            AccessibleDataCalloutWithAccounts(
                model = accessibleDataModel,
                accounts = accounts,
                institution = institution,
            )
            AnnotatedText(
                text = TextResource.StringId(R.string.success_pane_disconnect),
                onClickableTextClick = { uriHandler.openUri(disconnectUrl) },
                defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                ),
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                        .toSpanStyle()
                        .copy(color = FinancialConnectionsTheme.colors.textBrand)
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (showLinkAnotherAccount) {
                FinancialConnectionsButton(
                    enabled = loading.not(),
                    type = FinancialConnectionsButton.Type.Secondary,
                    onClick = onLinkAnotherAccountClick,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.success_pane_link_more_accounts))
                }
            }
            FinancialConnectionsButton(
                loading = loading,
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.success_pane_done))
            }
        }
    }
}

@Composable
@Preview
internal fun SuccessScreenPreview() {
    FinancialConnectionsTheme {
        SuccessContent(
            accessibleDataModel = AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
                    FinancialConnectionsAccount.Permissions.BALANCES,
                    FinancialConnectionsAccount.Permissions.OWNERSHIP,
                    FinancialConnectionsAccount.Permissions.TRANSACTIONS
                ),
                isStripeDirect = true,
                dataPolicyUrl = ""
            ),
            disconnectUrl = "",
            accounts = listOf(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id2",
                    name = "Account 2 - no acct numbers",
                    subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                    supportedPaymentMethodTypes = emptyList()
                ),
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id3",
                    name = "Account 3",
                    displayableAccountNumbers = "1234",
                    subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
                    supportedPaymentMethodTypes = emptyList()
                ),
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id4",
                    name = "Account 4",
                    displayableAccountNumbers = "1234",
                    subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                    supportedPaymentMethodTypes = emptyList()
                )
            ),
            loading = false,
            onDoneClick = {},
            onLinkAnotherAccountClick = {},
            showLinkAnotherAccount = true,
            onCloseClick = {},
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                featuredOrder = null,
                mobileHandoffCapable = false
            )
        )
    }
}

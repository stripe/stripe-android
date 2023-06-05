package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.Attention100
import com.stripe.android.financialconnections.ui.theme.Attention50
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
            accounts = payload.accounts,
            institution = payload.institution,
            successMessage = payload.successMessage,
            loading = state.value.completeSession is Loading,
            skipSuccessPane = payload.skipSuccessPane,
            onDoneClick = viewModel::onDoneClick,
            accountFailedToLinkMessage = payload.accountFailedToLinkMessage,
            onLearnMoreAboutDataAccessClick = viewModel::onLearnMoreAboutDataAccessClick,
            onDisconnectLinkClick = viewModel::onDisconnectLinkClick
        ) { parentViewModel.onCloseNoConfirmationClick(Pane.SUCCESS) }
    }
}

@Composable
private fun SuccessContent(
    accessibleDataModel: AccessibleDataCalloutModel,
    disconnectUrl: String,
    accounts: List<PartnerAccount>,
    institution: FinancialConnectionsInstitution,
    successMessage: TextResource,
    loading: Boolean,
    skipSuccessPane: Boolean,
    accountFailedToLinkMessage: TextResource?,
    onDoneClick: () -> Unit,
    onLearnMoreAboutDataAccessClick: () -> Unit,
    onDisconnectLinkClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick,
                elevation = scrollState.elevation
            )
        }
    ) {
        if (skipSuccessPane) {
            SuccessLoading()
        } else {
            SuccessLoaded(
                scrollState = scrollState,
                accounts = accounts,
                accessibleDataModel = accessibleDataModel,
                disconnectUrl = disconnectUrl,
                institution = institution,
                loading = loading,
                successMessage = successMessage,
                accountFailedToLinkMessage = accountFailedToLinkMessage,
                onLearnMoreAboutDataAccessClick = onLearnMoreAboutDataAccessClick,
                onDisconnectLinkClick = onDisconnectLinkClick,
                onDoneClick = onDoneClick
            )
        }
    }
}

@Composable
private fun SuccessLoading() {
    LoadingContent(
        title = stringResource(id = R.string.stripe_success_pane_skip_title),
        content = stringResource(id = R.string.stripe_success_pane_skip_desc),
    )
}

@Composable
@Suppress("LongMethod")
private fun SuccessLoaded(
    scrollState: ScrollState,
    accounts: List<PartnerAccount>,
    accessibleDataModel: AccessibleDataCalloutModel,
    disconnectUrl: String,
    successMessage: TextResource,
    institution: FinancialConnectionsInstitution,
    loading: Boolean,
    accountFailedToLinkMessage: TextResource?,
    onLearnMoreAboutDataAccessClick: () -> Unit,
    onDisconnectLinkClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(
                    top = 8.dp,
                    start = 24.dp,
                    end = 24.dp
                )
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(R.drawable.stripe_ic_check_circle),
                contentDescription = null,
                tint = FinancialConnectionsTheme.colors.textSuccess
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.stripe_success_title),
                style = FinancialConnectionsTheme.typography.subtitle
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = successMessage.toText().toString(),
                style = FinancialConnectionsTheme.typography.body
            )
            if (accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.size(24.dp))
                AccessibleDataCalloutWithAccounts(
                    model = accessibleDataModel,
                    accounts = accounts,
                    institution = institution,
                    onLearnMoreClick = { onLearnMoreAboutDataAccessClick() }
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_success_pane_disconnect),
                onClickableTextClick = {
                    onDisconnectLinkClick()
                    uriHandler.openUri(disconnectUrl)
                },
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
        }
        SuccessLoadedFooter(
            accountFailedToLinkMessage = accountFailedToLinkMessage,
            loading = loading,
            onDoneClick = onDoneClick
        )
    }
}

@Composable
private fun AccountNotSavedToLinkNotice(message: TextResource) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .border(color = Attention100, width = 1.dp)
            .background(color = Attention50)
            .padding(12.dp)
    ) {
        Row {
            Icon(
                modifier = Modifier
                    .size(12.dp)
                    .offset(y = 2.dp),
                painter = painterResource(R.drawable.stripe_ic_warning),
                contentDescription = null,
                tint = FinancialConnectionsTheme.colors.textAttention
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = message.toText().toString(),
                style = FinancialConnectionsTheme.typography.caption.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun SuccessLoadedFooter(
    loading: Boolean,
    accountFailedToLinkMessage: TextResource?,
    onDoneClick: () -> Unit
) {
    Column(
        Modifier.padding(
            bottom = 24.dp,
            start = 24.dp,
            end = 24.dp
        )
    ) {
        accountFailedToLinkMessage?.let {
            AccountNotSavedToLinkNotice(it)
            Spacer(modifier = Modifier.size(20.dp))
        }
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

@Preview(
    group = "Success",
    name = "Default"
)
@Suppress("LongMethod")
@Composable
internal fun SuccessScreenPreview() {
    FinancialConnectionsPreview {
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
                isNetworking = false,
                dataPolicyUrl = ""
            ),
            disconnectUrl = "",
            accounts = previewAccounts(),
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            successMessage = TextResource.PluralId(
                value = R.plurals.stripe_success_pane_link_with_connected_account_name,
                count = 2,
                args = listOf("ConnectedAccount", "BusinessName")
            ),
            loading = false,
            skipSuccessPane = false,
            accountFailedToLinkMessage = null,
            onDoneClick = {},
            onLearnMoreAboutDataAccessClick = {},
            onDisconnectLinkClick = {}
        ) {}
    }
}

@Composable
@Preview
@Suppress("LongMethod")
internal fun SuccessScreenPreviewFailedToLink() {
    FinancialConnectionsPreview {
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
                isNetworking = false,
                dataPolicyUrl = ""
            ),
            disconnectUrl = "",
            accounts = previewAccounts(),
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            successMessage = TextResource.Text("Hola"),
            loading = false,
            skipSuccessPane = false,
            accountFailedToLinkMessage = TextResource.PluralId(
                R.plurals.stripe_success_networking_save_to_link_failed,
                1,
                listOf("Random Business")
            ),
            onDoneClick = {},
            onLearnMoreAboutDataAccessClick = {},
            onDisconnectLinkClick = {}
        ) {}
    }
}

@Composable
private fun previewAccounts() = listOf(
    PartnerAccount(
        authorization = "Authorization",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id2",
        name = "Account 2 - no acct numbers",
        _allowSelection = true,
        allowSelectionMessage = "",
        subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
        supportedPaymentMethodTypes = emptyList()
    ),
    PartnerAccount(
        authorization = "Authorization",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id3",
        name = "Account 3",
        _allowSelection = true,
        allowSelectionMessage = "",
        displayableAccountNumbers = "1234",
        subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
        supportedPaymentMethodTypes = emptyList()
    ),
    PartnerAccount(
        authorization = "Authorization",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id4",
        name = "Account 4",
        _allowSelection = true,
        allowSelectionMessage = "",
        displayableAccountNumbers = "1234",
        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
        supportedPaymentMethodTypes = emptyList()
    )
)

package com.stripe.android.link.ui.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.uicore.text.Html
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun WalletScreen(
    viewModel: WalletViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    WalletBody(
        state = state,
        onItemSelected = viewModel::onItemSelected,
        onExpandedChanged = viewModel::setExpanded
    )
}

@Composable
internal fun WalletBody(
    state: WalletUiState,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
) {
    if (state.paymentDetailsList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(WALLET_LOADER_TAG),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isProcessing) {
        if (state.isProcessing) {
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .animateContentSize()
        ) {
            val selectedItem = state.selectedItem
            if (state.isExpanded || selectedItem == null) {
                ExpandedPaymentDetails(
                    uiState = state,
                    onItemSelected = onItemSelected,
                    onMenuButtonClick = {},
                    onAddNewPaymentMethodClick = {},
                    onCollapse = {
                        onExpandedChanged(false)
                    }
                )
            } else {
                CollapsedPaymentDetails(
                    selectedPaymentMethod = selectedItem,
                    enabled = !state.primaryButtonState.isBlocking,
                    onClick = {
                        onExpandedChanged(true)
                    }
                )
            }
        }

        AnimatedVisibility(state.showBankAccountTerms) {
            BankAccountTerms()
        }
    }
}

@Composable
internal fun CollapsedPaymentDetails(
    selectedPaymentMethod: ConsumerPaymentDetails.PaymentDetails,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag(COLLAPSED_WALLET_ROW)
            .fillMaxWidth()
            .height(64.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.linkColors.componentBorder,
                shape = MaterialTheme.linkShapes.large
            )
            .clip(MaterialTheme.linkShapes.large)
            .background(
                color = MaterialTheme.linkColors.componentBackground,
                shape = MaterialTheme.linkShapes.large
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stripe_wallet_collapsed_payment),
            modifier = Modifier
                .testTag(COLLAPSED_WALLET_HEADER_TAG)
                .padding(
                    start = HorizontalPadding,
                    end = 8.dp
                ),
            color = MaterialTheme.linkColors.disabledText
        )
        PaymentDetails(
            modifier = Modifier
                .testTag(COLLAPSED_WALLET_PAYMENT_DETAILS_TAG),
            paymentDetails = selectedPaymentMethod
        )
        Icon(
            painter = painterResource(R.drawable.stripe_link_chevron),
            contentDescription = stringResource(R.string.stripe_wallet_expand_accessibility),
            modifier = Modifier
                .padding(end = 22.dp)
                .testTag(COLLAPSED_WALLET_CHEVRON_ICON_TAG),
            tint = MaterialTheme.linkColors.disabledText
        )
    }
}

@Composable
private fun ExpandedPaymentDetails(
    uiState: WalletUiState,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onMenuButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onAddNewPaymentMethodClick: () -> Unit,
    onCollapse: () -> Unit
) {
    val isEnabled = !uiState.primaryButtonState.isBlocking

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.linkColors.componentBorder,
                shape = MaterialTheme.linkShapes.large
            )
            .clip(MaterialTheme.linkShapes.large)
            .background(
                color = MaterialTheme.linkColors.componentBackground,
                shape = MaterialTheme.linkShapes.large
            )
    ) {
        item {
            ExpandedRowHeader(
                isEnabled = isEnabled,
                onCollapse = onCollapse
            )
        }

        items(
            items = uiState.paymentDetailsList,
            key = {
                "payment_detail_${it.id}"
            }
        ) { item ->
            PaymentDetailsListItem(
                modifier = Modifier
                    .testTag(WALLET_SCREEN_PAYMENT_METHODS_LIST),
                paymentDetails = item,
                enabled = isEnabled,
                isSelected = uiState.selectedItem?.id == item.id,
                isUpdating = false,
                onClick = {
                    onItemSelected(item)
                },
                onMenuButtonClick = {
                    onMenuButtonClick(item)
                }
            )
        }

        item {
            AddPaymentMethodRow(
                isEnabled = isEnabled,
                onAddNewPaymentMethodClick = onAddNewPaymentMethodClick
            )
        }
    }
}

@Composable
private fun ExpandedRowHeader(
    isEnabled: Boolean,
    onCollapse: () -> Unit,
) {
    Row(
        modifier = Modifier
            .testTag(WALLET_SCREEN_EXPANDED_ROW_HEADER)
            .height(44.dp)
            .clickable(
                enabled = isEnabled,
                onClick = onCollapse
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.stripe_wallet_expanded_title),
            modifier = Modifier
                .padding(start = HorizontalPadding, top = 20.dp),
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.button
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.stripe_link_chevron),
            contentDescription = stringResource(id = R.string.stripe_wallet_expand_accessibility),
            modifier = Modifier
                .padding(top = 20.dp, end = 22.dp)
                .rotate(CHEVRON_ICON_ROTATION),
            tint = MaterialTheme.colors.onPrimary
        )
    }
}

@Composable
private fun AddPaymentMethodRow(
    isEnabled: Boolean,
    onAddNewPaymentMethodClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .testTag(WALLET_ADD_PAYMENT_METHOD_ROW)
            .fillMaxWidth()
            .height(60.dp)
            .clickable(enabled = isEnabled, onClick = onAddNewPaymentMethodClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.stripe_link_add_green),
            contentDescription = null,
            modifier = Modifier.padding(start = HorizontalPadding, end = 12.dp),
            tint = Color.Unspecified
        )
        Text(
            text = stringResource(id = R.string.stripe_add_payment_method),
            modifier = Modifier.padding(end = HorizontalPadding),
            color = MaterialTheme.linkColors.actionLabel,
            style = MaterialTheme.typography.button
        )
    }
}

@Composable
private fun BankAccountTerms() {
    Html(
        html = stringResource(R.string.stripe_wallet_bank_account_terms).replaceHyperlinks(),
        color = MaterialTheme.colors.onSecondary,
        style = MaterialTheme.typography.caption,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        urlSpanStyle = SpanStyle(
            color = MaterialTheme.colors.primary
        )
    )
}

private fun String.replaceHyperlinks() = this.replace(
    "<terms>",
    "<a href=\"https://stripe.com/legal/ach-payments/authorization\">"
).replace("</terms>", "</a>")

private const val CHEVRON_ICON_ROTATION = 180f
internal const val WALLET_LOADER_TAG = "wallet_screen_loader_tag"
internal const val COLLAPSED_WALLET_HEADER_TAG = "collapsed_wallet_header_tag"
internal const val COLLAPSED_WALLET_CHEVRON_ICON_TAG = "collapsed_wallet_chevron_icon_tag"
internal const val COLLAPSED_WALLET_PAYMENT_DETAILS_TAG = "collapsed_wallet_payment_details_tag"
internal const val COLLAPSED_WALLET_ROW = "collapsed_wallet_row_tag"
internal const val WALLET_SCREEN_EXPANDED_ROW_HEADER = "wallet_screen_expanded_row_header"
internal const val WALLET_ADD_PAYMENT_METHOD_ROW = "wallet_add_payment_method_row"
internal const val WALLET_SCREEN_PAYMENT_METHODS_LIST = "wallet_screen_payment_methods_list"

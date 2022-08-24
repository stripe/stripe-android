package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.R
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.link.ui.primaryButtonLabel
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.ui.core.elements.Html
import com.stripe.android.ui.core.injection.NonFallbackInjector
import com.stripe.android.ui.core.paymentsColors

@Preview
@Composable
private fun WalletBodyPreview() {
    DefaultLinkTheme {
        Surface {
            WalletBody(
                paymentDetailsList = listOf(
                    ConsumerPaymentDetails.Card(
                        "id1",
                        true,
                        2022,
                        12,
                        CardBrand.Visa,
                        "4242"
                    ),
                    ConsumerPaymentDetails.Card(
                        "id2",
                        false,
                        2023,
                        11,
                        CardBrand.MasterCard,
                        "4444"
                    )
                ),
                supportedTypes = SupportedPaymentMethod.allTypes,
                selectedItem = null,
                primaryButtonLabel = "Pay $10.99",
                primaryButtonState = PrimaryButtonState.Enabled,
                errorMessage = null,
                onItemSelected = {},
                onAddNewPaymentMethodClick = {},
                onEditPaymentMethod = {},
                onDeletePaymentMethod = {},
                onPrimaryButtonClick = {},
                onPayAnotherWayClick = {},
                showBottomSheetContent = {}
            )
        }
    }
}

@Composable
internal fun WalletBody(
    linkAccount: LinkAccount,
    injector: NonFallbackInjector,
    showBottomSheetContent: (BottomSheetContent?) -> Unit
) {
    val viewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.Factory(
            linkAccount,
            injector
        )
    )

    val paymentDetailsList by viewModel.paymentDetailsList.collectAsState()
    val primaryButtonState by viewModel.primaryButtonState.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (paymentDetailsList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        WalletBody(
            paymentDetailsList = paymentDetailsList,
            supportedTypes = viewModel.supportedTypes,
            selectedItem = selectedItem,
            primaryButtonLabel = primaryButtonLabel(viewModel.args, LocalContext.current.resources),
            primaryButtonState = primaryButtonState,
            errorMessage = errorMessage,
            onItemSelected = viewModel::onItemSelected,
            onAddNewPaymentMethodClick = viewModel::addNewPaymentMethod,
            onEditPaymentMethod = viewModel::editPaymentMethod,
            onDeletePaymentMethod = viewModel::deletePaymentMethod,
            onPrimaryButtonClick = viewModel::onConfirmPayment,
            onPayAnotherWayClick = viewModel::payAnotherWay,
            showBottomSheetContent = showBottomSheetContent
        )
    }
}

@Composable
internal fun WalletBody(
    paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>,
    supportedTypes: Set<String>,
    selectedItem: ConsumerPaymentDetails.PaymentDetails?,
    primaryButtonLabel: String,
    primaryButtonState: PrimaryButtonState,
    errorMessage: ErrorMessage?,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onAddNewPaymentMethodClick: () -> Unit,
    onEditPaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onDeletePaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onPayAnotherWayClick: () -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit
) {
    val selectedItemIsValid = selectedItem?.let { supportedTypes.contains(it.type) } ?: false
    var isWalletExpanded by rememberSaveable { mutableStateOf(!selectedItemIsValid) }
    var itemBeingRemoved by remember {
        mutableStateOf<ConsumerPaymentDetails.PaymentDetails?>(null)
    }
    var openDialog by remember { mutableStateOf(false) }

    itemBeingRemoved?.let {
        // Launch confirmation dialog at the first recomposition after marking item for deletion
        LaunchedEffect(it) {
            openDialog = true
        }

        ConfirmRemoveDialog(
            paymentDetails = it,
            showDialog = openDialog
        ) { confirmed ->
            if (confirmed) {
                onDeletePaymentMethod(it)
            }

            openDialog = false
            itemBeingRemoved = null
        }
    }

    ScrollableTopLevelColumn {
        Spacer(modifier = Modifier.height(12.dp))

        if (isWalletExpanded || !selectedItemIsValid) {
            isWalletExpanded = true
            ExpandedPaymentDetails(
                paymentDetailsList = paymentDetailsList,
                supportedTypes = supportedTypes,
                selectedItem = selectedItem?.takeIf { selectedItemIsValid },
                enabled = !primaryButtonState.isBlocking,
                onItemSelected = {
                    onItemSelected(it)
                    isWalletExpanded = false
                },
                onMenuButtonClick = {
                    showBottomSheetContent {
                        WalletPaymentMethodMenu(
                            paymentDetails = it,
                            onEditClick = {
                                showBottomSheetContent(null)
                                onEditPaymentMethod(it)
                            },
                            onRemoveClick = {
                                showBottomSheetContent(null)
                                itemBeingRemoved = it
                            },
                            onCancelClick = {
                                showBottomSheetContent(null)
                            }
                        )
                    }
                },
                onAddNewPaymentMethodClick = onAddNewPaymentMethodClick,
                onCollapse = {
                    isWalletExpanded = false
                }
            )
        } else {
            CollapsedPaymentDetails(
                selectedPaymentMethod = selectedItem!!,
                enabled = !primaryButtonState.isBlocking,
                onClick = {
                    isWalletExpanded = true
                }
            )
        }
        if (selectedItem is ConsumerPaymentDetails.BankAccount) {
            Html(
                html = stringResource(R.string.wallet_bank_account_terms),
                imageGetter = emptyMap(),
                color = MaterialTheme.paymentsColors.placeholderText,
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                urlSpanStyle = SpanStyle(
                    color = MaterialTheme.colors.primary
                )
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        errorMessage?.let {
            ErrorText(
                text = it.getMessage(LocalContext.current.resources),
                modifier = Modifier.fillMaxWidth()
            )
        }
        PrimaryButton(
            label = primaryButtonLabel,
            state = if (selectedItemIsValid) {
                primaryButtonState
            } else {
                PrimaryButtonState.Disabled
            },
            icon = R.drawable.stripe_ic_lock,
            onButtonClick = onPrimaryButtonClick
        )
        SecondaryButton(
            enabled = !primaryButtonState.isBlocking,
            label = stringResource(id = R.string.wallet_pay_another_way),
            onClick = onPayAnotherWayClick
        )
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
            .fillMaxWidth()
            .height(64.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.linkColors.componentBorder,
                shape = MaterialTheme.shapes.large
            )
            .background(
                color = MaterialTheme.linkColors.componentBackground,
                shape = MaterialTheme.shapes.large
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.wallet_collapsed_payment),
            modifier = Modifier.padding(
                start = HorizontalPadding,
                end = 8.dp
            ),
            color = MaterialTheme.linkColors.disabledText
        )
        PaymentDetails(paymentDetails = selectedPaymentMethod, enabled = true)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_link_chevron),
            contentDescription = stringResource(id = R.string.wallet_expand_accessibility),
            modifier = Modifier
                .padding(end = 22.dp)
                .semantics {
                    testTag = "ChevronIcon"
                },
            tint = MaterialTheme.linkColors.disabledText
        )
    }
}

@Composable
private fun ExpandedPaymentDetails(
    paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>,
    supportedTypes: Set<String>,
    selectedItem: ConsumerPaymentDetails.PaymentDetails?,
    enabled: Boolean,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onMenuButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onAddNewPaymentMethodClick: () -> Unit,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.linkColors.componentBorder,
                shape = MaterialTheme.shapes.large
            )
            .background(
                color = MaterialTheme.linkColors.componentBackground,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Row(
            modifier = Modifier
                .height(44.dp)
                .clickable(enabled = enabled, onClick = onCollapse),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.wallet_expanded_title),
                modifier = Modifier
                    .padding(start = HorizontalPadding, top = 20.dp),
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.button
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_link_chevron),
                contentDescription = stringResource(id = R.string.wallet_expand_accessibility),
                modifier = Modifier
                    .padding(top = 20.dp, end = 22.dp)
                    .rotate(180f)
                    .semantics {
                        testTag = "ChevronIcon"
                    },
                tint = MaterialTheme.colors.onPrimary
            )
        }

        // TODO(brnunes-stripe): Use LazyColumn, will need to write custom shape for the border
        // https://juliensalvi.medium.com/custom-shape-with-jetpack-compose-1cb48a991d42
        paymentDetailsList.forEachIndexed { index, item ->
            PaymentDetailsListItem(
                paymentDetails = item,
                enabled = enabled,
                isSupported = supportedTypes.contains(item.type),
                isSelected = selectedItem?.id == item.id,
                onClick = {
                    onItemSelected(item)
                },
                onMenuButtonClick = {
                    onMenuButtonClick(item)
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(enabled = enabled, onClick = onAddNewPaymentMethodClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_link_add),
                contentDescription = null,
                modifier = Modifier.padding(start = HorizontalPadding, end = 12.dp),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(id = R.string.wallet_add_payment_method),
                modifier = Modifier.padding(end = HorizontalPadding, bottom = 4.dp),
                color = MaterialTheme.linkColors.actionLabel,
                style = MaterialTheme.typography.button
            )
        }
    }
}

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
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.stripe.android.link.ui.primaryButtonLabel
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.ui.core.injection.NonFallbackInjector

@Preview
@Composable
private fun WalletBodyPreview() {
    DefaultLinkTheme {
        Surface {
            WalletBody(
                paymentDetails = listOf(
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
                initiallySelectedId = null,
                primaryButtonLabel = "Pay $10.99",
                primaryButtonState = PrimaryButtonState.Enabled,
                errorMessage = null,
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

    val paymentDetails by viewModel.paymentDetails.collectAsState()
    val primaryButtonState by viewModel.primaryButtonState.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()

    WalletBody(
        paymentDetails = paymentDetails,
        initiallySelectedId = null,
        primaryButtonLabel = primaryButtonLabel(viewModel.args, LocalContext.current.resources),
        primaryButtonState = primaryButtonState,
        errorMessage = errorMessage,
        onAddNewPaymentMethodClick = viewModel::addNewPaymentMethod,
        onEditPaymentMethod = viewModel::editPaymentMethod,
        onDeletePaymentMethod = viewModel::deletePaymentMethod,
        onPrimaryButtonClick = viewModel::onSelectedPaymentDetails,
        onPayAnotherWayClick = viewModel::payAnotherWay,
        showBottomSheetContent = showBottomSheetContent
    )
}

@Composable
internal fun WalletBody(
    paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
    initiallySelectedId: String?,
    primaryButtonLabel: String,
    primaryButtonState: PrimaryButtonState,
    errorMessage: ErrorMessage?,
    onAddNewPaymentMethodClick: () -> Unit,
    onEditPaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onDeletePaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onPrimaryButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onPayAnotherWayClick: () -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit
) {
    var isWalletExpanded by rememberSaveable { mutableStateOf(false) }
    var cardBeingRemoved by remember { mutableStateOf<ConsumerPaymentDetails.Card?>(null) }
    var openDialog by remember { mutableStateOf(false) }

    cardBeingRemoved?.let {
        // Launch dialog when the value of [cardBeingRemoved] changes.
        LaunchedEffect(it) {
            openDialog = true
        }

        ConfirmRemoveDialog(openDialog) { confirmed ->
            if (confirmed) {
                onDeletePaymentMethod(it)
            }

            openDialog = false
            cardBeingRemoved = null
        }
    }

    if (paymentDetails.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        ScrollableTopLevelColumn {
            Spacer(modifier = Modifier.height(12.dp))

            var selectedItemId by rememberSaveable {
                mutableStateOf(initiallySelectedId ?: getDefaultSelectedCard(paymentDetails))
            }

            // Update selected item if it's not on the list anymore
            if (paymentDetails.firstOrNull { it.id == selectedItemId } == null) {
                selectedItemId = getDefaultSelectedCard(paymentDetails)
            }

            if (isWalletExpanded) {
                ExpandedPaymentDetails(
                    paymentDetails = paymentDetails,
                    selectedItemId = selectedItemId,
                    enabled = !primaryButtonState.isBlocking,
                    onIndexSelected = {
                        selectedItemId = paymentDetails[it].id
                        isWalletExpanded = false
                    },
                    onMenuButtonClick = {
                        showBottomSheetContent {
                            WalletBottomSheetContent(
                                onCancelClick = {
                                    showBottomSheetContent(null)
                                },
                                onEditClick = {
                                    showBottomSheetContent(null)
                                    onEditPaymentMethod(it)
                                },
                                onRemoveClick = {
                                    showBottomSheetContent(null)
                                    cardBeingRemoved = it
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
                    selectedPaymentMethod = paymentDetails.first { it.id == selectedItemId },
                    enabled = !primaryButtonState.isBlocking,
                    onClick = {
                        isWalletExpanded = true
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            errorMessage?.let {
                ErrorText(text = it.getMessage(LocalContext.current.resources))
            }
            PrimaryButton(
                label = primaryButtonLabel,
                state = primaryButtonState,
                icon = R.drawable.stripe_ic_lock
            ) {
                onPrimaryButtonClick(paymentDetails.first { it.id == selectedItemId })
            }
            SecondaryButton(
                enabled = !primaryButtonState.isBlocking,
                label = stringResource(id = R.string.wallet_pay_another_way),
                onClick = onPayAnotherWayClick
            )
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
            modifier = Modifier.padding(horizontal = HorizontalPadding),
            color = MaterialTheme.linkColors.disabledText
        )
        if (selectedPaymentMethod is ConsumerPaymentDetails.Card) {
            CardDetails(card = selectedPaymentMethod)
        }
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
    paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
    selectedItemId: String,
    enabled: Boolean,
    onIndexSelected: (Int) -> Unit,
    onMenuButtonClick: (ConsumerPaymentDetails.Card) -> Unit,
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

        // TODO(brnunes-stripe): Use LazyColumn.
        paymentDetails.forEachIndexed { index, item ->
            when (item) {
                is ConsumerPaymentDetails.Card -> {
                    CardPaymentMethodItem(
                        cardDetails = item,
                        enabled = enabled,
                        isSelected = selectedItemId == item.id,
                        onClick = {
                            onIndexSelected(index)
                        },
                        onMenuButtonClick = {
                            onMenuButtonClick(item)
                        }
                    )
                }
            }
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

@Composable
private fun CardPaymentMethodItem(
    cardDetails: ConsumerPaymentDetails.Card,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier.padding(start = 20.dp, end = 6.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.linkColors.actionLabelLight,
                unselectedColor = MaterialTheme.linkColors.disabledText
            )
        )
        CardDetails(card = cardDetails)
        Spacer(modifier = Modifier.weight(1f))
        if (cardDetails.isDefault) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colors.secondary,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_default),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    color = MaterialTheme.linkColors.disabledText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(
            onClick = onMenuButtonClick,
            modifier = Modifier.padding(end = 6.dp),
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.edit),
                tint = MaterialTheme.linkColors.actionLabelLight
            )
        }
    }
    Divider(color = MaterialTheme.linkColors.componentDivider, thickness = 1.dp)
}

@Composable
internal fun CardDetails(
    card: ConsumerPaymentDetails.Card
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = card.brand.icon),
            contentDescription = card.brand.displayName,
            modifier = Modifier.padding(horizontal = 6.dp),
            tint = Color.Unspecified
        )
        Text(
            text = "•••• ",
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            text = card.last4,
            color = MaterialTheme.colors.onPrimary
        )
    }
}

private fun getDefaultSelectedCard(paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>) =
    paymentDetails.firstOrNull { it.isDefault }?.id ?: paymentDetails.first().id

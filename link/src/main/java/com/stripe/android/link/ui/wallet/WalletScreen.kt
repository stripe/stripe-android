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
import androidx.compose.runtime.livedata.observeAsState
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
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.PayAnotherWayButton
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.primaryButtonLabel
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails

@Preview
@Composable
private fun WalletBodyPreview() {
    DefaultLinkTheme {
        Surface {
            WalletBody(
                isProcessing = false,
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
                primaryButtonLabel = "Pay $10.99",
                onAddNewPaymentMethodClick = {},
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
    val isProcessing by viewModel.isProcessing.observeAsState(false)

    WalletBody(
        isProcessing = isProcessing,
        paymentDetails = paymentDetails,
        primaryButtonLabel = primaryButtonLabel(viewModel.args, LocalContext.current.resources),
        onAddNewPaymentMethodClick = viewModel::addNewPaymentMethod,
        onDeletePaymentMethod = viewModel::deletePaymentMethod,
        onPrimaryButtonClick = viewModel::onSelectedPaymentDetails,
        onPayAnotherWayClick = viewModel::payAnotherWay,
        showBottomSheetContent = showBottomSheetContent
    )
}

@Composable
internal fun WalletBody(
    isProcessing: Boolean,
    paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
    primaryButtonLabel: String,
    onAddNewPaymentMethodClick: () -> Unit,
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

            var selectedIndex by rememberSaveable {
                mutableStateOf(
                    paymentDetails.indexOfFirst { it.isDefault }
                        .takeUnless { it == -1 } ?: 0
                )
            }

            if (isWalletExpanded) {
                ExpandedPaymentDetails(
                    paymentDetails = paymentDetails,
                    selectedIndex = selectedIndex,
                    enabled = !isProcessing,
                    onIndexSelected = {
                        selectedIndex = it
                    },
                    onMenuButtonClick = {
                        showBottomSheetContent {
                            WalletBottomSheetContent(
                                onCancelClick = {
                                    showBottomSheetContent(null)
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
                    selectedPaymentMethod = paymentDetails[selectedIndex],
                    enabled = !isProcessing,
                    onClick = {
                        isWalletExpanded = true
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                label = primaryButtonLabel,
                state = if (isProcessing) {
                    PrimaryButtonState.Processing
                } else {
                    PrimaryButtonState.Enabled
                },
                icon = R.drawable.stripe_ic_lock
            ) {
                onPrimaryButtonClick(paymentDetails[selectedIndex])
            }
            PayAnotherWayButton(
                enabled = !isProcessing,
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
                enabled = enabled, onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.wallet_pay_with),
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
internal fun ExpandedPaymentDetails(
    paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
    selectedIndex: Int,
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
                        isSelected = selectedIndex == index,
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
                .clickable(onClick = onAddNewPaymentMethodClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_link_add),
                contentDescription = null,
                modifier = Modifier.padding(start = HorizontalPadding, end = 12.dp),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(id = R.string.wallet_add_new_payment_method),
                modifier = Modifier.padding(end = HorizontalPadding, bottom = 4.dp),
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.button
            )
        }
    }
}

@Composable
internal fun CardPaymentMethodItem(
    cardDetails: ConsumerPaymentDetails.Card,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier.padding(start = 20.dp, end = 6.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colors.primary,
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
            modifier = Modifier.padding(end = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.edit),
                tint = MaterialTheme.colors.primary
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

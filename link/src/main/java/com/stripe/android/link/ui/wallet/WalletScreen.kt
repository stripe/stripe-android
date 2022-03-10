package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails

private val horizontalPadding = 20.dp

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
                payButtonLabel = "Pay $10.99",
                onPayButtonClick = {},
                onPayAnotherWayClick = {},
                onAddNewPaymentMethodClick = {}
            )
        }
    }
}

@Composable
internal fun WalletBody(
    linkAccount: LinkAccount,
    injector: NonFallbackInjector
) {
    val viewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.Factory(
            linkAccount,
            injector
        )
    )

    val paymentDetails by viewModel.paymentDetails.collectAsState()

    WalletBody(
        paymentDetails = paymentDetails,
        payButtonLabel = viewModel.payButtonLabel,
        onPayButtonClick = viewModel::completePayment,
        onPayAnotherWayClick = viewModel::payAnotherWay,
        onAddNewPaymentMethodClick = viewModel::addNewPaymentMethod
    )
}

@Composable
internal fun WalletBody(
    paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
    payButtonLabel: String,
    onAddNewPaymentMethodClick: () -> Unit,
    onPayButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onPayAnotherWayClick: () -> Unit
) {
    var isWalletExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (paymentDetails.isEmpty()) {
            CircularProgressIndicator()
        } else {
            var selectedIndex by rememberSaveable {
                mutableStateOf(paymentDetails.indexOfFirst { it.isDefault })
            }

            if (isWalletExpanded) {
                ExpandedPaymentDetails(
                    paymentDetails = paymentDetails,
                    selectedIndex = selectedIndex,
                    onIndexSelected = {
                        selectedIndex = it
                    },
                    onAddNewPaymentMethodClick = onAddNewPaymentMethodClick,
                    onCollapse = {
                        isWalletExpanded = false
                    }
                )
            } else {
                CollapsedPaymentDetails(
                    selectedPaymentMethod = paymentDetails[selectedIndex],
                    onClick = {
                        isWalletExpanded = true
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            PayButton(label = payButtonLabel) {
                onPayButtonClick(paymentDetails[selectedIndex])
            }
            TextButton(
                onClick = onPayAnotherWayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.secondary
                )
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_pay_another_way),
                    color = MaterialTheme.colors.onSecondary
                )
            }
        }
    }
}

@Composable
internal fun CollapsedPaymentDetails(
    selectedPaymentMethod: ConsumerPaymentDetails.PaymentDetails,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onBackground,
                shape = MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.wallet_pay_with),
            modifier = Modifier.padding(horizontal = horizontalPadding),
            color = MaterialTheme.colors.onBackground
        )
        if (selectedPaymentMethod is ConsumerPaymentDetails.Card) {
            CardDetails(card = selectedPaymentMethod)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.ic_link_chevron),
            contentDescription = stringResource(id = R.string.wallet_expand_accessibility),
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .semantics {
                    testTag = "ChevronIcon"
                },
            tint = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
internal fun ExpandedPaymentDetails(
    paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    onAddNewPaymentMethodClick: () -> Unit,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onBackground,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Row(
            modifier = Modifier
                .height(44.dp)
                .padding(start = horizontalPadding, top = 20.dp, end = horizontalPadding)
                .clickable(onClick = onCollapse),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.wallet_expanded_title),
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.button
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_link_chevron),
                contentDescription = stringResource(id = R.string.wallet_expand_accessibility),
                modifier = Modifier
                    .rotate(180f)
                    .semantics {
                        testTag = "ChevronIcon"
                    },
                tint = MaterialTheme.colors.onPrimary
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
        ) {
            itemsIndexed(paymentDetails) { index, item ->
                when (item) {
                    is ConsumerPaymentDetails.Card -> {
                        CardPaymentMethodItem(
                            cardDetails = item,
                            isSelected = selectedIndex == index
                        ) {
                            onIndexSelected(index)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 4.dp)
                .clickable(onClick = onAddNewPaymentMethodClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_link_add),
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(id = R.string.wallet_add_new_payment_method),
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
    onClick: () -> Unit
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
            modifier = Modifier.padding(end = 6.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colors.primary,
                unselectedColor = MaterialTheme.colors.onBackground
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
                    color = MaterialTheme.colors.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    Divider(color = MaterialTheme.colors.onBackground, thickness = 1.dp)
}

@Composable
internal fun PayButton(
    label: String,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        TextButton(
            onClick = onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
        ) {
            Text(
                text = label,
                color = MaterialTheme.colors.onPrimary
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.stripe_ic_lock),
            contentDescription = null,
            modifier = Modifier
                .height(16.dp)
                // width should be 13dp and must include the horizontal padding
                .width(13.dp + 40.dp)
                .padding(horizontal = horizontalPadding),
            tint = MaterialTheme.colors.onPrimary
        )
    }
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

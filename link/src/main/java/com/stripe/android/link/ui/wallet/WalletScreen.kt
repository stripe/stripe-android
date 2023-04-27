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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.R
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.text.Html
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

@Preview
@Composable
private fun WalletBodyPreview() {
    val paymentDetailsList = listOf(
        ConsumerPaymentDetails.Card(
            id = "id1",
            isDefault = false,
            expiryYear = 2030,
            expiryMonth = 12,
            brand = CardBrand.Visa,
            last4 = "4242",
            cvcCheck = CvcCheck.Fail
        ),
        ConsumerPaymentDetails.Card(
            id = "id2",
            isDefault = false,
            expiryYear = 2022,
            expiryMonth = 1,
            brand = CardBrand.MasterCard,
            last4 = "4444",
            cvcCheck = CvcCheck.Pass
        ),
        ConsumerPaymentDetails.BankAccount(
            id = "id2",
            isDefault = true,
            bankIconCode = "icon",
            bankName = "Stripe Bank With Long Name",
            last4 = "6789"
        )
    )

    DefaultLinkTheme {
        Surface {
            WalletBody(
                uiState = WalletUiState(
                    paymentDetailsList = paymentDetailsList,
                    supportedTypes = SupportedPaymentMethod.allTypes,
                    selectedItem = paymentDetailsList[2],
                    isExpanded = true,
                    errorMessage = ErrorMessage.Raw("Something went wrong")
                ),
                primaryButtonLabel = "Pay $10.99",
                expiryDateController = SimpleTextFieldController(textFieldConfig = DateConfig()),
                cvcController = CvcController(cardBrandFlow = flowOf(CardBrand.Visa)),
                setExpanded = {},
                onItemSelected = {},
                onAddNewPaymentMethodClick = {},
                onEditPaymentMethod = {},
                onSetDefault = {},
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

    val uiState by viewModel.uiState.collectAsState()

    uiState.alertMessage?.let { alertMessage ->
        AlertDialog(
            text = { Text(alertMessage.getMessage(LocalContext.current.resources)) },
            onDismissRequest = viewModel::onAlertDismissed,
            confirmButton = {
                TextButton(onClick = viewModel::onAlertDismissed) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        color = MaterialTheme.linkColors.actionLabel
                    )
                }
            }
        )
    }

    if (uiState.paymentDetailsList.isEmpty()) {
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
            uiState = uiState,
            primaryButtonLabel = completePaymentButtonLabel(
                viewModel.args.stripeIntent,
                LocalContext.current.resources
            ),
            expiryDateController = viewModel.expiryDateController,
            cvcController = viewModel.cvcController,
            setExpanded = viewModel::setExpanded,
            onItemSelected = viewModel::onItemSelected,
            onAddNewPaymentMethodClick = viewModel::addNewPaymentMethod,
            onEditPaymentMethod = viewModel::editPaymentMethod,
            onSetDefault = viewModel::setDefault,
            onDeletePaymentMethod = viewModel::deletePaymentMethod,
            onPrimaryButtonClick = viewModel::onConfirmPayment,
            onPayAnotherWayClick = viewModel::payAnotherWay,
            showBottomSheetContent = showBottomSheetContent
        )
    }
}

@Composable
internal fun WalletBody(
    uiState: WalletUiState,
    primaryButtonLabel: String,
    expiryDateController: TextFieldController,
    cvcController: CvcController,
    setExpanded: (Boolean) -> Unit,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onAddNewPaymentMethodClick: () -> Unit,
    onEditPaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onSetDefault: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onDeletePaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onPayAnotherWayClick: () -> Unit,
    showBottomSheetContent: (BottomSheetContent?) -> Unit
) {
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

    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isProcessing) {
        if (uiState.isProcessing) {
            focusManager.clearFocus()
        }
    }

    ScrollableTopLevelColumn {
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.animateContentSize()) {
            if (uiState.isExpanded || uiState.selectedItem == null) {
                ExpandedPaymentDetails(
                    uiState = uiState,
                    onItemSelected = {
                        onItemSelected(it)
                        setExpanded(false)
                    },
                    onMenuButtonClick = {
                        showBottomSheetContent {
                            WalletPaymentMethodMenu(
                                paymentDetails = it,
                                onEditClick = {
                                    showBottomSheetContent(null)
                                    onEditPaymentMethod(it)
                                },
                                onSetDefaultClick = {
                                    showBottomSheetContent(null)
                                    onSetDefault(it)
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
                        setExpanded(false)
                    }
                )
            } else {
                CollapsedPaymentDetails(
                    selectedPaymentMethod = uiState.selectedItem,
                    enabled = !uiState.primaryButtonState.isBlocking,
                    onClick = {
                        setExpanded(true)
                    }
                )
            }
        }

        if (uiState.selectedItem is ConsumerPaymentDetails.BankAccount) {
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

        AnimatedVisibility(visible = uiState.errorMessage != null) {
            ErrorText(
                text = uiState.errorMessage?.getMessage(LocalContext.current.resources).orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }

        uiState.selectedCard?.let { selectedCard ->
            if (selectedCard.requiresCardDetailsRecollection) {
                CardDetailsRecollectionForm(
                    expiryDateController = expiryDateController,
                    cvcController = cvcController,
                    isCardExpired = selectedCard.isExpired,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            label = primaryButtonLabel,
            state = uiState.primaryButtonState,
            onButtonClick = onPrimaryButtonClick,
            iconEnd = R.drawable.stripe_ic_lock
        )

        SecondaryButton(
            enabled = !uiState.primaryButtonState.isBlocking,
            label = stringResource(id = R.string.stripe_wallet_pay_another_way),
            onClick = onPayAnotherWayClick
        )
    }
}

@Composable
internal fun CardDetailsRecollectionForm(
    expiryDateController: TextFieldController,
    cvcController: CvcController,
    isCardExpired: Boolean,
    modifier: Modifier = Modifier
) {
    val rowElement = remember(expiryDateController, cvcController) {
        val rowFields: List<SectionSingleFieldElement> = buildList {
            if (isCardExpired) {
                this += SimpleTextElement(
                    identifier = IdentifierSpec.Generic("date"),
                    controller = expiryDateController
                )
            }

            this += CvcElement(
                _identifier = IdentifierSpec.CardCvc,
                controller = cvcController
            )
        }

        RowElement(
            _identifier = IdentifierSpec.Generic("row_" + UUID.randomUUID().leastSignificantBits),
            fields = rowFields,
            controller = RowController(rowFields)
        )
    }

    val errorTextResId = if (isCardExpired) {
        R.string.stripe_wallet_update_expired_card_error
    } else {
        R.string.stripe_wallet_recollect_cvc_error
    }

    StripeThemeForLink {
        Column(modifier) {
            ErrorText(
                text = stringResource(errorTextResId),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionElementUI(
                enabled = true,
                element = SectionElement.wrap(rowElement),
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = rowElement.fields.last().identifier
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
            text = stringResource(id = R.string.stripe_wallet_collapsed_payment),
            modifier = Modifier.padding(
                start = HorizontalPadding,
                end = 8.dp
            ),
            color = MaterialTheme.linkColors.disabledText
        )
        PaymentDetails(paymentDetails = selectedPaymentMethod, enabled = true)
        Icon(
            painter = painterResource(id = R.drawable.stripe_link_chevron),
            contentDescription = stringResource(id = R.string.stripe_wallet_expand_accessibility),
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
    uiState: WalletUiState,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onMenuButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onAddNewPaymentMethodClick: () -> Unit,
    onCollapse: () -> Unit
) {
    val isEnabled = !uiState.primaryButtonState.isBlocking

    Column(
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
        Row(
            modifier = Modifier
                .height(44.dp)
                .clickable(enabled = isEnabled, onClick = onCollapse),
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
                    .rotate(180f)
                    .semantics {
                        testTag = "ChevronIcon"
                    },
                tint = MaterialTheme.colors.onPrimary
            )
        }

        // TODO(brnunes-stripe): Use LazyColumn, will need to write custom shape for the border
        // https://juliensalvi.medium.com/custom-shape-with-jetpack-compose-1cb48a991d42
        uiState.paymentDetailsList.forEach { item ->
            PaymentDetailsListItem(
                paymentDetails = item,
                enabled = isEnabled,
                isSupported = uiState.supportedTypes.contains(item.type),
                isSelected = uiState.selectedItem?.id == item.id,
                isUpdating = uiState.paymentMethodIdBeingUpdated == item.id,
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
                modifier = Modifier.padding(end = HorizontalPadding, bottom = 4.dp),
                color = MaterialTheme.linkColors.actionLabel,
                style = MaterialTheme.typography.button
            )
        }
    }
}

private fun String.replaceHyperlinks() = this.replace(
    "<terms>",
    "<a href=\"https://stripe.com/legal/ach-payments/authorization\">"
).replace("</terms>", "</a>")

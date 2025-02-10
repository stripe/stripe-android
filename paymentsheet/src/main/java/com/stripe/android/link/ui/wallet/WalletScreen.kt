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
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.text.Html
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun WalletScreen(
    viewModel: WalletViewModel,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    WalletBody(
        state = state,
        isExpanded = isExpanded,
        expiryDateController = viewModel.expiryDateController,
        cvcController = viewModel.cvcController,
        onItemSelected = viewModel::onItemSelected,
        onExpandedChanged = { expanded ->
            isExpanded = expanded
        },
        onPrimaryButtonClick = viewModel::onPrimaryButtonClicked,
        onPayAnotherWayClicked = viewModel::onPayAnotherWayClicked,
        onRemoveClicked = viewModel::onRemoveClicked,
        onEditPaymentMethodClicked = viewModel::onEditPaymentMethodClicked,
        onSetDefaultClicked = viewModel::onSetDefaultClicked,
        showBottomSheetContent = showBottomSheetContent,
        hideBottomSheetContent = hideBottomSheetContent,
        onAddNewPaymentMethodClicked = viewModel::onAddNewPaymentMethodClicked,
        onDismissAlert = viewModel::onDismissAlert
    )
}

@Composable
internal fun WalletBody(
    state: WalletUiState,
    isExpanded: Boolean,
    expiryDateController: TextFieldController,
    cvcController: CvcController,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onAddNewPaymentMethodClicked: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onPayAnotherWayClicked: () -> Unit,
    onDismissAlert: () -> Unit,
    onEditPaymentMethodClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onSetDefaultClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onRemoveClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: () -> Unit
) {
    if (state.paymentDetailsList.isEmpty()) {
        Loader()
        return
    }

    if (state.alertMessage != null) {
        AlertMessage(
            alertMessage = state.alertMessage,
            onDismissAlert = onDismissAlert
        )
    }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isProcessing) {
        if (state.isProcessing) {
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .testTag(WALLET_SCREEN_BOX)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PaymentDetailsSection(
            modifier = Modifier
                .weight(
                    weight = 1f,
                    fill = false
                ),
            state = state,
            isExpanded = isExpanded,
            expiryDateController = expiryDateController,
            cvcController = cvcController,
            onItemSelected = onItemSelected,
            onExpandedChanged = onExpandedChanged,
            showBottomSheetContent = showBottomSheetContent,
            onRemoveClicked = onRemoveClicked,
            onSetDefaultClicked = onSetDefaultClicked,
            onEditPaymentMethodClicked = onEditPaymentMethodClicked,
            onAddNewPaymentMethodClicked = onAddNewPaymentMethodClicked,
            hideBottomSheetContent = hideBottomSheetContent
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActionSection(
            state = state,
            onPrimaryButtonClick = onPrimaryButtonClick,
            onPayAnotherWayClicked = onPayAnotherWayClicked
        )
    }
}

@Composable
private fun PaymentDetailsSection(
    modifier: Modifier,
    state: WalletUiState,
    isExpanded: Boolean,
    expiryDateController: TextFieldController,
    cvcController: CvcController,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onAddNewPaymentMethodClicked: () -> Unit,
    onEditPaymentMethodClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onSetDefaultClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onRemoveClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        PaymentMethodSection(
            state = state,
            isExpanded = isExpanded,
            onItemSelected = onItemSelected,
            onExpandedChanged = onExpandedChanged,
            showBottomSheetContent = showBottomSheetContent,
            onRemoveClicked = onRemoveClicked,
            onSetDefaultClicked = onSetDefaultClicked,
            onEditPaymentMethodClicked = onEditPaymentMethodClicked,
            onAddNewPaymentMethodClicked = onAddNewPaymentMethodClicked,
            hideBottomSheetContent = hideBottomSheetContent
        )

        AnimatedVisibility(state.showBankAccountTerms) {
            BankAccountTerms()
        }

        ErrorSection(state.errorMessage)

        state.selectedCard?.let { selectedCard ->
            if (selectedCard.requiresCardDetailsRecollection) {
                Spacer(modifier = Modifier.height(16.dp))

                CardDetailsRecollectionForm(
                    paymentDetails = selectedCard,
                    expiryDateController = expiryDateController,
                    cvcController = cvcController,
                    isCardExpired = selectedCard.isExpired
                )
            }
        }
    }
}

@Composable
private fun ErrorSection(errorMessage: ResolvableString?) {
    AnimatedVisibility(
        visible = errorMessage != null
    ) {
        if (errorMessage != null) {
            ErrorText(
                text = errorMessage.resolve(LocalContext.current),
                modifier = Modifier
                    .testTag(WALLET_SCREEN_ERROR_TAG)
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ActionSection(
    state: WalletUiState,
    onPrimaryButtonClick: () -> Unit,
    onPayAnotherWayClicked: () -> Unit
) {
    Column {
        PrimaryButton(
            modifier = Modifier
                .testTag(WALLET_SCREEN_PAY_BUTTON),
            label = state.primaryButtonLabel.resolve(LocalContext.current),
            state = state.primaryButtonState,
            onButtonClick = onPrimaryButtonClick,
            iconEnd = com.stripe.android.ui.core.R.drawable.stripe_ic_lock
        )

        SecondaryButton(
            modifier = Modifier
                .testTag(WALLET_SCREEN_PAY_ANOTHER_WAY_BUTTON),
            enabled = !state.primaryButtonState.isBlocking,
            label = stringResource(id = R.string.stripe_wallet_pay_another_way),
            onClick = onPayAnotherWayClicked
        )
    }
}

@Composable
private fun PaymentMethodSection(
    state: WalletUiState,
    isExpanded: Boolean,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onAddNewPaymentMethodClicked: () -> Unit,
    onEditPaymentMethodClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onSetDefaultClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onRemoveClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: () -> Unit
) {
    Box(
        modifier = Modifier
            .animateContentSize()
    ) {
        val selectedItem = state.selectedItem
        if (isExpanded || selectedItem == null) {
            ExpandedPaymentDetails(
                uiState = state,
                onItemSelected = onItemSelected,
                onMenuButtonClick = {
                    showBottomSheetContent {
                        WalletPaymentMethodMenu(
                            modifier = Modifier
                                .testTag(WALLET_SCREEN_MENU_SHEET_TAG),
                            paymentDetails = it,
                            onEditClick = {
                                hideBottomSheetContent()
                                onEditPaymentMethodClicked(it)
                            },
                            onSetDefaultClick = {
                                hideBottomSheetContent()
                                onSetDefaultClicked(it)
                            },
                            onRemoveClick = {
                                hideBottomSheetContent()
                                onRemoveClicked(it)
                            },
                            onCancelClick = {
                                hideBottomSheetContent()
                            }
                        )
                    }
                },
                onAddNewPaymentMethodClick = onAddNewPaymentMethodClicked,
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
}

@Composable
internal fun CollapsedPaymentDetails(
    selectedPaymentMethod: ConsumerPaymentDetails.PaymentDetails,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column {
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
        ExpandedRowHeader(
            isEnabled = isEnabled,
            onCollapse = onCollapse
        )

        Column(
            modifier = Modifier
                .weight(
                    weight = 1f,
                    fill = false
                )
        ) {
            PaymentDetailsList(
                uiState = uiState,
                onItemSelected = onItemSelected,
                onMenuButtonClick = onMenuButtonClick
            )
        }

        AddPaymentMethodRow(
            isEnabled = isEnabled,
            onAddNewPaymentMethodClick = onAddNewPaymentMethodClick
        )
    }
}

@Composable
private fun PaymentDetailsList(
    uiState: WalletUiState,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onMenuButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
) {
    val isEnabled = !uiState.primaryButtonState.isBlocking

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
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
            text = stringResource(R.string.stripe_wallet_expanded_title),
            modifier = Modifier
                .padding(start = HorizontalPadding, top = 20.dp),
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.button
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.stripe_link_chevron),
            contentDescription = stringResource(R.string.stripe_wallet_expand_accessibility),
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
            painter = painterResource(R.drawable.stripe_link_add_green),
            contentDescription = null,
            modifier = Modifier.padding(start = HorizontalPadding, end = 12.dp),
            tint = Color.Unspecified
        )
        Text(
            text = stringResource(R.string.stripe_add_payment_method),
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
        style = MaterialTheme.typography.caption.copy(
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        urlSpanStyle = SpanStyle(
            color = MaterialTheme.colors.primary
        )
    )
}

@Composable
internal fun CardDetailsRecollectionForm(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    expiryDateController: TextFieldController,
    cvcController: CvcController,
    isCardExpired: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rowElement = remember(paymentDetails) {
        val rowFields = buildList {
            if (isCardExpired) {
                add(
                    element = SimpleTextElement(
                        identifier = IdentifierSpec.Generic("date"),
                        controller = expiryDateController
                    )
                )
            }

            add(
                element = CvcElement(
                    _identifier = IdentifierSpec.CardCvc,
                    controller = cvcController
                )
            )
        }

        RowElement(
            _identifier = IdentifierSpec.Generic(paymentDetails.id),
            fields = rowFields,
            controller = RowController(rowFields)
        )
    }

    val errorTextRes = if (isCardExpired) {
        R.string.stripe_wallet_update_expired_card_error
    } else {
        R.string.stripe_wallet_recollect_cvc_error
    }.resolvableString

    Column(modifier) {
        ErrorText(
            text = errorTextRes.resolve(context),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WALLET_SCREEN_RECOLLECTION_FORM_ERROR)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionElementUI(
            modifier = Modifier
                .testTag(WALLET_SCREEN_RECOLLECTION_FORM_FIELDS),
            enabled = true,
            element = SectionElement.wrap(rowElement),
            hiddenIdentifiers = emptySet(),
            lastTextFieldIdentifier = rowElement.fields.last().identifier
        )
    }
}

@Composable
private fun Loader() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(WALLET_LOADER_TAG),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AlertMessage(
    alertMessage: ResolvableString,
    onDismissAlert: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        modifier = Modifier
            .testTag(WALLET_SCREEN_DIALOG_TAG),
        text = { Text(alertMessage.resolve(context)) },
        onDismissRequest = onDismissAlert,
        confirmButton = {
            TextButton(
                modifier = Modifier
                    .testTag(WALLET_SCREEN_DIALOG_BUTTON_TAG),
                onClick = onDismissAlert
            ) {
                Text(
                    text = android.R.string.ok.resolvableString.resolve(context),
                    color = MaterialTheme.linkColors.actionLabel
                )
            }
        }
    )
}

private fun String.replaceHyperlinks() = this.replace(
    "<terms>",
    "<a href=\"https://link.com/terms/ach-authorization\">"
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
internal const val WALLET_SCREEN_PAY_BUTTON = "wallet_screen_pay_button"
internal const val WALLET_SCREEN_PAY_ANOTHER_WAY_BUTTON = "wallet_screen_pay_another_way_button"
internal const val WALLET_SCREEN_RECOLLECTION_FORM_ERROR = "wallet_screen_recollection_form_error"
internal const val WALLET_SCREEN_RECOLLECTION_FORM_FIELDS = "wallet_screen_recollection_form_fields"
internal const val WALLET_SCREEN_BOX = "wallet_screen_box"
internal const val WALLET_SCREEN_MENU_SHEET_TAG = "wallet_screen_menu_sheet_tag"
internal const val WALLET_SCREEN_DIALOG_TAG = "wallet_screen_dialog_tag"
internal const val WALLET_SCREEN_DIALOG_BUTTON_TAG = "wallet_screen_dialog_button_tag"
internal const val WALLET_SCREEN_ERROR_TAG = "wallet_screen_error_tag"

package com.stripe.android.link.ui.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.link.ui.wallet.WalletUiState.ViewEffect
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.ScrollableColumn
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
import com.stripe.android.ui.core.R as PaymentsUiCoreR
import com.stripe.android.uicore.R as StripeUiCoreR

@Composable
internal fun WalletScreen(
    viewModel: WalletViewModel,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.viewEffect) {
        when (state.viewEffect) {
            is ViewEffect.ShowAddPaymentMethodMenu -> {
                showBottomSheetContent(AddPaymentMethodMenu())
            }
            null -> Unit
        }
        viewModel.onViewEffectLaunched()
    }

    WalletBody(
        state = state,
        expiryDateController = viewModel.expiryDateController,
        cvcController = viewModel.cvcController,
        onItemSelected = viewModel::onItemSelected,
        onExpandedChanged = viewModel::onExpandedChanged,
        onPrimaryButtonClick = viewModel::onPrimaryButtonClicked,
        onPayAnotherWayClicked = viewModel::onPayAnotherWayClicked,
        onRemoveClicked = viewModel::onRemoveClicked,
        onUpdateClicked = viewModel::onUpdateClicked,
        onSetDefaultClicked = viewModel::onSetDefaultClicked,
        showBottomSheetContent = showBottomSheetContent,
        hideBottomSheetContent = hideBottomSheetContent,
        onAddNewPaymentMethodClicked = viewModel::onAddNewPaymentMethodClicked,
        onDismissAlert = viewModel::onDismissAlert
    )
}

@Composable
private fun AddPaymentMethodMenu() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
        Text("Test")
        Divider()
        Text("Test")
        Divider()
        Text("Test")
    }

}

@Composable
internal fun WalletBody(
    state: WalletUiState,
    expiryDateController: TextFieldController,
    cvcController: CvcController,
    onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onAddNewPaymentMethodClicked: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onPayAnotherWayClicked: () -> Unit,
    onDismissAlert: () -> Unit,
    onSetDefaultClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onRemoveClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onUpdateClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
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

    // TODO(tillh-stripe): Replace this with ScrollableTopLevelColumn
    ScrollableColumn(
        modifier = Modifier
            .testTag(WALLET_SCREEN_BOX)
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    ) {
        PaymentDetailsSection(
            modifier = Modifier,
            state = state,
            isExpanded = state.isExpanded,
            expiryDateController = expiryDateController,
            cvcController = cvcController,
            onItemSelected = onItemSelected,
            onExpandedChanged = onExpandedChanged,
            showBottomSheetContent = showBottomSheetContent,
            onRemoveClicked = onRemoveClicked,
            onUpdateClicked = onUpdateClicked,
            onSetDefaultClicked = onSetDefaultClicked,
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
    onSetDefaultClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onRemoveClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onUpdateClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
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
            onUpdateClicked = onUpdateClicked,
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
                .testTag(WALLET_SCREEN_PAY_BUTTON)
                .padding(top = 16.dp, bottom = 8.dp),
            label = state.primaryButtonLabel.resolve(LocalContext.current),
            state = state.primaryButtonState,
            onButtonClick = onPrimaryButtonClick,
            iconEnd = PaymentsUiCoreR.drawable.stripe_ic_lock
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
    onSetDefaultClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onRemoveClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    onUpdateClicked: (ConsumerPaymentDetails.PaymentDetails) -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: () -> Unit
) {
    val emailLabel = stringResource(StripeUiCoreR.string.stripe_email)
    val paymentLabel = stringResource(R.string.stripe_wallet_collapsed_payment)

    val labelMaxWidthDp = computeMaxLabelWidth(emailLabel, paymentLabel)
    PaymentMethodPicker(
        email = state.email,
        expanded = isExpanded,
        selectedItem = state.selectedItem,
        emailLabel = emailLabel,
        labelMaxWidth = labelMaxWidthDp,
        expandedContent = {
            ExpandedPaymentDetails(
                uiState = state,
                onItemSelected = onItemSelected,
                onMenuButtonClick = {
                    showBottomSheetContent {
                        WalletPaymentMethodMenu(
                            modifier = Modifier.testTag(WALLET_SCREEN_MENU_SHEET_TAG),
                            paymentDetails = it,
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
                            },
                            onUpdateClick = {
                                hideBottomSheetContent()
                                onUpdateClicked(it)
                            }
                        )
                    }
                },
                onAddNewPaymentMethodClick = onAddNewPaymentMethodClicked,
                onCollapse = {
                    onExpandedChanged(false)
                }
            )
        },
        collapsedContent = { selectedItem ->
            CollapsedPaymentDetails(
                selectedPaymentMethod = selectedItem,
                enabled = !state.primaryButtonState.isBlocking,
                label = paymentLabel,
                labelMaxWidth = labelMaxWidthDp,
                onClick = {
                    onExpandedChanged(true)
                },
            )
        },
    )
}

@Composable
private fun computeMaxLabelWidth(vararg labels: String): Dp {
    val textStyle = LocalTextStyle.current
    val textMeasurer = rememberTextMeasurer()

    val maxLabelSize = labels.maxOfOrNull { label ->
        textMeasurer.measure(style = textStyle, text = label).size.width
    } ?: 0

    val labelMaxWidthDp = with(LocalDensity.current) {
        maxLabelSize.toDp()
    }

    return labelMaxWidthDp
}

@Composable
private fun PaymentMethodPicker(
    email: String,
    emailLabel: String,
    labelMaxWidth: Dp,
    expanded: Boolean,
    selectedItem: ConsumerPaymentDetails.PaymentDetails?,
    modifier: Modifier = Modifier,
    collapsedContent: @Composable ((ConsumerPaymentDetails.PaymentDetails) -> Unit),
    expandedContent: @Composable (() -> Unit),
) {
    Column(
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = LinkTheme.colors.componentBorder,
                shape = LinkTheme.shapes.large
            )
            .clip(LinkTheme.shapes.large)
            .background(
                color = LinkTheme.colors.componentBackground,
                shape = LinkTheme.shapes.large
            )
    ) {
        EmailDetails(
            email = email,
            label = emailLabel,
            labelMaxWidth = labelMaxWidth
        )

        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = LinkTheme.colors.componentBorder,
        )

        if (expanded || selectedItem == null) {
            expandedContent()
        } else {
            collapsedContent(selectedItem)
        }
    }
}

@Composable
private fun CollapsedPaymentDetails(
    selectedPaymentMethod: ConsumerPaymentDetails.PaymentDetails,
    enabled: Boolean,
    label: String,
    labelMaxWidth: Dp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .testTag(COLLAPSED_WALLET_ROW)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp)
            .padding(start = HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .testTag(COLLAPSED_WALLET_HEADER_TAG)
                .width(labelMaxWidth),
            color = LinkTheme.colors.disabledText,
        )

        PaymentDetails(
            modifier = Modifier.testTag(COLLAPSED_WALLET_PAYMENT_DETAILS_TAG),
            paymentDetails = selectedPaymentMethod
        )

        Icon(
            painter = painterResource(R.drawable.stripe_link_chevron),
            contentDescription = stringResource(R.string.stripe_wallet_expand_accessibility),
            modifier = Modifier
                .padding(end = 22.dp)
                .testTag(COLLAPSED_WALLET_CHEVRON_ICON_TAG),
            tint = LinkTheme.colors.disabledText
        )
    }
}

@Composable
private fun EmailDetails(
    email: String,
    label: String,
    labelMaxWidth: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(
                vertical = 16.dp,
                horizontal = HorizontalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier.width(labelMaxWidth),
            text = label,
            color = LinkTheme.colors.disabledText,
        )

        Text(
            text = email,
            color = LinkTheme.colors.textPrimary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = LinkTheme.typography.bodyEmphasized,
            modifier = Modifier.weight(1f),
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

    Column(modifier = Modifier.fillMaxWidth()) {
        ExpandedRowHeader(
            isEnabled = isEnabled,
            onCollapse = onCollapse
        )

        uiState.paymentDetailsList.forEachIndexed { index, item ->
            PaymentDetailsListItem(
                modifier = Modifier
                    .testTag(WALLET_SCREEN_PAYMENT_METHODS_LIST),
                paymentDetails = item,
                enabled = isEnabled,
                isSelected = uiState.selectedItem?.id == item.id,
                isUpdating = uiState.cardBeingUpdated == item.id,
                onClick = { onItemSelected(item) },
                onMenuButtonClick = { onMenuButtonClick(item) }
            )

            if (index != uiState.paymentDetailsList.lastIndex || uiState.canAddNewPaymentMethod) {
                Divider(
                    color = LinkTheme.colors.componentBorder,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        if (uiState.canAddNewPaymentMethod) {
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
            .fillMaxWidth()
            .clickable(
                enabled = isEnabled,
                onClick = onCollapse,
            )
            .padding(start = 20.dp, end = 22.dp)
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.stripe_wallet_expanded_title),
            color = LinkTheme.colors.textPrimary,
            style = LinkTheme.typography.bodyEmphasized,
        )
        Icon(
            painter = painterResource(id = R.drawable.stripe_link_chevron),
            contentDescription = stringResource(R.string.stripe_wallet_expand_accessibility),
            modifier = Modifier.rotate(CHEVRON_ICON_ROTATION),
            tint = LinkTheme.colors.textPrimary,
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
            color = LinkTheme.colors.actionLabel,
            style = LinkTheme.typography.bodyEmphasized,
        )
    }
}

@Composable
private fun BankAccountTerms() {
    Html(
        html = stringResource(R.string.stripe_wallet_bank_account_terms).replaceHyperlinks(),
        color = LinkTheme.colors.textSecondary,
        style = LinkTheme.typography.caption.copy(
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        urlSpanStyle = SpanStyle(
            color = LinkTheme.colors.primary,
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
                    color = LinkTheme.colors.actionLabel
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

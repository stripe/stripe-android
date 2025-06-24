package com.stripe.android.link.ui.wallet

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.stripe.android.link.theme.LinkThemeConfig.contentOnPrimaryButton
import com.stripe.android.link.theme.PrimaryButtonHeight
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkAppBarMenu
import com.stripe.android.link.ui.LinkDivider
import com.stripe.android.link.ui.LinkLoadingScreen
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.link.utils.LinkScreenTransition
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.AddressLauncher.AdditionalFieldsConfiguration
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.text.Html
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch
import com.stripe.android.ui.core.R as PaymentsUiCoreR
import com.stripe.android.uicore.R as StripeUiCoreR

@Composable
internal fun WalletScreen(
    viewModel: WalletViewModel,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: suspend () -> Unit,
    onLogoutClicked: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    
    val addressLauncher = rememberAddressLauncher(
//        callback = viewModel::handleAddressResult,
        callback = {
            Log.d("WalletScreen", "$it")
        },
    )
    
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
        onLogoutClicked = onLogoutClicked,
        onSetDefaultClicked = viewModel::onSetDefaultClicked,
        showBottomSheetContent = showBottomSheetContent,
        hideBottomSheetContent = hideBottomSheetContent,
        onAddNewPaymentMethodClicked = viewModel::onAddNewPaymentMethodClicked,
        onDismissAlert = viewModel::onDismissAlert,
        addressLauncher = addressLauncher,
        onAddressSelected = viewModel::onAddressSelected,
        onShippingAddressesExpandedChanged = viewModel::onShippingAddressesExpandedChanged,
    )
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
    onLogoutClicked: () -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: suspend () -> Unit,
    addressLauncher: AddressLauncher,
    onAddressSelected: (ConsumerShippingAddress) -> Unit,
    onShippingAddressesExpandedChanged: (Boolean) -> Unit,
) {
    AnimatedContent(
        targetState = state.paymentDetailsList.isEmpty(),
        transitionSpec = { LinkScreenTransition },
    ) { isLoading ->
        if (isLoading) {
            LinkLoadingScreen(Modifier.testTag(WALLET_LOADER_TAG))
        } else {
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

            ScrollableTopLevelColumn {
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
                    onLogoutClicked = onLogoutClicked,
                    onSetDefaultClicked = onSetDefaultClicked,
                    onAddNewPaymentMethodClicked = onAddNewPaymentMethodClicked,
                    hideBottomSheetContent = hideBottomSheetContent,
                    addressLauncher = addressLauncher,
                    onAddressSelected = onAddressSelected,
                    onShippingAddressesExpandedChanged = onShippingAddressesExpandedChanged,
                )

                Spacer(modifier = Modifier.height(16.dp))

                ActionSection(
                    state = state,
                    onPrimaryButtonClick = onPrimaryButtonClick,
                    onPayAnotherWayClicked = onPayAnotherWayClicked
                )
            }
        }
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
    onLogoutClicked: () -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: suspend () -> Unit,
    addressLauncher: AddressLauncher,
    onAddressSelected: (ConsumerShippingAddress) -> Unit,
    onShippingAddressesExpandedChanged: (Boolean) -> Unit,
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
            hideBottomSheetContent = hideBottomSheetContent,
            onLogoutClicked = onLogoutClicked,
            addressLauncher = addressLauncher,
            onAddressSelected = onAddressSelected,
            onShippingAddressesExpandedChanged = onShippingAddressesExpandedChanged,
        )

        AnimatedVisibility(visible = state.mandate != null) {
            state.mandate?.let { mandate ->
                LinkMandate(mandate.resolve())
            }
        }

        ErrorSection(state.errorMessage)

        state.selectedCard?.let { selectedCard ->
            if (selectedCard.requiresCardDetailsRecollection) {
                Spacer(modifier = Modifier.height(16.dp))
                StripeThemeForLink {
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
            label = state.primaryButtonLabel.resolve(),
            state = state.primaryButtonState,
            onButtonClick = onPrimaryButtonClick,
            iconEnd = PaymentsUiCoreR.drawable.stripe_ic_lock
        )

        SecondaryButton(
            modifier = Modifier
                .testTag(WALLET_SCREEN_PAY_ANOTHER_WAY_BUTTON),
            enabled = !state.primaryButtonState.isBlocking,
            label = state.secondaryButtonLabel.resolve(),
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
    onLogoutClicked: () -> Unit,
    showBottomSheetContent: (BottomSheetContent) -> Unit,
    hideBottomSheetContent: suspend () -> Unit,
    addressLauncher: AddressLauncher,
    onAddressSelected: (ConsumerShippingAddress) -> Unit,
    onShippingAddressesExpandedChanged: (Boolean) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val emailLabel = stringResource(StripeUiCoreR.string.stripe_email)
    val paymentLabel = stringResource(R.string.stripe_wallet_collapsed_payment)

    val labelMaxWidthDp = computeMaxLabelWidth(emailLabel, paymentLabel)

    PaymentMethodPicker(
        email = state.email,
        expanded = isExpanded,
        selectedItem = state.selectedItem,
        emailLabel = emailLabel,
        labelMaxWidth = labelMaxWidthDp,
        onAccountMenuClicked = {
            showBottomSheetContent {
                LinkAppBarMenu(onLogoutClicked)
            }
        },
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
                                coroutineScope.launch {
                                    hideBottomSheetContent()
                                    onSetDefaultClicked(it)
                                }
                            },
                            onRemoveClick = {
                                coroutineScope.launch {
                                    hideBottomSheetContent()
                                    onRemoveClicked(it)
                                }
                            },
                            onUpdateClick = {
                                coroutineScope.launch {
                                    hideBottomSheetContent()
                                    onUpdateClicked(it)
                                }
                            }
                        )
                    }
                },
                onAddNewPaymentMethodClick = onAddNewPaymentMethodClicked,
                onCollapse = {
                    onExpandedChanged(false)
                },
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
        addressLauncher = addressLauncher,
        selectedAddress = state.selectedShippingAddress,
        shippingAddresses = state.shippingAddresses,
        onAddressSelected = onAddressSelected,
        shippingAddressesExpanded = state.shippingAddressesExpanded,
        onShippingAddressesExpandedChanged = onShippingAddressesExpandedChanged,
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
    onAccountMenuClicked: () -> Unit,
    collapsedContent: @Composable ((ConsumerPaymentDetails.PaymentDetails) -> Unit),
    expandedContent: @Composable (() -> Unit),
    addressLauncher: AddressLauncher,
    selectedAddress: ConsumerShippingAddress?,
    shippingAddresses: List<ConsumerShippingAddress>,
    onAddressSelected: (ConsumerShippingAddress) -> Unit,
    shippingAddressesExpanded: Boolean,
    onShippingAddressesExpandedChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LinkTheme.shapes.default)
            .background(
                color = LinkTheme.colors.surfaceSecondary,
                shape = LinkTheme.shapes.default
            )
    ) {
        EmailDetails(
            email = email,
            label = emailLabel,
            labelMaxWidth = labelMaxWidth,
            onMenuClicked = onAccountMenuClicked,
        )

        LinkDivider()

        ShippingAddressRow(
            labelMaxWidth = labelMaxWidth,
            isExpanded = expanded,
            addressLauncher = addressLauncher,
            selectedAddress = selectedAddress,
            shippingAddresses = shippingAddresses,
            onAddressSelected = onAddressSelected,
            shippingAddressesExpanded = shippingAddressesExpanded,
            onShippingAddressesExpandedChanged = onShippingAddressesExpandedChanged,
        )

        LinkDivider()

        AnimatedContent(
            targetState = expanded || selectedItem == null,
            transitionSpec = {
                if (targetState) {
                    // Expanding
                    (fadeIn() + expandVertically(expandFrom = Alignment.Top)) togetherWith fadeOut()
                } else {
                    // Collapsing
                    fadeIn() togetherWith (fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top))
                }
            }
        ) { showExpanded ->
            if (showExpanded) {
                expandedContent()
            } else {
                collapsedContent(selectedItem!!)
            }
        }
    }
}

@Composable
private fun ShippingAddressRow(
    labelMaxWidth: Dp,
    isExpanded: Boolean,
    addressLauncher: AddressLauncher,
    selectedAddress: ConsumerShippingAddress?,
    shippingAddresses: List<ConsumerShippingAddress>,
    onAddressSelected: (ConsumerShippingAddress) -> Unit,
    shippingAddressesExpanded: Boolean,
    onShippingAddressesExpandedChanged: (Boolean) -> Unit,
) {
    Column {
        AnimatedContent(
            targetState = shippingAddressesExpanded,
            transitionSpec = {
                if (targetState) {
                    (fadeIn() + expandVertically(expandFrom = Alignment.Top)) togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith (fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top))
                }
            }
        ) { expanded ->
            if (!expanded && selectedAddress != null) {
                // Collapsed row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp)
                        .clickable { onShippingAddressesExpandedChanged(true) }
                        .padding(vertical = 16.dp)
                        .padding(start = HorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Shipping",
                        modifier = Modifier.width(labelMaxWidth),
                        color = LinkTheme.colors.textTertiary,
                    )

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = selectedAddress.address.name ?: "",
                            color = LinkTheme.colors.textPrimary,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = LinkTheme.typography.bodyEmphasized,
                        )

                        selectedAddress.address.line1?.let {
                            Text(
                                text = it,
                                color = LinkTheme.colors.textTertiary,
                                style = LinkTheme.typography.detail,
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.stripe_link_chevron),
                        contentDescription = "Expand shipping address",
                        modifier = Modifier
                            .padding(end = 22.dp),
                        tint = LinkTheme.colors.iconTertiary
                    )
                }
            } else {
                // Expanded section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShippingAddressesExpandedChanged(false) }
                            .padding(start = 20.dp, end = 22.dp)
                            .padding(top = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Shipping addresses",
                            color = LinkTheme.colors.textTertiary,
                            style = LinkTheme.typography.body
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.stripe_link_chevron),
                            contentDescription = "Collapse shipping address",
                            modifier = Modifier.rotate(CHEVRON_ICON_ROTATION),
                            tint = LinkTheme.colors.iconTertiary,
                        )
                    }

                    // Display existing addresses
                    shippingAddresses.forEach { address ->
                        AddressInfo(
                            address = address,
                            onClick = onAddressSelected,
                        )
                        LinkDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    }

                    val linkColors = LinkTheme.colors

                    AddShippingAddressRow(
                        isEnabled = true,
                        onClick = {
                            val colors = PaymentSheet.Colors(
                                primary = linkColors.borderSelected,
                                surface = linkColors.surfacePrimary,
                                component = linkColors.surfaceSecondary,
                                componentBorder = linkColors.surfaceSecondary,
                                componentDivider = linkColors.borderDefault,
                                onComponent = linkColors.textPrimary,
                                subtitle = linkColors.textSecondary, // TODO
                                placeholderText = linkColors.textTertiary,
                                onSurface = linkColors.textPrimary,
                                appBarIcon = linkColors.iconTertiary,
                                error = linkColors.textCritical,
                            )

                            // TODO: Colors dark

                            val primaryButtonColors = PaymentSheet.PrimaryButtonColors(
                                background = linkColors.buttonBrand,
                                onBackground = linkColors.contentOnPrimaryButton,
                                border = linkColors.buttonBrand,
                            )

                            val config = AddressLauncher.Configuration.Builder()
                                .title("Add a shipping address")
                                .buttonTitle("Save shipping address")
                                .additionalFields(
                                    AdditionalFieldsConfiguration(
                                        phone = AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                                        checkboxLabel = null,
                                    )
                                )
                                .appearance(
                                    appearance = PaymentSheet.Appearance.Builder()
                                        .colorsLight(colors)
                                        .colorsDark(colors)
                                        .shapes(
                                            PaymentSheet.Shapes(
                                                cornerRadiusDp = 12f,
                                                borderStrokeWidthDp = 1.5f,
                                            )
                                        )
                                        .primaryButton(
                                            PaymentSheet.PrimaryButton(
                                                shape = PaymentSheet.PrimaryButtonShape(
                                                    cornerRadiusDp = 12f,
                                                    heightDp = PrimaryButtonHeight.value,
                                                ),
                                                colorsLight = primaryButtonColors,
                                                colorsDark = primaryButtonColors,
                                            )
                                        )
                                        .build()
                                )
                                .build()

                            addressLauncher.present(
                                publishableKey = "pk_test_placeholder", // You'll fill this in
                                configuration = config,
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressInfo(
    address: ConsumerShippingAddress,
    onClick: (ConsumerShippingAddress) -> Unit,
) {
    val name = address.address.name ?: ""
    val line1 = address.address.line1 ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(address)
            }
            .padding(horizontal = HorizontalPadding)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = LinkTheme.typography.bodyEmphasized,
            color = LinkTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (line1.isNotBlank()) {
            Text(
                text = line1,
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            color = LinkTheme.colors.textTertiary,
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
            tint = LinkTheme.colors.iconTertiary
        )
    }
}

@Composable
private fun EmailDetails(
    email: String,
    label: String,
    labelMaxWidth: Dp,
    onMenuClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(
                top = 16.dp,
                start = 20.dp,
                end = 14.dp,
                bottom = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier.width(labelMaxWidth),
            text = label,
            color = LinkTheme.colors.textTertiary,
        )

        Text(
            text = email,
            color = LinkTheme.colors.textPrimary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = LinkTheme.typography.bodyEmphasized,
            modifier = Modifier.weight(1f),
        )

        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.stripe_show_menu),
            tint = LinkTheme.colors.iconSecondary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onMenuClicked)
                .padding(4.dp),
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
    val isInteractionEnabled = !uiState.primaryButtonState.isBlocking

    Column(modifier = Modifier.fillMaxWidth()) {
        ExpandedRowHeader(
            isEnabled = isInteractionEnabled,
            onCollapse = onCollapse
        )

        uiState.paymentDetailsList.forEachIndexed { index, item ->
            val isItemAvailable = uiState.isItemAvailable(item)
            PaymentDetailsListItem(
                modifier = Modifier
                    .testTag(WALLET_SCREEN_PAYMENT_METHODS_LIST),
                paymentDetails = item,
                isClickable = isInteractionEnabled && isItemAvailable,
                isMenuButtonClickable = isInteractionEnabled,
                isAvailable = isItemAvailable,
                isSelected = uiState.selectedItem?.id == item.id,
                isUpdating = uiState.cardBeingUpdated == item.id,
                onClick = { onItemSelected(item) },
                onMenuButtonClick = { onMenuButtonClick(item) }
            )

            if (index != uiState.paymentDetailsList.lastIndex || uiState.canAddNewPaymentMethod) {
                LinkDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        if (uiState.canAddNewPaymentMethod) {
            AddPaymentMethodRow(
                isEnabled = isInteractionEnabled,
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
            color = LinkTheme.colors.textTertiary
        )
        Icon(
            painter = painterResource(id = R.drawable.stripe_link_chevron),
            contentDescription = stringResource(R.string.stripe_wallet_expand_accessibility),
            modifier = Modifier.rotate(CHEVRON_ICON_ROTATION),
            tint = LinkTheme.colors.iconTertiary,
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
        Text(
            text = stringResource(R.string.stripe_add_payment_method),
            modifier = Modifier.padding(horizontal = HorizontalPadding),
            color = LinkTheme.colors.textBrand,
            style = LinkTheme.typography.bodyEmphasized,
        )
    }
}

@Composable
private fun AddShippingAddressRow(
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(enabled = isEnabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Add shipping address",
            modifier = Modifier.padding(horizontal = HorizontalPadding),
            color = LinkTheme.colors.textBrand,
            style = LinkTheme.typography.bodyEmphasized,
        )
    }
}

@Composable
private fun LinkMandate(text: String) {
    Html(
        html = text.replaceHyperlinks(),
        color = LinkTheme.colors.textTertiary,
        style = LinkTheme.typography.caption.copy(
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        urlSpanStyle = SpanStyle(
            color = LinkTheme.colors.textBrand,
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
private fun AlertMessage(
    alertMessage: ResolvableString,
    onDismissAlert: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        modifier = Modifier
            .testTag(WALLET_SCREEN_DIALOG_TAG),
        text = {
            Text(
                text = alertMessage.resolve(),
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textPrimary,
            )
        },
        backgroundColor = LinkTheme.colors.surfacePrimary,
        onDismissRequest = onDismissAlert,
        confirmButton = {
            TextButton(
                modifier = Modifier
                    .testTag(WALLET_SCREEN_DIALOG_BUTTON_TAG),
                onClick = onDismissAlert
            ) {
                Text(
                    text = android.R.string.ok.resolvableString.resolve(context),
                    color = LinkTheme.colors.textBrand
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
internal const val WALLET_SCREEN_MENU_SHEET_TAG = "wallet_screen_menu_sheet_tag"
internal const val WALLET_SCREEN_DIALOG_TAG = "wallet_screen_dialog_tag"
internal const val WALLET_SCREEN_DIALOG_BUTTON_TAG = "wallet_screen_dialog_button_tag"
internal const val WALLET_SCREEN_ERROR_TAG = "wallet_screen_error_tag"

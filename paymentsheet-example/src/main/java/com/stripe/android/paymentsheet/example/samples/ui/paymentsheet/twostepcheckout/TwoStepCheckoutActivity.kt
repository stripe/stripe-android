@file:OptIn(WalletButtonsPreview::class)

package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.twostepcheckout

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentelement.ExtendedLabelsInPaymentOptionPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.ui.MAIN_FONT_SIZE
import com.stripe.android.paymentsheet.example.samples.ui.SUB_FONT_SIZE
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController

private const val GRAY_COLOR = 0xFFF5F5F5
private const val DARK_GRAY_COLOR = 0xFF1A1A1A

@OptIn(WalletButtonsPreview::class)
internal class TwoStepCheckoutActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(android.graphics.Color.BLACK)
            .setTextColor(android.graphics.Color.WHITE)
    }

    private val viewModel by viewModels<TwoStepCheckoutViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the toolbar/action bar color to black
        supportActionBar?.setBackgroundDrawable(
            android.graphics.Color.BLACK.toDrawable()
        )

        // Set status bar color to black and make text white for visibility
        window.statusBarColor = android.graphics.Color.BLACK
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false // Makes status bar text white

        setContent {
            val flowController = rememberPaymentSheetFlowController(
                paymentOptionCallback = viewModel::handlePaymentOptionChanged,
                paymentResultCallback = viewModel::handlePaymentSheetResult,
            )

            val addressLauncher = rememberAddressLauncher(
                callback = viewModel::handleAddressLauncherResult
            )

            TwoStepCheckoutTheme {
                val uiState by viewModel.state.collectAsState()

                uiState.paymentInfo?.let { paymentInfo ->
                    LaunchedEffect(paymentInfo, uiState.shippingAddress) {
                        configureFlowController(flowController, paymentInfo, uiState.shippingAddress)
                    }
                }

                // Update FlowController shipping details when address changes
                LaunchedEffect(uiState.shippingAddress) {
                    flowController.shippingDetails = uiState.shippingAddress
                }

                uiState.status?.let { status ->
                    if (uiState.didComplete) {
                        CompletedPaymentAlertDialog(
                            onDismiss = ::finish
                        )
                    } else {
                        LaunchedEffect(status) {
                            snackbar.setText(status).show()
                            viewModel.statusDisplayed()
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(
                            paddingValues = WindowInsets.systemBars.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            ).asPaddingValues()
                        ),
                ) {
                    when {
                        uiState.showConfiguration -> {
                            ConfigurationScreen(
                                uiState = uiState,
                                onEmailChanged = viewModel::updateCustomerEmail,
                                onCompleteConfiguration = viewModel::completeConfiguration
                            )
                        }
                        uiState.isError -> {
                            ErrorScreen(onRetry = viewModel::retry)
                        }
                        uiState.showLoading -> {
                            LoadingScreen()
                        }
                        uiState.showFinalCheckout -> {
                            FinalCheckoutScreen(
                                uiState = uiState,
                                flowController = flowController,
                                addressLauncher = addressLauncher,
                                onBuyClick = {
                                    viewModel.handleBuyButtonPressed()
                                    flowController.confirm()
                                }
                            )
                        }
                        else -> {
                            PaymentMethodSelectionScreen(
                                uiState = uiState,
                                flowController = flowController
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureFlowController(
        flowController: PaymentSheet.FlowController,
        paymentInfo: TwoStepCheckoutViewState.PaymentInfo,
        shippingAddress: AddressDetails?
    ) {
        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = paymentInfo.clientSecret,
            configuration = paymentInfo.paymentSheetConfig(shippingAddress),
            callback = viewModel::handleFlowControllerConfigured,
        )
        // Set FlowController shipping details
        flowController.shippingDetails = shippingAddress
    }
}

@Composable
private fun ConfigurationScreen(
    uiState: TwoStepCheckoutViewState,
    onEmailChanged: (String) -> Unit,
    onCompleteConfiguration: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Checkout Configuration",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        // Customer Email Section
        Text(
            text = "Customer Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = uiState.customerEmail,
            onValueChange = onEmailChanged,
            label = { Text("Customer Email (optional)") },
            placeholder = { Text("customer@example.com") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )

        // Feature flags section removed

        Spacer(modifier = Modifier.weight(1f))

        // Continue Button
        TextButton(
            onClick = onCompleteConfiguration,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
            )
        ) {
            Text(
                text = "Continue to Checkout",
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(WalletButtonsPreview::class)
@Composable
private fun PaymentMethodSelectionScreen(
    uiState: TwoStepCheckoutViewState,
    flowController: PaymentSheet.FlowController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.merchant_checkout_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        // Product Section
        ProductSection(
            cartState = uiState.cartState
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Total Section
        TotalSection(cartState = uiState.cartState)

        Spacer(modifier = Modifier.height(16.dp))

        // Wallet Buttons (Apple Pay/Google Pay/Link)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            flowController.WalletButtons()
        }

        // Other payments button (light gray)
        TextButton(
            enabled = uiState.isPaymentMethodButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            onClick = flowController::presentPaymentOptions,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = androidx.compose.ui.graphics.Color(GRAY_COLOR), // Light gray
                contentColor = androidx.compose.ui.graphics.Color.Black,
            )
        ) {
            Text(
                text = "Pay another way",
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.Black
            )
        }
    }
}

@OptIn(ExtendedLabelsInPaymentOptionPreview::class)
@Composable
private fun FinalCheckoutScreen(
    uiState: TwoStepCheckoutViewState,
    flowController: PaymentSheet.FlowController,
    addressLauncher: AddressLauncher,
    onBuyClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header (no back button)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Review and Pay",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // Product Section
        ProductSection(
            cartState = uiState.cartState
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Method Row
        PaymentMethodRow(
            paymentOption = uiState.paymentOption,
            onClick = flowController::presentPaymentOptions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Shipping Address Row
        ShippingAddressRow(
            shippingAddress = uiState.shippingAddress,
            onClick = {
                val publishableKey = com.stripe.android.PaymentConfiguration.getInstance(
                    context
                ).publishableKey
                addressLauncher.present(
                    publishableKey = publishableKey,
                    configuration = AddressLauncher.Configuration(
                        appearance = PaymentSheet.Appearance(
                            colorsLight = PaymentSheet.Colors(
                                primary = androidx.compose.ui.graphics.Color.Black.toArgb(),
                                surface = PaymentSheet.Colors.defaultLight.surface,
                                component = PaymentSheet.Colors.defaultLight.component,
                                componentBorder = PaymentSheet.Colors.defaultLight.componentBorder,
                                componentDivider = PaymentSheet.Colors.defaultLight.componentDivider,
                                onComponent = PaymentSheet.Colors.defaultLight.onComponent,
                                onSurface = PaymentSheet.Colors.defaultLight.onSurface,
                                subtitle = PaymentSheet.Colors.defaultLight.subtitle,
                                placeholderText = PaymentSheet.Colors.defaultLight.placeholderText,
                                appBarIcon = PaymentSheet.Colors.defaultLight.appBarIcon,
                                error = PaymentSheet.Colors.defaultLight.error
                            ),
                            colorsDark = PaymentSheet.Colors(
                                primary = androidx.compose.ui.graphics.Color.Black.toArgb(),
                                surface = PaymentSheet.Colors.defaultDark.surface,
                                component = PaymentSheet.Colors.defaultDark.component,
                                componentBorder = PaymentSheet.Colors.defaultDark.componentBorder,
                                componentDivider = PaymentSheet.Colors.defaultDark.componentDivider,
                                onComponent = PaymentSheet.Colors.defaultDark.onComponent,
                                onSurface = PaymentSheet.Colors.defaultDark.onSurface,
                                subtitle = PaymentSheet.Colors.defaultDark.subtitle,
                                placeholderText = PaymentSheet.Colors.defaultDark.placeholderText,
                                appBarIcon = PaymentSheet.Colors.defaultDark.appBarIcon,
                                error = PaymentSheet.Colors.defaultDark.error
                            )
                        ),
                        address = uiState.shippingAddress,
                        buttonTitle = "Save address",
                        title = "Shipping address",
                        additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                            phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED
                        )
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Shipping Method Row
        ShippingMethodRow()

        Spacer(modifier = Modifier.height(32.dp))

        // Total Section
        TotalSection(cartState = uiState.cartState)

        Spacer(modifier = Modifier.height(16.dp))

        // Buy Button
        BuyButton(
            buyButtonEnabled = uiState.isBuyButtonEnabled,
            onClick = onBuyClick
        )
    }
}

@Composable
private fun ProductSection(
    cartState: CartState
) {
    // For now, we'll use the first product from the cart
    val product = cartState.products.firstOrNull()

    if (product != null) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product image placeholder (emoji)
                Text(
                    text = product.icon,
                    fontSize = 48.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Classic Hot Dog",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "With mustard and relish",
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Quantity display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Quantity: ${product.quantity}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExtendedLabelsInPaymentOptionPreview::class)
@Composable
private fun PaymentMethodRow(
    paymentOption: PaymentOption?,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        // Payment method icon
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            paymentOption?.iconPainter?.let { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Inside
                )
            } ?: run {
                // Fallback icon if no payment method selected
                Icon(
                    painter = rememberVectorPainter(Icons.Default.Check),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = paymentOption?.labels?.label ?: "Payment method",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.KeyboardArrowRight),
            contentDescription = "Change payment method",
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }

    // Payment method subtitle
    val sublabel = paymentOption?.labels?.sublabel

    sublabel?.let { subtitle ->
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 36.dp) // 24dp icon + 12dp spacer
        )
    }
}

@Composable
private fun ShippingAddressRow(
    shippingAddress: AddressDetails?,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Default.LocationOn),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (shippingAddress != null) {
                shippingAddress.name?.let { name ->
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )
                }

                shippingAddress.address?.let { address ->
                    Text(
                        text = buildString {
                            append(address.line1 ?: "")
                            append(" ")
                            append(address.city ?: "")
                            append(" ")
                            append(address.state ?: "")
                            append(" ")
                            append(address.postalCode ?: "")
                        }.trim(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    text = "Add shipping address",
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.primary
                )
            }
        }

        Icon(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.KeyboardArrowRight),
            contentDescription = "Change shipping address",
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ShippingMethodRow() {
    var selectedMethod by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    val shippingMethods = listOf(
        ShippingMethod("USPS Ground Advantage", "Free standard shipping", "$0.00", true),
        ShippingMethod("USPS Priority Mail", "2-3 business days", "$8.99", false),
        ShippingMethod("UPS Next Day", "1 business day", "$24.99", false)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Default.LocalShipping),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = shippingMethods[selectedMethod].name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = shippingMethods[selectedMethod].description,
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Icon(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.KeyboardArrowRight),
            contentDescription = "Change shipping method",
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }

    // Dialog for selecting shipping method
    if (showDialog) {
        ShippingMethodDialog(
            shippingMethods = shippingMethods,
            selectedMethod = selectedMethod,
            onMethodSelected = { selectedMethod = it },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TotalSection(cartState: CartState) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Pay Hot Dog Stand",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total with estimated tax",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = cartState.formattedTotal,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
private fun ShippingMethodOption(
    method: ShippingMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colors.primary
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = method.name,
                fontSize = MAIN_FONT_SIZE,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = method.description,
                fontSize = SUB_FONT_SIZE,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Text(
            text = if (method.isFree) "Free" else method.price,
            fontSize = MAIN_FONT_SIZE,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun ShippingMethodDialog(
    shippingMethods: List<ShippingMethod>,
    selectedMethod: Int,
    onMethodSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select shipping method",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                shippingMethods.forEachIndexed { index, method ->
                    ShippingMethodOption(
                        method = method,
                        isSelected = selectedMethod == index,
                        onClick = {
                            onMethodSelected(index)
                            onDismiss()
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.primary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

private data class ShippingMethod(
    val name: String,
    val description: String,
    val price: String,
    val isFree: Boolean
)

@Composable
private fun TwoStepCheckoutTheme(
    content: @Composable () -> Unit
) {
    val blackPrimaryColors = if (isSystemInDarkTheme()) {
        darkColors(
            primary = androidx.compose.ui.graphics.Color.Black,
            primaryVariant = androidx.compose.ui.graphics.Color(DARK_GRAY_COLOR),
            secondary = androidx.compose.ui.graphics.Color.Black
        )
    } else {
        lightColors(
            primary = androidx.compose.ui.graphics.Color.Black,
            primaryVariant = androidx.compose.ui.graphics.Color(DARK_GRAY_COLOR),
            secondary = androidx.compose.ui.graphics.Color.Black
        )
    }

    MaterialTheme(
        colors = blackPrimaryColors,
        content = content
    )
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(20.dp)
                    .width(64.dp),
                color = MaterialTheme.colors.primary
            )
        }
    }
}

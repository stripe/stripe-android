@file:OptIn(WalletButtonsPreview::class)

package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.merchant_checkout

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentelement.ExtendedLabelsInPaymentOptionPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.MAIN_FONT_SIZE
import com.stripe.android.paymentsheet.example.samples.ui.SUB_FONT_SIZE
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.model.PaymentOption
import androidx.core.graphics.drawable.toDrawable

@OptIn(WalletButtonsPreview::class)
internal class MerchantCheckoutActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(android.graphics.Color.BLACK)
            .setTextColor(android.graphics.Color.WHITE)
    }

    private val viewModel by viewModels<MerchantCheckoutViewModel>()

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

            MerchantCheckoutTheme {
                val uiState by viewModel.state.collectAsState()
                val paymentMethodLabel = determinePaymentMethodLabel(uiState)

                uiState.paymentInfo?.let { paymentInfo ->
                    LaunchedEffect(paymentInfo) {
                        configureFlowController(flowController, paymentInfo)
                    }
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

                Box(
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
                                onMerchantNameChanged = viewModel::updateMerchantName,
                                onInlineOtpToggled = viewModel::updateInlineOtpEnabled,
                                onCompleteConfiguration = viewModel::completeConfiguration
                            )
                        }
                        uiState.isError -> {
                            ErrorScreen(onRetry = viewModel::retry)
                        }
                        uiState.isProcessing && uiState.paymentInfo == null -> {
                            LoadingScreen()
                        }
                        uiState.showFinalCheckout -> {
                            FinalCheckoutScreen(
                                uiState = uiState,
                                flowController = flowController,
                                addressLauncher = addressLauncher,
                                onBackClick = viewModel::goBackToPaymentMethods,
                                onBuyClick = {
                                    viewModel.handleBuyButtonPressed()
                                    flowController.confirm()
                                }
                            )
                        }
                        else -> {
                            PaymentMethodSelectionScreen(
                                uiState = uiState,
                                flowController = flowController,
                                paymentMethodLabel = paymentMethodLabel,
                                onSettingsClick = viewModel::goBackToConfiguration
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureFlowController(
        flowController: PaymentSheet.FlowController,
        paymentInfo: MerchantCheckoutViewState.PaymentInfo,
    ) {
        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = paymentInfo.clientSecret,
            configuration = paymentInfo.paymentSheetConfig,
            callback = viewModel::handleFlowControllerConfigured,
        )
    }
}

@Composable
private fun ConfigurationScreen(
    uiState: MerchantCheckoutViewState,
    onEmailChanged: (String) -> Unit,
    onMerchantNameChanged: (String) -> Unit,
    onInlineOtpToggled: (Boolean) -> Unit,
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
        
        // Merchant Settings Section
        Text(
            text = "Merchant Settings", 
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = uiState.merchantName,
            onValueChange = onMerchantNameChanged,
            label = { Text("Merchant Display Name") },
            placeholder = { Text("Your Store Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )
        
        // Feature Flags Section
        Text(
            text = "Feature Flags",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Inline OTP Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Enable Inline OTP",
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = "Show inline OTP in wallet buttons",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = uiState.enableInlineOtp,
                onCheckedChange = onInlineOtpToggled
            )
        }
        
        // Info text
        Text(
            text = "Configure your checkout settings. If you provide a customer email, it will be used to create a customer record for saved payment methods.",
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )
        
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
    uiState: MerchantCheckoutViewState,
    flowController: PaymentSheet.FlowController,
    paymentMethodLabel: String,
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with settings button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Merchant Checkout",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = rememberVectorPainter(Icons.Default.Settings),
                    contentDescription = "Settings",
                    tint = MaterialTheme.colors.onSurface
                )
            }
        }
        
        // Product Section
        ProductSection(
            cartState = uiState.cartState,
            isLoading = uiState.isProcessing
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Total Section
        TotalSection(cartState = uiState.cartState)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Wallet Buttons (Apple Pay/Google Pay/Link)
        if (uiState.paymentInfo != null && !uiState.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                flowController.WalletButtons()
            }
        }
        
        // Other payments button (light gray)
        TextButton(
            enabled = uiState.isPaymentMethodButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            onClick = flowController::presentPaymentOptions,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5), // Light gray
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
    uiState: MerchantCheckoutViewState,
    flowController: PaymentSheet.FlowController,
    addressLauncher: AddressLauncher,
    onBackClick: () -> Unit,
    onBuyClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Review and Pay",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Product Section
        ProductSection(
            cartState = uiState.cartState,
            isLoading = uiState.isProcessing
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Payment Method Row
        PaymentMethodRow(
            paymentOption = uiState.paymentOption,
            isLinkSelected = uiState.isLinkSelected,
            onClick = flowController::presentPaymentOptions
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
                        address = uiState.shippingAddress,
                        buttonTitle = "Save address",
                        title = "Shipping address"
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Shipping Method Row
        ShippingMethodRow()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Total Section
        TotalSection(cartState = uiState.cartState)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Buy Button
        BuyButton(
            buyButtonEnabled = uiState.isBuyButtonEnabled,
            onClick = onBuyClick
        )
    }
}

@Composable
private fun determinePaymentMethodLabel(uiState: MerchantCheckoutViewState): String {
    val context = LocalContext.current
    return remember(uiState) {
        if (uiState.paymentOption?.label != null) {
            uiState.paymentOption.label
        } else if (!uiState.isProcessing) {
            context.getString(R.string.select)
        } else {
            context.getString(R.string.loading)
        }
    }
}

@Composable
private fun ProductSection(
    cartState: CartState,
    isLoading: Boolean
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
                        text = "Men's Wool Runner Mizzles",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Black • Size 13",
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Quantity selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { /* TODO: Implement quantity decrease */ },
                    enabled = !isLoading
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Default.Remove),
                        contentDescription = "Decrease quantity"
                    )
                }
                
                Text(
                    text = product.quantity.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                IconButton(
                    onClick = { /* TODO: Implement quantity increase */ },
                    enabled = !isLoading
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Default.Add),
                        contentDescription = "Increase quantity"
                    )
                }
            }
        }
    }
}

@OptIn(ExtendedLabelsInPaymentOptionPreview::class)
@Composable
private fun PaymentMethodRow(
    paymentOption: PaymentOption?,
    isLinkSelected: Boolean,
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
        paymentOption?.iconPainter?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
            )
        } ?: run {
            // Fallback icon if no payment method selected
            Icon(
                painter = rememberVectorPainter(Icons.Default.Check),
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = when {
                isLinkSelected -> "Link"
                paymentOption?.labels?.label != null -> paymentOption.labels.label
                else -> "Link"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Icon(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.KeyboardArrowRight),
            contentDescription = "Change payment method",
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
    }
    
    // Payment method subtitle
    val sublabel = when {
        isLinkSelected -> paymentOption?.labels?.sublabel
        paymentOption?.labels?.sublabel != null -> paymentOption.labels.sublabel
        else -> null
    }
    
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
            modifier = Modifier.padding(end = 12.dp)
        )
        
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
                        text = "${address.line1 ?: ""} ${address.city ?: ""} ${address.state ?: ""} ${address.postalCode ?: ""}".trim(),
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
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
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
            modifier = Modifier.padding(end = 12.dp)
        )
        
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
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
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
private fun MerchantCheckoutTheme(
    content: @Composable () -> Unit
) {
    val blackPrimaryColors = if (isSystemInDarkTheme()) {
        darkColors(
            primary = androidx.compose.ui.graphics.Color.Black,
            primaryVariant = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
            secondary = androidx.compose.ui.graphics.Color.Black
        )
    } else {
        lightColors(
            primary = androidx.compose.ui.graphics.Color.Black,
            primaryVariant = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
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
            androidx.compose.material.CircularProgressIndicator(
                modifier = Modifier
                    .padding(20.dp)
                    .width(64.dp),
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "Setting up checkout...",
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
} 
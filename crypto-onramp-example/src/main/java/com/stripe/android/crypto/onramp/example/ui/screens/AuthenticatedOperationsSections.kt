package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.crypto.onramp.example.CHECKOUT_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.COLLECT_CARD_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.CREATE_CRYPTO_TOKEN_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.CREATE_SESSION_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.GET_WALLET_OWNERSHIP_CHALLENGE_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.REGISTER_WALLET_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.SUBMIT_DETERMINISTIC_WALLET_OWNERSHIP_SIGNATURE_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.SUBMIT_WALLET_OWNERSHIP_SIGNATURE_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.network.OnrampSessionResponse
import com.stripe.android.crypto.onramp.example.network.SettlementSpeed
import com.stripe.android.crypto.onramp.example.ui.components.GooglePayButton
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.PaymentMethodDisplayData
import com.stripe.android.crypto.onramp.model.PaymentMethodSelection

@Composable
internal fun OperationsHeader(
    email: String,
    consentedLinkAuthIntentIds: List<String>
) {
    Text(
        text = "Authenticated Operations",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Text(
        text = "Email: $email",
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Text(
        text = "Consented LAIs:\n${consentedLinkAuthIntentIds.joinToString("\n")}",
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
internal fun SessionSummary(
    onrampSessionResponse: OnrampSessionResponse?
) {
    onrampSessionResponse?.let { response ->
        Text(
            text = "Onramp Session ID: ${response.id}",
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "Session Status: ${response.status}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Total Amount: ${response.sourceTotalAmount}",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Payment Method: ${response.paymentMethod}",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = buildString {
                append("Exchange Amount: ${response.transactionDetails.sourceExchangeAmount}")
                append(" ${response.transactionDetails.sourceCurrency}")
                append(" -> ${response.transactionDetails.destinationExchangeAmount}")
                append(" ${response.transactionDetails.destinationCurrency}")
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Network Fee: ${response.transactionDetails.fees.networkFeeAmount}",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Transaction Fee: ${response.transactionDetails.fees.transactionFeeAmount}",
            modifier = Modifier.padding(bottom = 16.dp)
        )
    } ?: Spacer(modifier = Modifier.height(16.dp))
}

@Composable
internal fun SelectedPaymentSummary(
    selectedPaymentData: PaymentMethodDisplayData?,
    selectedSettlementSpeed: SettlementSpeed,
    onSelectSettlementSpeed: (SettlementSpeed) -> Unit
) {
    val paymentData = selectedPaymentData ?: return

    if (paymentData.type == PaymentMethodDisplayData.Type.BankAccount) {
        Text(
            text = "Settlement Speed",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            SettlementSpeed.entries.forEach { speed ->
                val isSelected = selectedSettlementSpeed == speed
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) {
                                MaterialTheme.colors.primary
                            } else {
                                Color.LightGray
                            }
                        )
                        .clickable { onSelectSettlementSpeed(speed) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = speed.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }

    Image(
        painter = paymentData.iconPainter,
        contentDescription = paymentData.label,
        modifier = Modifier
            .height(24.dp)
            .padding(end = 8.dp)
    )

    Text(
        text = "Selected Payment Type: ${paymentData.label}",
        modifier = Modifier.padding(bottom = 24.dp)
    )

    Text(
        text = "Selected Payment Value: ${paymentData.sublabel}",
        modifier = Modifier.padding(bottom = 24.dp)
    )
}

@Composable
internal fun WalletAddressSection(
    walletAddress: String,
    onWalletAddressChange: (String) -> Unit,
    selectedNetwork: CryptoNetwork,
    isDropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    onSelectNetwork: (CryptoNetwork) -> Unit,
    onRegisterWalletAddress: () -> Unit
) {
    Text(
        text = "Register Wallet Address",
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Box {
        OutlinedTextField(
            value = selectedNetwork.value.replaceFirstChar { it.uppercase() },
            onValueChange = { },
            readOnly = true,
            label = { Text("Network") },
            trailingIcon = {
                TextButton(onClick = { onDropdownExpandedChange(true) }) {
                    Text("▼")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { onDropdownExpandedChange(false) }
        ) {
            CryptoNetwork.entries.forEach { network ->
                DropdownMenuItem(
                    onClick = {
                        onSelectNetwork(network)
                        onDropdownExpandedChange(false)
                    }
                ) {
                    Text(network.value.replaceFirstChar { it.uppercase() })
                }
            }
        }
    }

    OutlinedTextField(
        value = walletAddress,
        onValueChange = onWalletAddressChange,
        label = { Text("Wallet Address") },
        placeholder = { Text("0x1234567890abcdef...") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )

    Button(
        onClick = onRegisterWalletAddress,
        modifier = Modifier
            .testTag(REGISTER_WALLET_BUTTON_TAG)
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text("Register Wallet Address")
    }
}

@Composable
@Suppress("LongMethod")
internal fun WalletOwnershipSection(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    challengeId: String?,
    challengeMessage: String?,
    challengeExpiresAt: String?,
    verifiedOwnership: Boolean?,
    signatureInput: String,
    onSignatureInputChange: (String) -> Unit,
    onGetWalletOwnershipChallenge: () -> Unit,
    onSubmitWalletOwnershipSignature: () -> Unit,
    onSubmitDeterministicWalletOwnershipSignature: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!isExpanded) }
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Wallet Ownership",
            fontWeight = FontWeight.Bold
        )
        Text(text = if (isExpanded) "Hide" else "Show")
    }

    AnimatedVisibility(visible = isExpanded) {
        Column {
            Button(
                onClick = onGetWalletOwnershipChallenge,
                modifier = Modifier
                    .testTag(GET_WALLET_OWNERSHIP_CHALLENGE_BUTTON_TAG)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Get Wallet Ownership Challenge")
            }

            challengeId?.let { id ->
                Text(
                    text = "Challenge ID: $id",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            challengeExpiresAt?.let { expiresAt ->
                Text(
                    text = "Challenge expires at: $expiresAt",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            challengeMessage?.let { message ->
                Text(
                    text = "Challenge message:\n$message",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            verifiedOwnership?.let { verified ->
                Text(
                    text = "Verified ownership: $verified",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = signatureInput,
                onValueChange = onSignatureInputChange,
                label = { Text("Wallet Ownership Signature") },
                placeholder = { Text("Signature or deterministic test signature") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Button(
                onClick = onSubmitDeterministicWalletOwnershipSignature,
                enabled = challengeId != null,
                modifier = Modifier
                    .testTag(SUBMIT_DETERMINISTIC_WALLET_OWNERSHIP_SIGNATURE_BUTTON_TAG)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Submit Deterministic Test Signature")
            }

            Button(
                onClick = onSubmitWalletOwnershipSignature,
                enabled = challengeId != null,
                modifier = Modifier
                    .testTag(SUBMIT_WALLET_OWNERSHIP_SIGNATURE_BUTTON_TAG)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text("Submit Wallet Ownership Signature")
            }
        }
    }
}

@Composable
internal fun VerificationSection(
    onStartVerification: () -> Unit,
    onShowCrsCarfDeclaration: () -> Unit
) {
    Text(
        text = "Verification",
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Button(
        onClick = onStartVerification,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text("Start Identity Verification")
    }

    Button(
        onClick = onShowCrsCarfDeclaration,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text("CRS CARF Declaration")
    }
}

@Composable
internal fun PaymentSection(
    googlePayIsReady: Boolean,
    onCollectPayment: (PaymentMethodSelection) -> Unit
) {
    Text(
        text = "Payment",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Button(
        onClick = { onCollectPayment(PaymentMethodSelection.Card()) },
        modifier = Modifier
            .testTag(COLLECT_CARD_BUTTON_TAG)
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("Collect Card")
    }

    Button(
        onClick = { onCollectPayment(PaymentMethodSelection.BankAccount()) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("Collect Bank Account")
    }

    Button(
        onClick = { onCollectPayment(PaymentMethodSelection.CardAndBankAccount()) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("Collect Card or Bank Account")
    }

    GooglePayButton(
        enabled = googlePayIsReady,
        onClick = {
            onCollectPayment(
                PaymentMethodSelection.GooglePay(
                    currencyCode = "USD",
                    amount = 0L
                )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
internal fun CheckoutSection(
    hasSession: Boolean,
    onCreatePaymentToken: () -> Unit,
    onCreateSession: () -> Unit,
    onPerformCheckout: () -> Unit
) {
    Button(
        onClick = onCreatePaymentToken,
        modifier = Modifier
            .testTag(CREATE_CRYPTO_TOKEN_BUTTON_TAG)
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("Create Crypto Payment Token")
    }

    Button(
        onClick = onCreateSession,
        modifier = Modifier
            .testTag(CREATE_SESSION_BUTTON_TAG)
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text("Create Session")
    }

    Button(
        onClick = onPerformCheckout,
        enabled = hasSession,
        modifier = Modifier
            .testTag(CHECKOUT_BUTTON_TAG)
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(if (hasSession) "Checkout" else "Checkout (Create session first)")
    }
}

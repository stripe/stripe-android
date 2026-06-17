package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.crypto.onramp.example.model.OnrampUiState
import com.stripe.android.crypto.onramp.example.network.SettlementSpeed
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.PaymentMethodSelection
import com.stripe.android.paymentsheet.PaymentSheet

@Composable
@Suppress("LongMethod")
internal fun AuthenticatedOperationsScreen(
    uiState: OnrampUiState,
    onAuthenticate: (String) -> Unit,
    onRegisterWalletAddress: (String, CryptoNetwork) -> Unit,
    onGetWalletOwnershipChallenge: (String, CryptoNetwork) -> Unit,
    onSubmitWalletOwnershipSignature: (String) -> Unit,
    onSubmitDeterministicWalletOwnershipSignature: () -> Unit,
    onWalletOwnershipSignatureChange: (String) -> Unit,
    onCollectKyc: (KycInfo) -> Unit,
    onVerifyKyc: () -> Unit,
    onStartVerification: () -> Unit,
    onShowCrsCarfDeclaration: () -> Unit,
    onCollectPayment: (PaymentMethodSelection) -> Unit,
    onCreatePaymentToken: () -> Unit,
    onCreateSession: () -> Unit,
    onPerformCheckout: () -> Unit,
    onLogOut: () -> Unit,
    onBack: () -> Unit,
    onSelectSettlementSpeed: (SettlementSpeed) -> Unit,
    onKycFirstNameChange: (String) -> Unit,
    onKycLastNameChange: (String) -> Unit,
    onKycBirthCountryChange: (String) -> Unit,
    onKycBirthCityChange: (String) -> Unit,
    onKycNationalitiesChange: (String) -> Unit,
    onKycAddressChange: (PaymentSheet.Address) -> Unit,
    onIdentifierTypeChange: (Int, String) -> Unit,
    onIdentifierValueChange: (Int, String) -> Unit,
    onAddIdentifier: () -> Unit,
    onRemoveIdentifier: (Int) -> Unit,
    onRetrieveMissingIdentifiers: () -> Unit,
    onSubmitIdentifiers: () -> Unit,
) {
    var walletAddressInput by remember {
        mutableStateOf(uiState.walletAddress ?: DEFAULT_WALLET_ADDRESS)
    }
    var selectedNetwork by remember {
        mutableStateOf(uiState.network ?: CryptoNetwork.Ethereum)
    }
    var isNetworkDropdownExpanded by remember { mutableStateOf(false) }
    var isWalletOwnershipExpanded by remember { mutableStateOf(false) }
    var isKycExpanded by remember { mutableStateOf(false) }
    var isIdentifierExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.walletAddress) {
        if (!uiState.walletAddress.isNullOrBlank()) {
            walletAddressInput = uiState.walletAddress
        }
    }

    LaunchedEffect(uiState.network) {
        uiState.network?.let { selectedNetwork = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OperationsHeader(
            email = uiState.email,
            consentedLinkAuthIntentIds = uiState.consentedLinkAuthIntentIds
        )

        SessionSummary(onrampSessionResponse = uiState.onrampSession)
        SelectedPaymentSummary(
            selectedPaymentData = uiState.selectedPaymentData,
            selectedSettlementSpeed = uiState.settlementSpeed,
            onSelectSettlementSpeed = onSelectSettlementSpeed
        )

        Text(
            text = "Request scopes",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        AuthenticateSection(onAuthenticate = onAuthenticate)

        WalletAddressSection(
            walletAddress = walletAddressInput,
            onWalletAddressChange = { walletAddressInput = it },
            selectedNetwork = selectedNetwork,
            isDropdownExpanded = isNetworkDropdownExpanded,
            onDropdownExpandedChange = { isNetworkDropdownExpanded = it },
            onSelectNetwork = { selectedNetwork = it },
            onRegisterWalletAddress = {
                onRegisterWalletAddress(walletAddressInput, selectedNetwork)
            }
        )

        WalletOwnershipSection(
            isExpanded = isWalletOwnershipExpanded,
            onExpandedChange = { isWalletOwnershipExpanded = it },
            challengeId = uiState.walletOwnershipChallengeId,
            challengeMessage = uiState.walletOwnershipChallengeMessage,
            challengeExpiresAt = uiState.walletOwnershipChallengeExpiresAt,
            verifiedOwnership = uiState.walletOwnershipVerified,
            signatureInput = uiState.walletOwnershipSignatureInput,
            onSignatureInputChange = onWalletOwnershipSignatureChange,
            onGetWalletOwnershipChallenge = {
                onGetWalletOwnershipChallenge(walletAddressInput, selectedNetwork)
            },
            onSubmitWalletOwnershipSignature = {
                onSubmitWalletOwnershipSignature(uiState.walletOwnershipSignatureInput)
            },
            onSubmitDeterministicWalletOwnershipSignature = onSubmitDeterministicWalletOwnershipSignature
        )

        KycSection(
            isExpanded = isKycExpanded,
            onExpandedChange = { isKycExpanded = it },
            firstName = uiState.kycFirstName,
            onFirstNameChange = onKycFirstNameChange,
            lastName = uiState.kycLastName,
            onLastNameChange = onKycLastNameChange,
            birthCountry = uiState.kycBirthCountry,
            onBirthCountryChange = onKycBirthCountryChange,
            birthCity = uiState.kycBirthCity,
            onBirthCityChange = onKycBirthCityChange,
            nationalities = uiState.kycNationalities,
            onNationalitiesChange = onKycNationalitiesChange,
            address = uiState.kycAddress,
            onAddressChange = onKycAddressChange,
            onCollectKyc = onCollectKyc,
            onVerifyKyc = onVerifyKyc
        )

        IdentifierSection(
            isExpanded = isIdentifierExpanded,
            onExpandedChange = { isIdentifierExpanded = it },
            identifierInputs = uiState.identifierInputs,
            onIdentifierTypeChange = onIdentifierTypeChange,
            onIdentifierValueChange = onIdentifierValueChange,
            onAddIdentifier = onAddIdentifier,
            onRemoveIdentifier = onRemoveIdentifier,
            missingIdentifiersSummary = uiState.missingIdentifiersSummary,
            submitIdentifiersSummary = uiState.submitIdentifiersSummary,
            onRetrieveMissingIdentifiers = onRetrieveMissingIdentifiers,
            onSubmitIdentifiers = onSubmitIdentifiers
        )

        VerificationSection(
            onStartVerification = onStartVerification,
            onShowCrsCarfDeclaration = onShowCrsCarfDeclaration
        )
        PaymentSection(
            googlePayIsReady = uiState.googlePayIsReady,
            onCollectPayment = onCollectPayment
        )
        CheckoutSection(
            hasSession = uiState.onrampSession != null,
            onCreatePaymentToken = onCreatePaymentToken,
            onCreateSession = onCreateSession,
            onPerformCheckout = onPerformCheckout
        )

        Button(
            onClick = onLogOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Log Out")
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Sign in")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private const val DEFAULT_WALLET_ADDRESS = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"

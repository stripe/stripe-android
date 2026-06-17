package com.stripe.android.crypto.onramp.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stripe.android.crypto.onramp.example.OnrampViewModel
import com.stripe.android.crypto.onramp.example.model.Screen
import com.stripe.android.crypto.onramp.example.ui.components.LoadingScreen
import com.stripe.android.crypto.onramp.example.ui.screens.AuthenticatedOperationsScreen
import com.stripe.android.crypto.onramp.example.ui.screens.AuthenticationScreen
import com.stripe.android.crypto.onramp.example.ui.screens.LoginSignupScreen
import com.stripe.android.crypto.onramp.example.ui.screens.RegistrationScreen
import com.stripe.android.crypto.onramp.example.ui.screens.SeamlessSignInScreen
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.PaymentMethodSelection

@Composable
@Suppress("LongMethod")
internal fun OnrampScreen(
    viewModel: OnrampViewModel,
    modifier: Modifier = Modifier,
    onAuthenticateUser: (String) -> Unit,
    onRegisterWalletAddress: (String, CryptoNetwork) -> Unit,
    onStartVerification: () -> Unit,
    onShowCrsCarfDeclaration: () -> Unit,
    onCollectPayment: (PaymentMethodSelection) -> Unit,
    onCreatePaymentToken: () -> Unit,
    onVerifyKyc: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.screen != Screen.LoginSignup) {
        viewModel.onBackToLoginSignup()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (uiState.screen) {
            Screen.SeamlessSignIn -> {
                SeamlessSignInScreen(
                    email = uiState.email,
                    onContinue = viewModel::seamlessSignInContinue,
                    onNotMe = viewModel::logOut
                )
            }
            Screen.LoginSignup -> {
                LoginSignupScreen(
                    onRegister = viewModel::registerUser,
                    onLogin = viewModel::loginUser
                )
            }
            Screen.Loading -> {
                LoadingScreen(message = uiState.loadingMessage ?: "Loading...")
            }
            Screen.Registration -> {
                RegistrationScreen(
                    initialEmail = uiState.email,
                    onRegister = { email, phone, country, fullName ->
                        viewModel.registerNewLinkUser(
                            LinkUserInfo(
                                email = email.trim(),
                                fullName = fullName?.trim()?.takeIf(String::isNotEmpty),
                                phone = phone.trim(),
                                country = country.trim()
                            )
                        )
                    },
                    onBack = viewModel::onBackToLoginSignup
                )
            }
            Screen.Authentication -> {
                AuthenticationScreen(
                    email = uiState.email,
                    onAuthenticate = onAuthenticateUser,
                    onUpdatePhoneNumber = viewModel::updatePhoneNumber,
                    onBack = viewModel::onBackToLoginSignup
                )
            }
            Screen.AuthenticatedOperations -> {
                AuthenticatedOperationsScreen(
                    uiState = uiState,
                    onAuthenticate = onAuthenticateUser,
                    onRegisterWalletAddress = onRegisterWalletAddress,
                    onGetWalletOwnershipChallenge = viewModel::getWalletOwnershipChallenge,
                    onSubmitWalletOwnershipSignature = viewModel::submitWalletOwnershipSignature,
                    onSubmitDeterministicWalletOwnershipSignature =
                        viewModel::submitDeterministicWalletOwnershipSignature,
                    onWalletOwnershipSignatureChange = viewModel::updateWalletOwnershipSignatureInput,
                    onCollectKyc = viewModel::collectKycInfo,
                    onVerifyKyc = onVerifyKyc,
                    onStartVerification = onStartVerification,
                    onShowCrsCarfDeclaration = onShowCrsCarfDeclaration,
                    onCollectPayment = onCollectPayment,
                    onCreatePaymentToken = onCreatePaymentToken,
                    onCreateSession = viewModel::createSession,
                    onPerformCheckout = viewModel::performCheckout,
                    onLogOut = viewModel::logOut,
                    onBack = viewModel::onBackToLoginSignup,
                    onSelectSettlementSpeed = viewModel::updateSettlementSpeed,
                    onKycFirstNameChange = viewModel::updateKycFirstName,
                    onKycLastNameChange = viewModel::updateKycLastName,
                    onKycBirthCountryChange = viewModel::updateKycBirthCountry,
                    onKycBirthCityChange = viewModel::updateKycBirthCity,
                    onKycNationalitiesChange = viewModel::updateKycNationalities,
                    onKycAddressChange = viewModel::updateKycAddress,
                    onIdentifierTypeChange = viewModel::updateIdentifierType,
                    onIdentifierValueChange = viewModel::updateIdentifierValue,
                    onAddIdentifier = viewModel::addIdentifierInput,
                    onRemoveIdentifier = viewModel::removeIdentifierInput,
                    onRetrieveMissingIdentifiers = viewModel::retrieveMissingIdentifiers,
                    onSubmitIdentifiers = viewModel::submitIdentifiers
                )
            }
        }
    }
}

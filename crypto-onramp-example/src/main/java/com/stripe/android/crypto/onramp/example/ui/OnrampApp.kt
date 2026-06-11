package com.stripe.android.crypto.onramp.example.ui

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stripe.android.crypto.onramp.example.INITIALIZATION_FAILURE_ALERT_MESSAGE_TAG
import com.stripe.android.crypto.onramp.example.INITIALIZATION_FAILURE_ALERT_TAG
import com.stripe.android.crypto.onramp.example.INITIALIZATION_FAILURE_ALERT_TITLE_TAG
import com.stripe.android.crypto.onramp.example.OnrampViewModel
import com.stripe.android.crypto.onramp.example.SNACKBAR_TAG
import com.stripe.android.crypto.onramp.example.SNACKBAR_TEXT_TAG
import com.stripe.android.crypto.onramp.example.ui.components.AddressForm
import com.stripe.android.crypto.onramp.example.ui.theme.OnrampExampleTheme
import com.stripe.android.crypto.onramp.model.PaymentMethodSelection
import com.stripe.android.paymentsheet.PaymentSheet

@Composable
@Suppress("LongMethod")
internal fun OnrampApp(
    viewModel: OnrampViewModel,
    onAuthenticateUser: (String) -> Unit,
    onCollectPayment: (PaymentMethodSelection) -> Unit,
    onStartVerification: () -> Unit,
    onShowCrsCarfDeclaration: () -> Unit,
    onSubmitAddress: (PaymentSheet.Address) -> Unit,
    onVerifyKyc: () -> Unit,
) {
    val showAddressModal by viewModel.updateAddressEvent.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val initializationFailureAlert by viewModel.initializationFailureAlert
        .collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )

    LaunchedEffect(message) {
        message?.let { currentMessage ->
            snackbarHostState.showSnackbar(
                message = currentMessage,
                duration = SnackbarDuration.Short
            )
            Log.d(LOG_TAG, currentMessage)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(showAddressModal) {
        if (showAddressModal) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    OnrampExampleTheme {
        initializationFailureAlert?.let { alert ->
            AlertDialog(
                modifier = Modifier.testTag(INITIALIZATION_FAILURE_ALERT_TAG),
                onDismissRequest = viewModel::clearInitializationFailureAlert,
                title = {
                    Text(
                        text = alert.title,
                        modifier = Modifier.testTag(INITIALIZATION_FAILURE_ALERT_TITLE_TAG)
                    )
                },
                text = {
                    Text(
                        text = alert.message,
                        modifier = Modifier.testTag(INITIALIZATION_FAILURE_ALERT_MESSAGE_TAG)
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::clearInitializationFailureAlert) {
                        Text("OK")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    windowInsets = AppBarDefaults.topAppBarWindowInsets,
                    title = { Text("Onramp Coordinator") }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.testTag("OnrampSnackbarHost"),
                    snackbar = { data ->
                        Snackbar(modifier = Modifier.testTag(SNACKBAR_TAG)) {
                            Text(
                                data.message,
                                modifier = Modifier.testTag(SNACKBAR_TEXT_TAG)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            ModalBottomSheetLayout(
                sheetState = sheetState,
                sheetGesturesEnabled = false,
                sheetContent = {
                    AddressForm(
                        onSubmit = { updatedAddress ->
                            viewModel.clearUpdateAddressEvent()
                            onSubmitAddress(updatedAddress)
                        },
                        onDismiss = viewModel::clearUpdateAddressEvent
                    )
                }
            ) {
                OnrampScreen(
                    modifier = Modifier.padding(innerPadding),
                    viewModel = viewModel,
                    onAuthenticateUser = onAuthenticateUser,
                    onRegisterWalletAddress = viewModel::registerWalletAddress,
                    onStartVerification = onStartVerification,
                    onShowCrsCarfDeclaration = onShowCrsCarfDeclaration,
                    onCollectPayment = onCollectPayment,
                    onCreatePaymentToken = viewModel::createCryptoPaymentToken,
                    onVerifyKyc = onVerifyKyc
                )
            }
        }
    }
}

private const val LOG_TAG = "OnrampExample"

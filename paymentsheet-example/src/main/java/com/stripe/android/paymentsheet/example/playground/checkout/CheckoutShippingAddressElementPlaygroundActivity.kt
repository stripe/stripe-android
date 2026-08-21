@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme

internal class CheckoutShippingAddressElementPlaygroundActivity : AppCompatActivity() {

    private val viewModel: CheckoutControllerExampleViewModel by viewModels {
        CheckoutControllerExampleViewModel.factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = viewModel.controller.createPresenter(this)
        val paymentElement = presenter.paymentElement()
        val shippingAddressElement = presenter.shippingAddressElement()

        setContent {
            CheckoutShippingAddressElementPlayground(
                viewModel = viewModel,
                onAddShippingAddress = shippingAddressElement::present,
                onSelectPaymentMethod = paymentElement::present,
                onConfirm = {
                    viewModel.clearConfirmationResult()
                    presenter.confirm()
                },
            )
        }
    }
}

@Composable
private fun CheckoutShippingAddressElementPlayground(
    viewModel: CheckoutControllerExampleViewModel,
    onAddShippingAddress: () -> Unit,
    onSelectPaymentMethod: () -> Unit,
    onConfirm: () -> Unit,
) {
    val status by viewModel.status.collectAsState()
    val confirmationResult by viewModel.confirmationResult.collectAsState()
    val isUpdating by viewModel.controller.isUpdating.collectAsState()
    val configured = status as? CheckoutControllerExampleViewModel.Status.Configured
    val session = configured?.session
    val hasShippingAddress = session?.shippingAddress != null
    val hasPaymentMethod = session?.paymentOptionDisplayData != null
    val isComplete = confirmationResult is CheckoutControllerExampleViewModel.ConfirmationResult.Completed

    PlaygroundTheme(
        content = {
            GuidedContent(
                status = status,
                session = session,
                confirmationResult = confirmationResult,
            )
        },
        bottomBarContent = {
            GuidedActions(
                canEdit = configured != null && !isUpdating && !isComplete,
                canConfirm = hasShippingAddress && hasPaymentMethod && !isUpdating && !isComplete,
                onAddShippingAddress = onAddShippingAddress,
                onSelectPaymentMethod = onSelectPaymentMethod,
                onConfirm = onConfirm,
            )
        },
    )
}

@Composable
private fun GuidedContent(
    status: CheckoutControllerExampleViewModel.Status,
    session: Session?,
    confirmationResult: CheckoutControllerExampleViewModel.ConfirmationResult?,
) {
    Text(
        text = "Checkout Shipping Address Element",
        style = MaterialTheme.typography.h5,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Complete each action in order, then verify the refreshed Checkout Session.")
    Spacer(modifier = Modifier.height(16.dp))
    ActionStatus(
        step = "1. Add shipping address",
        detail = if (session?.shippingAddress != null) "Ready" else "Required",
        isReady = session?.shippingAddress != null,
    )
    ActionStatus(
        step = "2. Select payment method",
        detail = session?.paymentOptionDisplayData?.label ?: "Required",
        isReady = session?.paymentOptionDisplayData != null,
    )
    ActionStatus(
        step = "3. Confirm session",
        detail = confirmationResult.confirmationDetail(),
        isReady = confirmationResult is CheckoutControllerExampleViewModel.ConfirmationResult.Completed,
    )
    StatusContent(status)
    ConfirmationResultContent(confirmationResult)
}

@Composable
private fun GuidedActions(
    canEdit: Boolean,
    canConfirm: Boolean,
    onAddShippingAddress: () -> Unit,
    onSelectPaymentMethod: () -> Unit,
    onConfirm: () -> Unit,
) {
    Button(
        onClick = onAddShippingAddress,
        enabled = canEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("1. Add Shipping Address")
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onSelectPaymentMethod,
        enabled = canEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("2. Select Payment Method")
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onConfirm,
        enabled = canConfirm,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("3. Confirm Session")
    }
}

@Composable
private fun ActionStatus(
    step: String,
    detail: String,
    isReady: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(step, style = MaterialTheme.typography.subtitle1)
        Text(
            text = detail,
            color = if (isReady) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
        )
    }
}

@Composable
private fun StatusContent(status: CheckoutControllerExampleViewModel.Status) {
    when (status) {
        CheckoutControllerExampleViewModel.Status.Loading -> {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Preparing Checkout Session...")
        }
        is CheckoutControllerExampleViewModel.Status.Error -> {
            Spacer(modifier = Modifier.height(16.dp))
            Text(status.message, color = MaterialTheme.colors.error)
        }
        is CheckoutControllerExampleViewModel.Status.Configured -> Unit
    }
}

@Composable
private fun ConfirmationResultContent(
    result: CheckoutControllerExampleViewModel.ConfirmationResult?,
) {
    when (result) {
        null -> Unit
        CheckoutControllerExampleViewModel.ConfirmationResult.Canceled -> {
            InlineResult("Confirmation canceled. You can retry.")
        }
        is CheckoutControllerExampleViewModel.ConfirmationResult.Failed -> {
            InlineResult("Confirmation failed: ${result.message}. You can retry.")
        }
        is CheckoutControllerExampleViewModel.ConfirmationResult.Completed -> {
            CompletionVerification(result.session)
        }
    }
}

@Composable
private fun InlineResult(message: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(message, color = MaterialTheme.colors.error)
}

@Composable
private fun CompletionVerification(session: Session?) {
    val shippingAddress = session?.shippingAddress
    Spacer(modifier = Modifier.height(24.dp))
    Text("Refreshed Session Verification", style = MaterialTheme.typography.h6)
    Spacer(modifier = Modifier.height(8.dp))
    VerificationRow("Session ID", session?.id, !session?.id.isNullOrBlank())
    VerificationRow(
        label = "Status",
        value = session?.status.displayName(),
        passes = session?.status is Session.Status.Complete,
    )
    VerificationRow("Name", shippingAddress?.name, !shippingAddress?.name.isNullOrBlank())
    VerificationRow(
        label = "Line 1",
        value = shippingAddress?.address?.line1,
        passes = !shippingAddress?.address?.line1.isNullOrBlank(),
    )
    VerificationRow(
        label = "Line 2",
        value = shippingAddress?.address?.line2,
        passes = !shippingAddress?.address?.line2.isNullOrBlank(),
    )
    VerificationRow("City", shippingAddress?.address?.city, !shippingAddress?.address?.city.isNullOrBlank())
    VerificationRow("State", shippingAddress?.address?.state, !shippingAddress?.address?.state.isNullOrBlank())
    VerificationRow(
        label = "Postal code",
        value = shippingAddress?.address?.postalCode,
        passes = !shippingAddress?.address?.postalCode.isNullOrBlank(),
    )
    VerificationRow(
        label = "Country",
        value = shippingAddress?.address?.country,
        passes = !shippingAddress?.address?.country.isNullOrBlank(),
    )
}

@Composable
private fun VerificationRow(
    label: String,
    value: String?,
    passes: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.body2)
            Text(value ?: "Missing", style = MaterialTheme.typography.caption)
        }
        Text(
            text = if (passes) "PASS" else "FAIL",
            color = if (passes) MaterialTheme.colors.primary else MaterialTheme.colors.error,
            style = MaterialTheme.typography.subtitle2,
        )
    }
}

private fun CheckoutControllerExampleViewModel.ConfirmationResult?.confirmationDetail(): String {
    return when (this) {
        null -> "Pending"
        CheckoutControllerExampleViewModel.ConfirmationResult.Canceled -> "Canceled"
        is CheckoutControllerExampleViewModel.ConfirmationResult.Failed -> "Failed"
        is CheckoutControllerExampleViewModel.ConfirmationResult.Completed -> "Completed"
    }
}

private fun Session.Status?.displayName(): String {
    return when (this) {
        is Session.Status.Open -> "Open"
        is Session.Status.Complete -> "Complete"
        is Session.Status.Expired -> "Expired"
        null -> "Missing"
    }
}

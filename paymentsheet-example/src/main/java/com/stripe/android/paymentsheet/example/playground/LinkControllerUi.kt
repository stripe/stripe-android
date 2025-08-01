@file:Suppress("LongMethod", "MagicNumber")

package com.stripe.android.paymentsheet.example.playground

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.ui.core.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun LinkControllerUi(
    modifier: Modifier,
    controllerState: LinkController.State,
    playgroundState: LinkControllerPlaygroundState,
    onEmailChange: (email: String) -> Unit,
    onPaymentMethodButtonClick: (email: String) -> Unit,
    onCreatePaymentMethodClick: () -> Unit,
    onAuthenticationClick: (email: String, existingOnly: Boolean) -> Unit,
    onRegisterConsumerClick: (email: String, phone: String, country: String, name: String?) -> Unit,
    onErrorMessage: (message: String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var existingOnly by rememberSaveable { mutableStateOf(false) }
    var showRegistrationForm by rememberSaveable { mutableStateOf(false) }
    var registrationPhone by rememberSaveable { mutableStateOf("") }
    var registrationCountry by rememberSaveable { mutableStateOf("US") }
    var registrationName by rememberSaveable { mutableStateOf("") }
    val errorToPresent = playgroundState.linkControllerError()

    val scope = rememberCoroutineScope()
    DisposableEffect(email) {
        val job = scope.launch {
            delay(500L)
            onEmailChange(email)
        }
        onDispose { job.cancel() }
    }

    LaunchedEffect(errorToPresent) {
        if (errorToPresent != null) {
            onErrorMessage(errorToPresent.message ?: "An error occurred")
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusBox(
            controllerState = controllerState,
            playgroundState = playgroundState,
        )
        Divider(Modifier.padding(bottom = 10.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = email,
            label = { Text(text = "Consumer email") },
            onValueChange = { email = it }
        )

        // Registration Form Section
        val chevronRotation by animateFloatAsState(
            targetValue = if (!showRegistrationForm) -180f else 0f,
            label = "chevron_rotation"
        )

        Row(
            modifier = Modifier
                .clickable { showRegistrationForm = !showRegistrationForm }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            Text(
                modifier = Modifier.weight(1f),
                text = if (showRegistrationForm) "Hide registration form" else "Show registration form",
                style = MaterialTheme.typography.subtitle2,
                color = color,
            )
            Icon(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        rotationZ = chevronRotation
                    },
                painter = painterResource(com.stripe.android.uicore.R.drawable.stripe_ic_chevron_down),
                contentDescription = null,
                tint = color,
            )
        }
        AnimatedVisibility(
            visible = showRegistrationForm,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = registrationPhone,
                    label = { Text(text = "Phone") },
                    onValueChange = { registrationPhone = it }
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = registrationCountry,
                    label = { Text(text = "Country") },
                    onValueChange = { registrationCountry = it }
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = registrationName,
                    label = { Text(text = "Name (optional)") },
                    onValueChange = { registrationName = it }
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onRegisterConsumerClick(
                            email.trim(),
                            registrationPhone.trim(),
                            registrationCountry.trim(),
                            registrationName.trim().takeIf { it.isNotEmpty() }
                        )
                    },
                    enabled = email.isNotBlank() &&
                        registrationPhone.isNotBlank() &&
                        registrationCountry.isNotBlank()
                ) {
                    Text("Register")
                }
            }
        }
        Divider(Modifier.padding(bottom = 10.dp))

        AuthenticateButton(
            modifier = Modifier.fillMaxWidth(),
            email = email,
            onClick = { onAuthenticationClick(email, existingOnly) },
        )
        LabeledCheckbox(
            modifier = Modifier
                .clickable(onClick = { existingOnly = !existingOnly })
                .align(Alignment.Start)
                .padding(8.dp),
            label = "Require existing consumer",
            checked = existingOnly,
        )
        Divider(Modifier.padding(top = 10.dp, bottom = 20.dp))

        PaymentMethodButton(
            preview = controllerState.selectedPaymentMethodPreview,
            onClick = { onPaymentMethodButtonClick(email) },
        )
        Spacer(Modifier.height(16.dp))

        ConfirmButton(
            onClick = onCreatePaymentMethodClick,
            enabled = controllerState.selectedPaymentMethodPreview != null,
        )
    }
}

@Composable
private fun StatusBox(
    controllerState: LinkController.State,
    playgroundState: LinkControllerPlaygroundState,
) {
    val statusItems = buildList {
        val lookupText =
            when (playgroundState.lookupConsumerResult) {
                is LinkController.LookupConsumerResult.Success -> {
                    val exists = if (playgroundState.lookupConsumerResult.isConsumer) "exists" else "does not exist"
                    "${playgroundState.lookupConsumerResult.email} $exists"
                }
                is LinkController.LookupConsumerResult.Failed -> {
                    "Failed: ${playgroundState.lookupConsumerResult.error.message}"
                }
                null -> {
                    ""
                }
            }

        add("Configure result" to (playgroundState.configureResult?.toString() ?: ""))
        add("Consumer lookup" to lookupText)
        add("Consumer verified" to (controllerState.isConsumerVerified?.toString() ?: ""))
        add("Payment Method created" to (controllerState.createdPaymentMethod?.id ?: ""))
        add("Authentication result" to (playgroundState.authenticationResult?.toString() ?: ""))
        add("Register result" to (playgroundState.registerConsumerResult?.toString() ?: ""))
    }

    if (statusItems.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusItems.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text = "$label:",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = value,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LinkControllerPlaygroundState.linkControllerError(): Throwable? = listOf(
    (configureResult as? LinkController.ConfigureResult.Failed)?.error,
    (presentPaymentMethodsResult as? LinkController.PresentPaymentMethodsResult.Failed)?.error,
    (lookupConsumerResult as? LinkController.LookupConsumerResult.Failed)?.error,
    (createPaymentMethodResult as? LinkController.CreatePaymentMethodResult.Failed)?.error,
    (authenticationResult as? LinkController.AuthenticationResult.Failed)?.error,
).firstOrNull { it != null }

@Composable
@Preview(showBackground = true)
private fun LinkControllerUiPreview() {
    PaymentSheetExampleTheme {
        LinkControllerUi(
            modifier = Modifier,
            controllerState = LinkController.State(),
            playgroundState = LinkControllerPlaygroundState(),
            onEmailChange = {},
            onPaymentMethodButtonClick = {},
            onCreatePaymentMethodClick = {},
            onAuthenticationClick = { _, _ -> },
            onRegisterConsumerClick = { _, _, _, _ -> },
            onErrorMessage = {},
        )
    }
}

@Composable
private fun ConfirmButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick, enabled = enabled)
            .background(color = Color.Black)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.h6,
        color = Color.White,
        textAlign = TextAlign.Center,
        text = "Confirm",
    )
}

@Composable
private fun PaymentMethodButton(
    preview: LinkController.PaymentMethodPreview?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(color = Color.Black.copy(alpha = 0.1f))
            .heightIn(min = 80.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconSize = 24.dp
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = preview,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { preview ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (preview != null) {
                    Image(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(preview.iconRes),
                        contentDescription = null,
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                    ) {
                        Text(
                            modifier = Modifier,
                            text = preview.label,
                            style = MaterialTheme.typography.h6,
                        )
                        preview.sublabel?.let { sublabel ->
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = sublabel,
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(
                                    alpha = 0.6f
                                ),
                            )
                        }
                    }
                } else {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(R.drawable.stripe_ic_paymentsheet_pm_card),
                        contentDescription = null,
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                    ) {
                        Text(
                            modifier = Modifier,
                            text = "Choose payment method",
                            style = MaterialTheme.typography.h6,
                        )
                    }
                }
            }
        }
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(com.stripe.android.paymentsheet.R.drawable.stripe_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentMethodButtonPreview() {
    PaymentSheetExampleTheme {
        Column {
            PaymentMethodButton(
                modifier = Modifier.padding(16.dp),
                preview = null,
                onClick = {},
            )
            PaymentMethodButton(
                modifier = Modifier.padding(16.dp),
                preview = LinkController.PaymentMethodPreview(
                    label = "Link",
                    sublabel = "Visa (Personal) •••• 4242",
                    iconRes = com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_link_arrow,
                ),
                onClick = {},
            )
        }
    }
}

@Composable
private fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = true,
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = label,
            maxLines = 1,
            style = MaterialTheme.typography.body2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AuthenticateButton(
    email: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = buildString {
                append("Authenticate")
                if (email.isNotBlank()) {
                    append(" ${email.trim()}")
                }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.zIndex
import com.stripe.android.link.ui.verification.LinkVerificationDialog
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.FragmentPaymentOptionsPrimaryButtonBinding
import com.stripe.android.paymentsheet.navigation.Content
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
internal fun PaymentOptionsScreen(
    viewModel: PaymentOptionsViewModel,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    val targetElevation by remember {
        derivedStateOf {
            if (scrollState.value > 0) {
                8.dp
            } else {
                0.dp
            }
        }
    }

    val elevation by animateDpAsState(targetValue = targetElevation)

    Column {
        // We need to set a z-index to make sure that the Surface's elevation shadow is rendered
        // correctly above the screen content.
        Surface(elevation = elevation, modifier = Modifier.zIndex(1f)) {
            PaymentSheetTopBar(viewModel)
        }

        PaymentOptionsScreenContent(
            viewModel = viewModel,
            modifier = modifier.verticalScroll(scrollState),
        )
    }
}

@Composable
internal fun PaymentOptionsScreenContent(
    viewModel: PaymentOptionsViewModel,
    modifier: Modifier = Modifier,
) {
    val headerText by viewModel.headerText.collectAsState(initial = null)
    val currentScreen by viewModel.currentScreen.collectAsState()

    val errorMessage by viewModel.error.collectAsState(initial = null)
    val notesText by viewModel.notesText.collectAsState()

    val showLinkDialog by viewModel.linkHandler.showLinkVerificationDialog.collectAsState()
    val bottomPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing_bottom)

    Column(
        modifier = modifier.padding(bottom = bottomPadding),
    ) {
        if (showLinkDialog) {
            LinkVerificationDialog(
                linkLauncher = viewModel.linkHandler.linkLauncher,
                onResult = viewModel.linkHandler::handleLinkVerificationResult,
            )
        }

        headerText?.let { text ->
            StripeTheme {
                H4Text(
                    text = stringResource(text),
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .padding(horizontal = 20.dp),
                )
            }
        }

        currentScreen.Content(viewModel)

        errorMessage?.let { error ->
            ErrorMessage(
                error = error,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }

        AndroidViewBinding(
            factory = FragmentPaymentOptionsPrimaryButtonBinding::inflate,
        )

        notesText?.let { text ->
            Html(
                html = text,
                color = MaterialTheme.stripeColors.subtitle,
                style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

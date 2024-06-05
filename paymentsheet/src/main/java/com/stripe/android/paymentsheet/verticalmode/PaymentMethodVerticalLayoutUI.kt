package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.uicore.image.StripeImageLoader

internal const val TEST_TAG_MANAGE_SCREEN = "TEST_TAG_MANAGE_SCREEN"

@Composable
internal fun PaymentMethodVerticalLayoutUI(interactor: PaymentMethodVerticalLayoutInteractor) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val state by interactor.state.collectAsState()

    PaymentMethodVerticalLayoutUI(
        paymentMethods = state.supportedPaymentMethods,
        selectedIndex = state.selectedPaymentMethodIndex,
        isEnabled = !state.isProcessing,
        onViewMorePaymentMethods = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods
            )
        },
        onItemSelectedListener = {
            interactor.handleViewAction(PaymentMethodVerticalLayoutInteractor.ViewAction.PaymentMethodSelected(it.code))
        },
        imageLoader = imageLoader,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun PaymentMethodVerticalLayoutUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(
            onClick = { onViewMorePaymentMethods() },
            modifier = Modifier.testTag(TEST_TAG_MANAGE_SCREEN)
        ) {
            Text(text = "Go to manage screen")
        }

        NewPaymentMethodVerticalLayoutUI(
            paymentMethods = paymentMethods,
            selectedIndex = selectedIndex,
            isEnabled = isEnabled,
            onItemSelectedListener = onItemSelectedListener,
            imageLoader = imageLoader
        )
    }
}

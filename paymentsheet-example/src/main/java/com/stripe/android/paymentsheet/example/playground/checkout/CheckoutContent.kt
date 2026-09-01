@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement

@Composable
internal fun CheckoutContent(
    session: Session,
    paymentElement: PaymentElement,
    currencySelectorElement: CurrencySelectorElement,
    expressCheckoutElement: ExpressCheckoutElement,
    isUpdating: Boolean,
    operationMessage: String?,
    onApplyPromotionCode: (String) -> Unit,
    onRemovePromotionCode: () -> Unit,
    onUpdateEmail: (String) -> Unit,
) {
    Text(text = "Tax status: ${session.tax.status}", style = MaterialTheme.typography.h6)
    Spacer(Modifier.height(16.dp))
    LineItemsSection(session)
    TotalSummarySection(session)

    if (session.isCurrencySelectorAvailable) {
        Spacer(Modifier.height(16.dp))
        currencySelectorElement.Content()
    }
    if (session.availableExpressCheckoutPaymentMethods.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        expressCheckoutElement.Content()
    }
    Spacer(Modifier.height(16.dp))
    paymentElement.Content()
    Spacer(Modifier.height(20.dp))
    SessionOperations(
        initialEmail = session.email.orEmpty(),
        isUpdating = isUpdating,
        message = operationMessage,
        onApplyPromotionCode = onApplyPromotionCode,
        onRemovePromotionCode = onRemovePromotionCode,
        onUpdateEmail = onUpdateEmail,
    )
}

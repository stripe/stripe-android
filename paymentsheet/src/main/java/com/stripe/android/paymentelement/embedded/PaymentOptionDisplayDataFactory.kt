package com.stripe.android.paymentelement.embedded

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.darkThemeIconUrl
import com.stripe.android.paymentsheet.model.drawableResourceId
import com.stripe.android.paymentsheet.model.label
import com.stripe.android.paymentsheet.model.lightThemeIconUrl
import com.stripe.android.paymentsheet.model.paymentMethodType
import javax.inject.Inject

@ExperimentalEmbeddedPaymentElementApi
internal class PaymentOptionDisplayDataFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val context: Context,
) {
    fun create(
        selection: PaymentSelection?,
        paymentMethodMetadata: PaymentMethodMetadata,
        billingDetails: PaymentSheet.BillingDetails?
    ): EmbeddedPaymentElement.PaymentOptionDisplayData? {
        if (selection == null) {
            return null
        }

        val mandate = when (selection) {
            is PaymentSelection.New -> {
                paymentMethodMetadata.formElementsForCode(
                    code = selection.paymentMethodType,
                    uiDefinitionFactoryArgumentsFactory = NullUiDefinitionFactoryHelper.nullEmbeddedUiDefinitionFactory
                )?.firstNotNullOfOrNull { it.mandateText }
            }
            is PaymentSelection.Saved -> {
                selection.mandateText(
                    paymentMethodMetadata.merchantName,
                    paymentMethodMetadata.hasIntentToSetup()
                )
            }
            is PaymentSelection.ExternalPaymentMethod -> null
            PaymentSelection.GooglePay -> null
            PaymentSelection.Link -> null
        }

        return EmbeddedPaymentElement.PaymentOptionDisplayData(
            label = selection.label.resolve(context),
            imageLoader = {
                iconLoader.load(
                    drawableResourceId = selection.drawableResourceId,
                    lightThemeIconUrl = selection.lightThemeIconUrl,
                    darkThemeIconUrl = selection.darkThemeIconUrl,
                )
            },
            billingDetails = billingDetails,
            paymentMethodType = selection.paymentMethodType,
            mandateText = if (mandate == null) null else AnnotatedString(mandate.resolve(context))
        )
    }
}

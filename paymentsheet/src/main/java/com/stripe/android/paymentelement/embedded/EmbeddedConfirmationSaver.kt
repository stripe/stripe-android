package com.stripe.android.paymentelement.embedded

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.canSave
import javax.inject.Inject
import javax.inject.Provider

internal fun interface EmbeddedConfirmationSaver {
    fun save(stripeIntent: StripeIntent)
}

internal class DefaultEmbeddedConfirmationSaver @Inject constructor(
    private val prefsRepositoryFactory: PrefsRepository.Factory,
    private val paymentMethodMetadataProvider: Provider<PaymentMethodMetadata?>,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val initializationModeProvider: Provider<PaymentElementLoader.InitializationMode?>,
) : EmbeddedConfirmationSaver {
    override fun save(stripeIntent: StripeIntent) {
        val paymentMethodMetadata = paymentMethodMetadataProvider.get() ?: return
        val customerId = paymentMethodMetadata.customerMetadata?.id

        val currentSelection = selectionHolder.selection.value

        val selectionToSave = when (currentSelection) {
            is PaymentSelection.New -> stripeIntent.paymentMethod.takeIf {
                val initializationMode = initializationModeProvider.get()
                val alwaysSave = paymentMethodMetadata.forceSetupFutureUseBehaviorAndNewMandate
                initializationMode != null && (currentSelection.canSave(initializationMode) || alwaysSave)
            }?.let { method ->
                PaymentSelection.Saved(method)
            }
            is PaymentSelection.Saved -> {
                when (currentSelection.walletType) {
                    PaymentSelection.Saved.WalletType.GooglePay -> {
                        PaymentSelection.GooglePay
                    }
                    PaymentSelection.Saved.WalletType.Link -> {
                        // Don't save as Link, but instead as the actual payment method. If the payment method isn't
                        // attached to the customer, we will fallback to Link during load.
                        currentSelection
                    }
                    null -> currentSelection
                }
            }
            else -> currentSelection
        }

        selectionToSave?.let {
            prefsRepositoryFactory.create(customerId).savePaymentSelection(selectionToSave)
        }
    }
}

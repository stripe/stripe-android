package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import javax.inject.Inject

internal class ConfirmationSaver @Inject constructor(
    private val prefsRepositoryFactory: PrefsRepository.Factory,
) : ConfirmationHandler.Saver {
    override fun save(
        stripeIntent: StripeIntent,
        confirmationOption: ConfirmationHandler.Option,
        alwaysSave: Boolean,
    ) {
        val savedSelection = when (confirmationOption) {
            is PaymentMethodConfirmationOption.New -> savedSelectionForNew(
                stripeIntent = stripeIntent,
                confirmationOption = confirmationOption,
                alwaysSave = alwaysSave,
            )
            is PaymentMethodConfirmationOption.Saved -> {
                when (confirmationOption.paymentMethod.card?.wallet?.walletType) {
                    Wallet.Type.GooglePay -> SavedSelection.GooglePay
                    Wallet.Type.Link -> SavedSelection.Link
                    else -> SavedSelection.PaymentMethod(confirmationOption.paymentMethod.id)
                }
            }
            is GooglePayConfirmationOption -> SavedSelection.GooglePay
            is LinkConfirmationOption -> SavedSelection.Link
            else -> null
        }

        savedSelection?.let {
            prefsRepositoryFactory.create(stripeIntent.paymentMethod?.customerId).setSavedSelection(savedSelection)
        }
    }

    private fun savedSelectionForNew(
        stripeIntent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        alwaysSave: Boolean,
    ): SavedSelection? {
        return stripeIntent.paymentMethod.takeIf {
            val setupFutureUsageSet = when (stripeIntent) {
                is PaymentIntent -> {
                    stripeIntent.isSetupFutureUsageSet(confirmationOption.createParams.typeCode)
                }
                is SetupIntent -> true
            }
            alwaysSave || confirmationOption.shouldSave || setupFutureUsageSet
        }?.let { method ->
            SavedSelection.PaymentMethod(method.id)
        }
    }
}

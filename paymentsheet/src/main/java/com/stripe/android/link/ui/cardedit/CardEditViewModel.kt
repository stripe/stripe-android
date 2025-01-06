package com.stripe.android.link.ui.cardedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.link.ui.paymentmenthod.Factory
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.analytics.code
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CardEditViewModel @Inject constructor(
    private val paymentDetailsId: String,
    private val linkAccount: LinkAccount,
    private val configuration: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val logger: Logger,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {
    private val paymentMethodMetadata = Factory.paymentMethodMetadata(configuration)
    private val formHelper = Factory.formHelper(
        configuration = configuration,
        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
        selectionUpdater = {
//            it.code()
//            updateSelection(it)
        }
    )

    init {
        viewModelScope.launch {
            loadCard()
        }
    }

    private suspend fun loadCard() {
        linkAccountManager.listPaymentDetails(
            paymentMethodTypes = configuration.stripeIntent.supportedPaymentMethodTypes(linkAccount)
        ).fold(
            onSuccess = { paymentDetails ->
                val card = paymentDetails.paymentDetails
                    .filterIsInstance<ConsumerPaymentDetails.Card>()
                    .firstOrNull { it.id == paymentDetailsId }

                if (card == null) {
                    logger.error("CardEditViewModel error: ", CardNotFoundException())
                    return@fold dismissWithResult(LinkActivityResult.Failed(CardNotFoundException()))
                }

//                val formElements = card.formElements()
//                _formElements.value = formElements
            },
            onFailure = { e ->
                logger.error("CardEditViewModel error: ", e)
                dismissWithResult(LinkActivityResult.Failed(e))
            }
        )
    }
}
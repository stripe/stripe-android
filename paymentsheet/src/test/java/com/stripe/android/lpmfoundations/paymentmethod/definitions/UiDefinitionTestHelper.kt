package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.runtime.Composable
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.AccountRange
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.mockito.kotlin.mock

@Composable
internal fun PaymentMethodDefinition.CreateFormUi(
    metadata: PaymentMethodMetadata,
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
) {
    metadata.setTestAddressRepository(mock())
    metadata.setTestCardAccountRangeRepositoryFactory(NullCardAccountRangeRepositoryFactory)
    val formElements = formElements(
        metadata = metadata,
        paymentMethodCreateParams = paymentMethodCreateParams,
        paymentMethodExtraParams = paymentMethodExtraParams,
    )
    FormUI(
        hiddenIdentifiers = emptySet(),
        enabled = true,
        elements = formElements,
        lastTextFieldIdentifier = null,
    )
}

private object NullCardAccountRangeRepositoryFactory : CardAccountRangeRepository.Factory {
    override fun create(): CardAccountRangeRepository {
        return NullCardAccountRangeRepository
    }

    override fun createWithStripeRepository(
        stripeRepository: StripeRepository,
        publishableKey: String
    ): CardAccountRangeRepository {
        return NullCardAccountRangeRepository
    }

    private object NullCardAccountRangeRepository : CardAccountRangeRepository {
        override suspend fun getAccountRange(cardNumber: CardNumber.Unvalidated): AccountRange? {
            return null
        }

        override suspend fun getAccountRanges(cardNumber: CardNumber.Unvalidated): List<AccountRange>? {
            return null
        }

        override val loading: StateFlow<Boolean> = stateFlowOf(false)
    }
}

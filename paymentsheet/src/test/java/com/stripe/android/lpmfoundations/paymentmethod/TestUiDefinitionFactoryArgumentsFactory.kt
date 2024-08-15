package com.stripe.android.lpmfoundations.paymentmethod

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.model.AccountRange
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal object TestUiDefinitionFactoryArgumentsFactory {
    fun create(
        paymentMethodCreateParams: PaymentMethodCreateParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
    ): UiDefinitionFactory.Arguments.Factory {
        val context: Context? = try {
            ApplicationProvider.getApplicationContext<Application>()
        } catch (_: Throwable) {
            null
        }
        return UiDefinitionFactory.Arguments.Factory.Default(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory(context),
            paymentMethodCreateParams = paymentMethodCreateParams,
            paymentMethodExtraParams = paymentMethodExtraParams,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            onLinkInlineSignupStateChanged = { throw AssertionError("Not implemented") },
        )
    }

    private fun cardAccountRangeRepositoryFactory(context: Context?): CardAccountRangeRepository.Factory {
        return if (context == null) {
            NullCardAccountRangeRepositoryFactory
        } else {
            DefaultCardAccountRangeRepositoryFactory(context)
        }
    }
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

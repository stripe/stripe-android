package com.stripe.android.lpmfoundations.paymentmethod

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory

internal object TestUiDefinitionFactoryArgumentsFactory {
    fun create(
        paymentMethodCreateParams: PaymentMethodCreateParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
        linkInlineHandler: LinkInlineHandler? = null,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
        initialLinkUserInput: UserInput? = null,
        setAsDefaultMatchesSaveForFutureUse: Boolean = false,
    ): UiDefinitionFactory.Arguments.Factory {
        val context: Context? = try {
            ApplicationProvider.getApplicationContext<Application>()
        } catch (_: Throwable) {
            null
        }
        return UiDefinitionFactory.Arguments.Factory.Default(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory(context),
            paymentMethodCreateParams = paymentMethodCreateParams,
            paymentMethodOptionsParams = paymentMethodOptionsParams,
            paymentMethodExtraParams = paymentMethodExtraParams,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            initialLinkUserInput = initialLinkUserInput,
            onLinkInlineSignupStateChanged = { throw AssertionError("Not implemented") },
            setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            linkInlineHandler = linkInlineHandler,
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

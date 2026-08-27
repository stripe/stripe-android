package com.stripe.android.lpmfoundations.paymentmethod

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal object TestUiDefinitionFactoryArgumentsFactory {
    fun create(
        paymentMethodCreateParams: PaymentMethodCreateParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        initialValues: Map<IdentifierSpec, String?>? = null,
        linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
        linkInlineHandler: LinkInlineHandler? = null,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
        initialLinkUserInput: UserInput? = null,
        setAsDefaultMatchesSaveForFutureUse: Boolean = false,
        automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper? = null,
        tapToAddHelper: TapToAddHelper? = null,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper? = null,
        isNfcScanningAvailable: IsNfcScanningAvailable? = null,
    ): UiDefinitionFactory.Arguments.Factory {
        val context: Context? = try {
            ApplicationProvider.getApplicationContext<Application>()
        } catch (_: Throwable) {
            null
        }
        val delegate = UiDefinitionFactory.Arguments.Factory.Default(
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
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
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = tapToAddHelper,
            paymentMethodMessagingPromotionsHelper = paymentMethodMessagePromotionsHelper,
            isNfcScanningAvailable = isNfcScanningAvailable,
        )
        return if (initialValues == null) {
            delegate
        } else {
            InitialValuesOverridingFactory(delegate = delegate, initialValues = initialValues)
        }
    }

    /**
     * Seeds a form the way production does: at element construction, through
     * [UiDefinitionFactory.Arguments.initialValues], rather than by pushing values into elements
     * that have already been built.
     */
    private class InitialValuesOverridingFactory(
        private val delegate: UiDefinitionFactory.Arguments.Factory,
        private val initialValues: Map<IdentifierSpec, String?>,
    ) : UiDefinitionFactory.Arguments.Factory {
        override fun create(
            metadata: PaymentMethodMetadata,
            requiresMandate: Boolean,
        ): UiDefinitionFactory.Arguments {
            val arguments = delegate.create(metadata, requiresMandate)
            return arguments.copy(initialValues = arguments.initialValues + initialValues)
        }
    }

    private fun cardAccountRangeRepositoryFactory(context: Context?): CardAccountRangeRepository.Factory {
        return if (context == null) {
            NullCardAccountRangeRepositoryFactory
        } else {
            DefaultCardAccountRangeRepositoryFactory(context)
        }
    }
}

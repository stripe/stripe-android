package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.containsVolatileDifferences
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Provider

internal fun interface EmbeddedSelectionChooser {
    fun choose(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection?,
        newSelection: PaymentSelection?,
        newConfiguration: CommonConfiguration,
        formSheetAction: EmbeddedPaymentElement.FormSheetAction,
    ): PaymentSelection?
}

internal class DefaultEmbeddedSelectionChooser @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val formHelperFactory: EmbeddedFormHelperFactory,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
) : EmbeddedSelectionChooser {
    // Compatibility checks only inspect synchronously constructed form metadata.
    private val coroutineScope = CoroutineScope(Job().apply { cancel() })

    private var previousConfiguration: CommonConfiguration?
        get() = savedStateHandle[PREVIOUS_CONFIGURATION_KEY]
        set(value) = savedStateHandle.set(PREVIOUS_CONFIGURATION_KEY, value)

    private var previousPaymentMethodMetadata: PaymentMethodMetadata?
        get() = savedStateHandle[PREVIOUS_PAYMENT_METHOD_METADATA_KEY]
        set(value) = savedStateHandle.set(PREVIOUS_PAYMENT_METHOD_METADATA_KEY, value)

    override fun choose(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection?,
        newSelection: PaymentSelection?,
        newConfiguration: CommonConfiguration,
        formSheetAction: EmbeddedPaymentElement.FormSheetAction,
    ): PaymentSelection? {
        val result = newSelection?.takeIf {
            shouldUseNewSelectionAsDefaultPaymentMethod(
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethods = paymentMethods,
                newSelection = it,
            )
        } ?: previousSelection?.takeIf {
            shouldUsePreviousSelection(
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethods = paymentMethods,
                previousSelection = it,
                newConfiguration = newConfiguration
            )
        } ?: newSelection

        if (
            internalRowSelectionCallback.get() != null &&
            formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm
        ) {
            return null
        }

        previousConfiguration = newConfiguration
        previousPaymentMethodMetadata = paymentMethodMetadata

        return result
    }

    /**
     * In the case that there is a defaultPaymentMethod and setAsDefault is enabled, newSelection.paymentMethod
     * will be the defaultPaymentMethod. See [DefaultPaymentElementLoader.retrieveInitialPaymentSelection]
     */
    private fun shouldUseNewSelectionAsDefaultPaymentMethod(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        newSelection: PaymentSelection,
    ): Boolean {
        return paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled == true &&
            newSelection is PaymentSelection.Saved &&
            canUseSelection(
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethods = paymentMethods,
                previousSelection = newSelection,
            )
    }

    private fun shouldUsePreviousSelection(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection,
        newConfiguration: CommonConfiguration,
    ): Boolean {
        return canUseSelection(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethods = paymentMethods,
            previousSelection = previousSelection,
        ) && previousConfiguration?.containsVolatileDifferences(newConfiguration) != true
    }

    private fun canUseSelection(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection,
    ): Boolean {
        // The types that are allowed for this intent, as returned by the backend
        val allowedTypes = paymentMethodMetadata.supportedPaymentMethodTypes()

        return when (previousSelection) {
            is PaymentSelection.New -> {
                val code = previousSelection.paymentMethodCreateParams.typeCode
                code in allowedTypes && hasCompatibleForm(
                    previousSelection = previousSelection,
                    paymentMethodMetadata = paymentMethodMetadata
                )
            }
            is PaymentSelection.Saved -> {
                val paymentMethod = previousSelection.paymentMethod
                val code = paymentMethod.type?.code
                code in allowedTypes && paymentMethod in (paymentMethods ?: emptyList())
            }
            is PaymentSelection.GooglePay -> {
                paymentMethodMetadata.isGooglePayReady
            }
            is PaymentSelection.Link -> {
                paymentMethodMetadata.linkState != null
            }
            is PaymentSelection.ExternalPaymentMethod -> {
                paymentMethodMetadata.isExternalPaymentMethod(previousSelection.type)
            }
            is PaymentSelection.CustomPaymentMethod -> {
                paymentMethodMetadata.isCustomPaymentMethod(previousSelection.id)
            }
        }
    }

    private fun hasCompatibleForm(
        previousSelection: PaymentSelection.New,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Boolean {
        val newFormDefinitionFactory = formHelperFactory.createFormDefinitionFactory(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            // Card scan auto-launch is only relevant in the form, not when selecting the default.
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            // Not important for determining formType so use default value
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            paymentMethodMessagePromotionsHelper = null,
            linkInlineHandler = LinkInlineHandler.create(),
        )
        val newFormType = newFormDefinitionFactory.formTypeForCode(previousSelection.paymentMethodType)
        if (newFormType != FormHelper.FormType.UserInteractionRequired) {
            return true
        }
        return previousPaymentMethodMetadata?.let { previousPaymentMethodMetadata ->
            val previousFormDefinitionFactory = formHelperFactory.createFormDefinitionFactory(
                coroutineScope = coroutineScope,
                paymentMethodMetadata = previousPaymentMethodMetadata,
                // Card scan auto-launch is only relevant in the form, not when selecting the default.
                automaticallyLaunchedCardScanFormDataHelper = null,
                tapToAddHelper = null,
                // Not important for determining formType so use default value
                setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
                paymentMethodMessagePromotionsHelper = null,
                linkInlineHandler = LinkInlineHandler.create(),
            )
            val previousFormElements =
                previousFormDefinitionFactory.formElementsForCode(previousSelection.paymentMethodType)
            val newFormElements = newFormDefinitionFactory.formElementsForCode(previousSelection.paymentMethodType)
            previousFormElements.size >= newFormElements.size
        } == true
    }

    companion object {
        const val PREVIOUS_CONFIGURATION_KEY = "DefaultEmbeddedSelectionChooser_PREVIOUS_CONFIGURATION_KEY"
        const val PREVIOUS_PAYMENT_METHOD_METADATA_KEY =
            "DefaultEmbeddedSelectionChooser_PREVIOUS_PAYMENT_METHOD_METADATA_KEY"
    }
}

package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor.FormType
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.stateFlowOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>

    fun dataLoaded(paymentMethodMetadata: PaymentMethodMetadata)
}

internal fun interface EmbeddedContentHelperFactory {
    fun create(coroutineScope: CoroutineScope): EmbeddedContentHelper
}

@AssistedFactory
internal interface DefaultEmbeddedContentHelperFactory : EmbeddedContentHelperFactory {
    override fun create(coroutineScope: CoroutineScope): DefaultEmbeddedContentHelper
}

internal class DefaultEmbeddedContentHelper @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    @IOContext private val workContext: CoroutineContext,
    private val customerRepository: CustomerRepository,
    private val selectionHolder: EmbeddedSelectionHolder,
) : EmbeddedContentHelper {

    private val paymentMethodMetadata: StateFlow<PaymentMethodMetadata?> = savedStateHandle.getStateFlow(
        key = PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT,
        initialValue = null,
    )
    private val mandate: StateFlow<ResolvableString?> = savedStateHandle.getStateFlow(
        key = MANDATE_KEY_EMBEDDED_CONTENT,
        initialValue = null,
    )
    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    init {
        coroutineScope.launch {
            paymentMethodMetadata.collect { paymentMethodMetadata ->
                _embeddedContent.value = if (paymentMethodMetadata == null) {
                    null
                } else {
                    EmbeddedContent(
                        interactor = createInteractor(
                            coroutineScope = coroutineScope,
                            paymentMethodMetadata = paymentMethodMetadata,
                        )
                    )
                }
            }
        }
        coroutineScope.launch {
            mandate.collect { mandate ->
                _embeddedContent.update { originalEmbeddedContent ->
                    originalEmbeddedContent?.copy(mandate = mandate)
                }
            }
        }
    }

    override fun dataLoaded(paymentMethodMetadata: PaymentMethodMetadata) {
        savedStateHandle[PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT] = paymentMethodMetadata
    }

    private fun createInteractor(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): PaymentMethodVerticalLayoutInteractor {
        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
        val customerStateHolder: CustomerStateHolder = CustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
        )
        val formHelper = createFormHelper(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
        )
        val savedPaymentMethodMutator = createSavedPaymentMethodMutator(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
        )

        return DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = stateFlowOf(false),
            selection = selectionHolder.selection,
            paymentMethodIncentiveInteractor = paymentMethodIncentiveInteractor,
            formTypeForCode = { code ->
                if (formHelper.requiresFormScreen(code)) {
                    FormType.UserInteractionRequired
                } else {
                    val mandate = formHelper.formElementsForCode(code).firstNotNullOfOrNull { it.mandateText }
                    if (mandate == null) {
                        FormType.Empty
                    } else {
                        FormType.MandateOnly(mandate)
                    }
                }
            },
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            transitionToManageScreen = {
            },
            transitionToManageOneSavedPaymentMethodScreen = {
            },
            transitionToFormScreen = {
            },
            paymentMethods = customerStateHolder.paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            providePaymentMethodName = savedPaymentMethodMutator.providePaymentMethodName,
            canRemove = customerStateHolder.canRemove,
            onEditPaymentMethod = {
            },
            onSelectSavedPaymentMethod = {
                setSelection(PaymentSelection.Saved(it))
            },
            walletsState = stateFlowOf(null),
            canShowWalletsInline = true,
            onMandateTextUpdated = { updatedMandate ->
                savedStateHandle[MANDATE_KEY_EMBEDDED_CONTENT] = updatedMandate
            },
            updateSelection = { updatedSelection ->
                setSelection(updatedSelection)
            },
            isCurrentScreen = stateFlowOf(false),
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportFormShown = eventReporter::onPaymentMethodFormShown,
            onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
        )
    }

    private fun createSavedPaymentMethodMutator(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
    ): SavedPaymentMethodMutator {
        return SavedPaymentMethodMutator(
            eventReporter = eventReporter,
            coroutineScope = coroutineScope,
            workContext = workContext,
            customerRepository = customerRepository,
            selection = selectionHolder.selection,
            providePaymentMethodName = { code ->
                code?.let {
                    paymentMethodMetadata.supportedPaymentMethodForCode(code)
                }?.displayName.orEmpty()
            },
            clearSelection = {
                setSelection(null)
            },
            customerStateHolder = customerStateHolder,
            onPaymentMethodRemoved = {
            },
            onModifyPaymentMethod = { _, _, _, _, _ ->
            },
            onUpdatePaymentMethod = { _, _, _, _ ->
            },
            navigationPop = {
            },
            isCbcEligible = {
                paymentMethodMetadata.cbcEligibility is CardBrandChoiceEligibility.Eligible
            },
            isGooglePayReady = stateFlowOf(false),
            isLinkEnabled = stateFlowOf(false),
            isNotPaymentFlow = false,
        )
    }

    private fun createFormHelper(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): FormHelper {
        val linkInlineHandler = createLinkInlineHandler(coroutineScope)
        return FormHelper(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = {
                when (val currentSelection = selectionHolder.selection.value) {
                    is PaymentSelection.ExternalPaymentMethod -> {
                        NewOrExternalPaymentSelection.External(currentSelection)
                    }
                    is PaymentSelection.New -> {
                        NewOrExternalPaymentSelection.New(currentSelection)
                    }
                    else -> null
                }
            },
            selectionUpdater = {
                setSelection(it)
            },
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            onLinkInlineSignupStateChanged = linkInlineHandler::onStateUpdated,
        )
    }

    private fun createLinkInlineHandler(
        coroutineScope: CoroutineScope,
    ): LinkInlineHandler {
        return LinkInlineHandler(
            coroutineScope = coroutineScope,
            payWithLink = { _, _, _ ->
            },
            selection = selectionHolder.selection,
            updateLinkPrimaryButtonUiState = {
            },
            primaryButtonLabel = stateFlowOf(null),
            shouldCompleteLinkFlowInline = false,
        )
    }

    private fun setSelection(paymentSelection: PaymentSelection?) {
        savedStateHandle[MANDATE_KEY_EMBEDDED_CONTENT] = null
        selectionHolder.set(paymentSelection)
    }

    companion object {
        const val PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT = "PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT"
        const val MANDATE_KEY_EMBEDDED_CONTENT = "MANDATE_KEY_EMBEDDED_CONTENT"
    }
}

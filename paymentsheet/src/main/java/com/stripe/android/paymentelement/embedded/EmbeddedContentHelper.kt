package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
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
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>

    fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        rowStyle: Embedded.RowStyle,
        embeddedViewDisplaysMandateText: Boolean,
    )

    fun setFormLauncher(formLauncher: ((code: String, paymentMethodMetaData: PaymentMethodMetadata?) -> Unit)?)

    fun clearFormLauncher()
}

internal fun interface EmbeddedContentHelperFactory {
    fun create(coroutineScope: CoroutineScope): EmbeddedContentHelper
}

@AssistedFactory
internal interface DefaultEmbeddedContentHelperFactory : EmbeddedContentHelperFactory {
    override fun create(coroutineScope: CoroutineScope): DefaultEmbeddedContentHelper
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
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

    private val mandate: StateFlow<ResolvableString?> = savedStateHandle.getStateFlow(
        key = MANDATE_KEY_EMBEDDED_CONTENT,
        initialValue = null,
    )
    private val state: StateFlow<State?> = savedStateHandle.getStateFlow(
        key = STATE_KEY_EMBEDDED_CONTENT,
        initialValue = null
    )
    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    private var formLauncher: ((code: String, paymentMethodMetaData: PaymentMethodMetadata?) -> Unit)? = null

    init {
        coroutineScope.launch {
            state.collect { state ->
                _embeddedContent.value = if (state == null) {
                    null
                } else {
                    EmbeddedContent(
                        interactor = createInteractor(
                            coroutineScope = coroutineScope,
                            paymentMethodMetadata = state.paymentMethodMetadata,
                        ),
                        rowStyle = state.rowStyle
                    )
                }
            }
        }
        coroutineScope.launch {
            mandate.collect { mandate ->
                if (state.value?.embeddedViewDisplaysMandateText == true) {
                    _embeddedContent.update { originalEmbeddedContent ->
                        originalEmbeddedContent?.copy(mandate = mandate)
                    }
                }
            }
        }
    }

    override fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        rowStyle: Embedded.RowStyle,
        embeddedViewDisplaysMandateText: Boolean,
    ) {
        savedStateHandle[STATE_KEY_EMBEDDED_CONTENT] = State(
            paymentMethodMetadata = paymentMethodMetadata,
            rowStyle = rowStyle,
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        )
    }

    override fun setFormLauncher(
        formLauncher: ((code: String, paymentMethodMetaData: PaymentMethodMetadata?) -> Unit)?
    ) {
        this.formLauncher = formLauncher
    }

    override fun clearFormLauncher() {
        formLauncher = null
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
            transitionToFormScreen = {
                formLauncher?.invoke(it, state.value?.paymentMethodMetadata)
            },
            paymentMethods = customerStateHolder.paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            providePaymentMethodName = savedPaymentMethodMutator.providePaymentMethodName,
            canRemove = customerStateHolder.canRemove,
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
            isCurrentScreen = stateFlowOf(true),
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
        return DefaultFormHelper(
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
        if (paymentSelection != selectionHolder.selection.value) {
            savedStateHandle[MANDATE_KEY_EMBEDDED_CONTENT] = null
        }
        selectionHolder.set(paymentSelection)
    }

    @Parcelize
    class State(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val rowStyle: Embedded.RowStyle,
        val embeddedViewDisplaysMandateText: Boolean,
    ) : Parcelable

    companion object {
        const val MANDATE_KEY_EMBEDDED_CONTENT = "MANDATE_KEY_EMBEDDED_CONTENT"
        const val STATE_KEY_EMBEDDED_CONTENT = "STATE_KEY_EMBEDDED_CONTENT"
    }
}

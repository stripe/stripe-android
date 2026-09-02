package com.stripe.android.elements

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodOrientation
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.verticalmode.EmbeddedMandate
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.Closeable
import javax.inject.Inject

internal interface PaymentElementHorizontalContentHelper {
    val content: StateFlow<PaymentElementHorizontalContent?>
}

internal class DefaultPaymentElementHorizontalContentHelper @Inject constructor(
    @ViewModelScope coroutineScope: CoroutineScope,
    state: StateFlow<EmbeddedContentHelperStateHolder.State?>,
    customerStateHolder: CustomerStateHolder,
    private val contentFactory: PaymentElementHorizontalContentFactory,
) : PaymentElementHorizontalContentHelper {
    private val _content = MutableStateFlow<PaymentElementHorizontalContent?>(null)
    override val content: StateFlow<PaymentElementHorizontalContent?> = _content.asStateFlow()

    init {
        coroutineScope.launch {
            combine(state, customerStateHolder.paymentMethods) { currentState, savedPaymentMethods ->
                currentState?.takeIf {
                    savedPaymentMethods.isEmpty() &&
                        it.paymentMethodMetadata.paymentMethodOrientation() == PaymentMethodOrientation.Horizontal
                }
            }.distinctUntilChanged().collect { horizontalState ->
                val replacement = horizontalState?.let(contentFactory::create)
                _content.value?.close()
                _content.value = replacement
            }
        }
    }
}

internal fun interface PaymentElementHorizontalContentFactory {
    fun create(state: EmbeddedContentHelperStateHolder.State): PaymentElementHorizontalContent
}

internal class DefaultPaymentElementHorizontalContentFactory @Inject constructor(
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    private val confirmationHandler: ConfirmationHandler,
    private val eventReporter: EventReporter,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : PaymentElementHorizontalContentFactory {
    @Suppress("LongMethod")
    override fun create(state: EmbeddedContentHelperStateHolder.State): PaymentElementHorizontalContent {
        val paymentMethodMetadata = state.paymentMethodMetadata
        val interactorScope = coroutineScope.childScope(Dispatchers.Main)
        val initialCode = selectionHolder.selection.value?.paymentMethodType
            ?.takeIf { it in paymentMethodMetadata.supportedPaymentMethodTypes() }
            ?: paymentMethodMetadata.supportedPaymentMethodTypes().first()
        val mandate = MutableStateFlow<ResolvableString?>(null)
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = interactorScope,
            setAsDefaultMatchesSaveForFutureUse = true,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            automaticallyLaunchedCardScanFormDataHelper =
                embeddedFormHelperFactory.createAutomaticallyLaunchedCardScanFormDataHelper(
                    selectedPaymentMethodCode = initialCode,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
            tapToAddHelper = null,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            autocompleteAddressInteractorFactory = null,
            selectionUpdater = selectionHolder::setSelection,
        )
        val bankFormInteractor = BankFormInteractor(
            updateSelection = selectionHolder::setSelection,
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ),
        )
        val validationRequested = MutableSharedFlow<Unit>()
        val interactor = DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initialCode,
            selection = selectionHolder.selection,
            processing = confirmationHandler.state.mapAsStateFlow {
                it is ConfirmationHandler.State.Confirming
            },
            validationRequested = validationRequested,
            incentive = bankFormInteractor.paymentMethodIncentiveInteractor.displayedIncentive,
            supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods(),
            createFormArguments = formHelper::createFormArguments,
            formElementsForCode = formHelper::formElementsForCode,
            clearErrorMessages = {},
            reportFieldInteraction = eventReporter::onPaymentMethodFormInteraction,
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportPromotionDisplayed = { code ->
                paymentMethodMessagePromotionsHelper.reportPromotionDisplayed(code, paymentMethodMetadata)
            },
            createUSBankAccountFormArguments = { code ->
                USBankAccountFormArguments.createForEmbedded(
                    paymentMethodMetadata = paymentMethodMetadata,
                    selectedPaymentMethodCode = code,
                    hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
                    isCompleteFlow = false,
                    draftPaymentSelection = null,
                    bankFormInteractor = bankFormInteractor,
                    hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.isNotEmpty(),
                    autocompleteAddressInteractorFactory = null,
                    onMandateTextChanged = { updatedMandate, _ -> mandate.value = updatedMandate },
                    onAnalyticsEvent = eventReporter::onUsBankAccountFormEvent,
                    onUpdatePrimaryButtonUIState = {},
                    onError = {},
                    onFormCompleted = {
                        eventReporter.onPaymentMethodFormCompleted(PaymentMethod.Type.USBankAccount.code)
                    },
                )
            },
            coroutineScope = interactorScope,
            uiContext = Dispatchers.Main,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = null,
                    isVerticalLayout = false,
                )
            },
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
        )

        return DefaultPaymentElementHorizontalContent(
            interactor = interactor,
            mandate = mandate,
            embeddedViewDisplaysMandateText = state.embeddedViewDisplaysMandateText,
            appearance = state.configuration.appearance,
            eventReporter = eventReporter,
            elementsSessionId = paymentMethodMetadata.elementsSessionId,
        )
    }
}

internal interface PaymentElementHorizontalContent : Closeable {
    @Composable
    fun Content()
}

internal class DefaultPaymentElementHorizontalContent(
    private val interactor: AddPaymentMethodInteractor,
    private val mandate: StateFlow<ResolvableString?>,
    private val embeddedViewDisplaysMandateText: Boolean,
    private val appearance: PaymentSheet.Appearance,
    private val eventReporter: EventReporter,
    private val elementsSessionId: String?,
) : PaymentElementHorizontalContent {
    @Composable
    override fun Content() {
        val currentMandate by mandate.collectAsState()
        EventReporterProvider(eventReporter, elementsSessionId) {
            PaymentElementTheme(appearance = appearance) {
                Column(Modifier.animateContentSize()) {
                    AddPaymentMethod(interactor = interactor)
                    EmbeddedMandate(
                        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                        mandate = currentMandate,
                    )
                }
            }
        }
    }

    override fun close() {
        interactor.close()
    }
}

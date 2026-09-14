package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal interface EmbeddedContentHelper {
    val embeddedContent: StateFlow<EmbeddedContent?>

    fun presentPaymentOptions()
}

internal class DefaultEmbeddedContentHelper @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val state: StateFlow<EmbeddedContentHelperStateHolder.State?>,
    private val verticalLayoutInteractorFactory: EmbeddedPaymentMethodVerticalLayoutInteractorFactory,
    private val embeddedWalletsHelper: EmbeddedWalletsHelper,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val paymentOptionsPresenter: EmbeddedPaymentOptionsPresenter,
) : EmbeddedContentHelper {

    private val _embeddedContent = MutableStateFlow<EmbeddedContent?>(null)
    override val embeddedContent: StateFlow<EmbeddedContent?> = _embeddedContent.asStateFlow()

    init {
        coroutineScope.launch {
            state.collect { state ->
                val replacement = state?.let { currentState ->
                    val isImmediateAction = internalRowSelectionCallback.get() != null
                    EmbeddedContent(
                        interactor = verticalLayoutInteractorFactory.create(
                            paymentMethodMetadata = currentState.paymentMethodMetadata,
                            configuration = currentState.configuration,
                            walletsState = embeddedWalletsHelper.walletsState(currentState.paymentMethodMetadata),
                            isImmediateAction = isImmediateAction,
                            embeddedViewDisplaysMandateText = currentState.embeddedViewDisplaysMandateText,
                        ),
                        embeddedViewDisplaysMandateText = currentState.embeddedViewDisplaysMandateText,
                        appearance = currentState.configuration.appearance,
                        isImmediateAction = isImmediateAction,
                    )
                }
                _embeddedContent.value?.close()
                _embeddedContent.value = replacement
            }
        }
    }

    override fun presentPaymentOptions() {
        paymentOptionsPresenter.present()
    }
}

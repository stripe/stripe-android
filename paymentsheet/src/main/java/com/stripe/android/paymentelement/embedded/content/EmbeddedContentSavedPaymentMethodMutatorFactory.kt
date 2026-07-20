package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class EmbeddedContentSavedPaymentMethodMutatorFactory @Inject constructor(
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
    @UIContext private val uiContext: CoroutineContext,
    private val savedPaymentMethodRepository: SavedPaymentMethodRepository,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val sheetLauncherHolder: EmbeddedSheetLauncherHolder,
) {
    fun create(paymentMethodMetadata: PaymentMethodMetadata): SavedPaymentMethodMutator {
        return SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            eventReporter = eventReporter,
            coroutineScope = coroutineScope,
            workContext = workContext,
            uiContext = uiContext,
            savedPaymentMethodRepository = savedPaymentMethodRepository,
            selection = selectionHolder.selection,
            setSelection = selectionHolder::setSelection,
            customerStateHolder = customerStateHolder,
            prePaymentMethodRemoveActions = {},
            postPaymentMethodRemoveActions = {},
            onUpdatePaymentMethod = { _, _, _, _, _ ->
                sheetLauncherHolder.sheetLauncher?.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                    configuration = confirmationStateHolder.state?.configuration,
                )
            },
            isLinkEnabled = stateFlowOf(paymentMethodMetadata.linkState != null),
            isNotPaymentFlow = false,
            accountLinkBrandFlow = linkAccountHolder.linkAccountInfo.mapAsStateFlow { it.account?.linkBrand },
        )
    }
}

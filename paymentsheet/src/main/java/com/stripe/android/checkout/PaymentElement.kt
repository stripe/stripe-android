package com.stripe.android.checkout

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodEmbeddedLayoutUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.image.rememberDrawablePainter
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.stateFlowOf
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement internal constructor(
    private val controller: CheckoutController,
    private val sheetLauncher: EmbeddedSheetLauncher,
) {

    @Composable
    fun PaymentOptionsContent() {
        val metadata by controller.paymentMethodMetadata.collectAsState()
        val currentMetadata = metadata ?: return

        val interactor = remember(currentMetadata) {
            createInteractor(currentMetadata)
        }

        Column {
            PaymentMethodEmbeddedLayoutUI(
                interactor = interactor,
                embeddedViewDisplaysMandateText = true,
                appearance = Embedded.default,
            )
        }
    }

    fun presentPaymentOptions() {
        // TODO: Launch payment options selection sheet.
    }

    fun clearPaymentOption() {
        controller.updateSelection(null)
    }

    @Suppress("LongMethod")
    private fun createInteractor(
        paymentMethodMetadata: PaymentMethodMetadata,
    ): PaymentMethodVerticalLayoutInteractor {
        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
            incentive = paymentMethodMetadata.paymentMethodIncentive,
        )
        val formHelper = controller.component.embeddedFormHelperFactory.create(
            coroutineScope = controller.coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = controller.component.eventReporter,
            selectionUpdater = { controller.updateSelection(it) },
            setAsDefaultMatchesSaveForFutureUse =
                FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            paymentMethodMessagePromotionsHelper = null,
        )
        val customerStateHolder = controller.customerStateHolder
        val embeddedConfirmationState = EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = paymentMethodMetadata,
            selection = controller.selectionFlow.value,
            configuration = controller.embeddedConfiguration
                ?: buildMinimalEmbeddedConfiguration(),
        )
        return DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = controller.isLoading,
            temporarySelection = MutableStateFlow(null),
            selection = controller.selectionFlow,
            paymentMethodIncentiveInteractor = paymentMethodIncentiveInteractor,
            formTypeForCode = { code -> formHelper.formTypeForCode(code) },
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            transitionToManageScreen = {
                val customerState = customerStateHolder.customer.value
                    ?: return@DefaultPaymentMethodVerticalLayoutInteractor
                sheetLauncher.launchManage(
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = customerState,
                    selection = controller.selectionFlow.value,
                    embeddedConfirmationState = embeddedConfirmationState,
                )
            },
            transitionToFormScreen = { code ->
                sheetLauncher.launchForm(
                    code = code,
                    paymentMethodMetadata = paymentMethodMetadata,
                    hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any {
                        it.type?.code == code
                    },
                    embeddedConfirmationState = embeddedConfirmationState,
                    customerState = customerStateHolder.customer.value,
                    promotion = null,
                )
            },
            paymentMethods = customerStateHolder.paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            canRemove = customerStateHolder.canRemove,
            canUpdateFullPaymentMethodDetails = customerStateHolder.canUpdateFullPaymentMethodDetails,
            walletsState = stateFlowOf(null),
            updateSelection = { selection, _ -> controller.updateSelection(selection) },
            isCurrentScreen = stateFlowOf(true),
            reportPaymentMethodTypeSelected = { },
            reportFormShown = { },
            onUpdatePaymentMethod = { },
            shouldUpdateVerticalModeSelection = { paymentMethodCode ->
                val requiresFormScreen = paymentMethodCode != null &&
                    formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
                !requiresFormScreen
            },
            displaysMandatesInFormScreen = false,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { _, _ -> },
            paymentMethodMessagePromotionsHelper = null,
        )
    }

    private fun buildMinimalEmbeddedConfiguration(): EmbeddedPaymentElement.Configuration {
        return EmbeddedPaymentElement.Configuration.Builder(
            merchantDisplayName = ""
        ).build()
    }

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class PaymentOptionDisplayData internal constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val imageLoader: suspend () -> Drawable,
        val label: String,
        val billingDetails: PaymentSheet.BillingDetails?,
        val paymentMethodType: String,
        val mandateText: AnnotatedString?,
    ) {
        private val iconDrawable: Drawable by lazy {
            DelegateDrawable(imageLoader)
        }

        val iconPainter: Painter
            @Composable
            get() = rememberDrawablePainter(iconDrawable)
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true

        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        )
    }
}

package com.stripe.android.paymentelement.embedded.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.verticalmode.EmbeddedMandateForPaymentMethod
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodEmbeddedLayoutUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.utils.collectAsState
import java.io.Closeable

@Immutable
internal data class EmbeddedContent(
    private val interactor: PaymentMethodVerticalLayoutInteractor,
    private val embeddedViewDisplaysMandateText: Boolean,
    private val appearance: PaymentSheet.Appearance,
    private val isImmediateAction: Boolean,
    private val preferFormInteractor: AddPaymentMethodInteractor?,
    private val onMorePaymentMethods: () -> Unit,
    private val eventReporter: EventReporter?,
    private val preferForm: Boolean,
) : Closeable {
    constructor(
        interactor: PaymentMethodVerticalLayoutInteractor,
        embeddedViewDisplaysMandateText: Boolean,
        appearance: PaymentSheet.Appearance,
        isImmediateAction: Boolean,
    ) : this(
        interactor = interactor,
        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        appearance = appearance,
        isImmediateAction = isImmediateAction,
        preferFormInteractor = null,
        onMorePaymentMethods = {},
        eventReporter = null,
        preferForm = false,
    )

    override fun close() {
        interactor.close()
        preferFormInteractor?.close()
    }

    @Suppress("LongMethod")
    @Composable
    fun Content() {
        val embeddedAppearance = appearance.embeddedAppearance

        /**
         * This validation is here because of a weird interaction with 2-Step Integration.
         *
         * If this were in configure or when state is set, then it would fail for the 1st instance of embedded
         * in the 2 step integration because a user would not set a rowSelectionBehavior on the 1st instance of embedded
         * because the 1st instance doesn't show a UI. However, the first instance still has to be configured so it will
         * fail unless the user sets an empty rowSelection ImmediateAction callback.
         *
         * Having validation here ensures that we only validate when the embedded content is shown.
         */
        LaunchedEffect(embeddedAppearance.style, isImmediateAction) {
            if (embeddedAppearance.style is Embedded.RowStyle.FlatWithDisclosure && !isImmediateAction) {
                throw IllegalArgumentException(
                    "EmbeddedPaymentElement.Builder.rowSelectionBehavior() must be set to ImmediateAction when using " +
                        "FlatWithDisclosure RowStyle. Use a different style or enable ImmediateAction " +
                        "rowSelectionBehavior"
                )
            }
        }

        PaymentElementTheme(appearance = appearance) {
            Column(
                modifier = Modifier
                    .animateContentSize()
            ) {
                val verticalState by interactor.state.collectAsState()
                if (preferFormInteractor != null) {
                    val preferredCode = preferFormInteractor.state.value.selectedPaymentMethodCode
                    EventReporterProvider(requireNotNull(eventReporter)) {
                        PreferFormUI(
                            interactor = preferFormInteractor,
                            showFooter = verticalState.displayedSavedPaymentMethod != null ||
                                verticalState.displayablePaymentMethods.any { it.code != preferredCode },
                            onMorePaymentMethods = onMorePaymentMethods,
                        )
                    }
                    EmbeddedMandateForPaymentMethod(
                        interactor = interactor,
                        paymentMethodCode = preferredCode,
                        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                    )
                } else {
                    val selectedPaymentMethodCode =
                        (verticalState.selection as? PaymentMethodVerticalLayoutInteractor.Selection.New)
                            ?.takeIf { preferForm }
                            ?.code
                    val displaySavedPaymentMethodOnly = preferForm &&
                        verticalState.selection?.isSaved == true
                    PaymentMethodEmbeddedLayoutUI(
                        interactor = interactor,
                        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                        appearance = embeddedAppearance,
                        displayedPaymentMethodCode = selectedPaymentMethodCode,
                        displaySavedPaymentMethodOnly = displaySavedPaymentMethodOnly,
                    )
                    val alternatives = verticalState.displayablePaymentMethods.filterNot {
                        it.syntheticCode == selectedPaymentMethodCode
                    }
                    val hasAlternativePaymentMethods = alternatives.isNotEmpty() ||
                        selectedPaymentMethodCode != null && verticalState.displayedSavedPaymentMethod != null
                    if ((selectedPaymentMethodCode != null || displaySavedPaymentMethodOnly) &&
                        hasAlternativePaymentMethods
                    ) {
                        Spacer(Modifier.height(16.dp))
                        VerticalModeMorePaymentMethodsFooter(
                            alternatives = alternatives,
                            enabled = !verticalState.isProcessing,
                            onClick = onMorePaymentMethods,
                        )
                    }
                }
            }
        }
    }
}

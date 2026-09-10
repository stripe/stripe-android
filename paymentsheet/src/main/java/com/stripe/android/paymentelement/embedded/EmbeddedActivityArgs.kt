package com.stripe.android.paymentelement.embedded

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.parcelize.Parcelize

/** Stable Parcelable carrier used by the activity contract and saved instance state. */
@Parcelize
internal data class EmbeddedActivityArgs(
    val paymentMethodMetadata: PaymentMethodMetadata,
    val configuration: EmbeddedPaymentElement.Configuration,
    val productUsage: Set<String>,
    val paymentElementCallbackIdentifier: String,
    val statusBarColor: Int?,
    val selection: PaymentSelection?,
    val previousNewSelections: Bundle,
    val customerState: CustomerState?,
    val promotions: List<PaymentMethodMessagePromotion>,
    val launchMode: EmbeddedLaunchMode,
    val presentationState: PresentationState,
) : Parcelable {
    internal enum class PresentationState {
        Loading,
        Ready,
    }

    companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"

        fun fromIntent(intent: Intent): EmbeddedActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_ARGS, EmbeddedActivityArgs::class.java)
            }
        }
    }
}

internal sealed interface EmbeddedActivityState {
    val appearance: PaymentSheet.Appearance

    data class LoadingPaymentOptions(
        val context: Context,
        val initialSelection: PaymentSelection?,
        val previousNewSelections: PreviousNewSelections,
        val customerState: CustomerState?,
    ) : EmbeddedActivityState {
        override val appearance: PaymentSheet.Appearance
            get() = context.configuration.appearance
    }

    sealed interface Ready : EmbeddedActivityState {
        val context: Context
        val initialSelection: PaymentSelection?
        val previousNewSelections: PreviousNewSelections
        val customerState: CustomerState?

        override val appearance: PaymentSheet.Appearance
            get() = context.configuration.appearance

        val launchMode: EmbeddedLaunchMode
            get() = when (this) {
                is Form -> EmbeddedLaunchMode.Form(selectedPaymentMethodCode)
                is Manage -> EmbeddedLaunchMode.Manage
                is PaymentOptions -> EmbeddedLaunchMode.PaymentOptions
            }

        val promotions: List<PaymentMethodMessagePromotion>
            get() = when (this) {
                is Form -> listOfNotNull(promotion)
                is Manage, is PaymentOptions -> emptyList()
            }

        data class Form(
            override val context: Context,
            val selectedPaymentMethodCode: String,
            override val initialSelection: PaymentSelection.New?,
            override val previousNewSelections: PreviousNewSelections,
            override val customerState: CustomerState?,
            val promotion: PaymentMethodMessagePromotion?,
        ) : Ready

        data class Manage(
            override val context: Context,
            override val initialSelection: PaymentSelection?,
            override val previousNewSelections: PreviousNewSelections,
            override val customerState: CustomerState,
        ) : Ready

        data class PaymentOptions(
            override val context: Context,
            override val initialSelection: PaymentSelection?,
            override val previousNewSelections: PreviousNewSelections,
            override val customerState: CustomerState?,
        ) : Ready
    }

    data class Context(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val configuration: EmbeddedPaymentElement.Configuration,
        val productUsage: Set<String>,
        val paymentElementCallbackIdentifier: String,
        val statusBarColor: Int?,
    )
}

internal fun EmbeddedActivityState.toArgs(): EmbeddedActivityArgs {
    val context: EmbeddedActivityState.Context
    val selection: PaymentSelection?
    val previousNewSelections: PreviousNewSelections
    val customerState: CustomerState?
    val promotions: List<PaymentMethodMessagePromotion>
    val launchMode: EmbeddedLaunchMode
    val presentationState: EmbeddedActivityArgs.PresentationState

    when (this) {
        is EmbeddedActivityState.LoadingPaymentOptions -> {
            context = this.context
            selection = initialSelection
            previousNewSelections = this.previousNewSelections
            customerState = this.customerState
            promotions = emptyList()
            launchMode = EmbeddedLaunchMode.PaymentOptions
            presentationState = EmbeddedActivityArgs.PresentationState.Loading
        }
        is EmbeddedActivityState.Ready -> {
            context = this.context
            selection = initialSelection
            previousNewSelections = this.previousNewSelections
            customerState = this.customerState
            promotions = this.promotions
            launchMode = this.launchMode
            presentationState = EmbeddedActivityArgs.PresentationState.Ready
        }
    }

    return EmbeddedActivityArgs(
        paymentMethodMetadata = context.paymentMethodMetadata,
        configuration = context.configuration,
        productUsage = context.productUsage,
        paymentElementCallbackIdentifier = context.paymentElementCallbackIdentifier,
        statusBarColor = context.statusBarColor,
        selection = selection,
        previousNewSelections = previousNewSelections.toBundle(),
        customerState = customerState,
        promotions = promotions,
        launchMode = launchMode,
        presentationState = presentationState,
    )
}

internal fun EmbeddedActivityArgs.toState(): EmbeddedActivityState? {
    val context = EmbeddedActivityState.Context(
        paymentMethodMetadata = paymentMethodMetadata,
        configuration = configuration,
        productUsage = productUsage,
        paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
        statusBarColor = statusBarColor,
    )
    val previousNewSelections = PreviousNewSelections.fromBundle(previousNewSelections)

    return when (presentationState) {
        EmbeddedActivityArgs.PresentationState.Loading -> toLoadingState(context, previousNewSelections)
        EmbeddedActivityArgs.PresentationState.Ready -> toReadyState(context, previousNewSelections)
    }
}

private fun EmbeddedActivityArgs.toLoadingState(
    context: EmbeddedActivityState.Context,
    previousNewSelections: PreviousNewSelections,
): EmbeddedActivityState.LoadingPaymentOptions? {
    return if (launchMode is EmbeddedLaunchMode.PaymentOptions && promotions.isEmpty()) {
        EmbeddedActivityState.LoadingPaymentOptions(
            context = context,
            initialSelection = selection,
            previousNewSelections = previousNewSelections,
            customerState = customerState,
        )
    } else {
        null
    }
}

private fun EmbeddedActivityArgs.toReadyState(
    context: EmbeddedActivityState.Context,
    previousNewSelections: PreviousNewSelections,
): EmbeddedActivityState.Ready? {
    return when (val launchMode = launchMode) {
        is EmbeddedLaunchMode.Form -> {
            val initialSelection = selection as? PaymentSelection.New
            if ((selection != null && initialSelection == null) || promotions.size > 1) {
                null
            } else {
                EmbeddedActivityState.Ready.Form(
                    context = context,
                    selectedPaymentMethodCode = launchMode.selectedPaymentMethodCode,
                    initialSelection = initialSelection,
                    previousNewSelections = previousNewSelections,
                    customerState = customerState,
                    promotion = promotions.singleOrNull(),
                )
            }
        }
        is EmbeddedLaunchMode.Manage -> {
            if (customerState == null || promotions.isNotEmpty()) {
                null
            } else {
                EmbeddedActivityState.Ready.Manage(
                    context = context,
                    initialSelection = selection,
                    previousNewSelections = previousNewSelections,
                    customerState = customerState,
                )
            }
        }
        is EmbeddedLaunchMode.PaymentOptions -> {
            if (promotions.isNotEmpty()) {
                null
            } else {
                EmbeddedActivityState.Ready.PaymentOptions(
                    context = context,
                    initialSelection = selection,
                    previousNewSelections = previousNewSelections,
                    customerState = customerState,
                )
            }
        }
    }
}

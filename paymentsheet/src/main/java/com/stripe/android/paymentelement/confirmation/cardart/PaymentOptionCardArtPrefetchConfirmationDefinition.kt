package com.stripe.android.paymentelement.confirmation.cardart

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent
import com.stripe.android.paymentsheet.PaymentOptionCardArtProvider
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class PaymentOptionCardArtPrefetchConfirmationDefinition @Inject constructor(
    private val imageLoader: StripeImageLoader,
    @PaymentOptionCardArtPrefetchScope private val coroutineScope: CoroutineScope,
    private val paymentOptionCardArtProvider: PaymentOptionCardArtProvider,
    @IOContext private val workContext: CoroutineContext,
    private val errorReporter: ErrorReporter,
) : ConfirmationDefinition<ConfirmationHandler.Option, Unit, Nothing, Nothing> {

    override val key: String = "PaymentOptionCardArtPrefetch"

    override fun option(confirmationOption: ConfirmationHandler.Option): ConfirmationHandler.Option? = null

    override fun canConfirm(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ): Boolean = false

    override suspend fun action(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<Nothing> {
        val error = IllegalStateException("CardArtPrefetchConfirmationDefinition should not be used for confirmation")
        errorReporter.report(UnexpectedErrorEvent.CARD_ART_PREFETCH_INVOKED_FOR_CONFIRMATION)
        return ConfirmationDefinition.Action.Fail(
            cause = error,
            message = "CardArtPrefetchConfirmationDefinition should not be used for confirmation".resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
        )
    }

    override fun launch(
        launcher: Unit,
        arguments: Nothing,
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        errorReporter.report(UnexpectedErrorEvent.CARD_ART_PREFETCH_INVOKED_FOR_CONFIRMATION)
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (Nothing) -> Unit,
    ): Unit = Unit

    override fun toResult(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: Nothing,
        result: Nothing,
    ): ConfirmationDefinition.Result {
        errorReporter.report(UnexpectedErrorEvent.CARD_ART_PREFETCH_INVOKED_FOR_CONFIRMATION)
        return ConfirmationDefinition.Result.NextStep(
            confirmationOption = confirmationOption,
            arguments = confirmationArgs,
        )
    }

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        val cardArts = paymentMethodMetadata.cardArts
        if (cardArts.isEmpty()) return

        for (cardArt in cardArts) {
            val paymentOptionImageUrl = paymentOptionCardArtProvider(cardArt) ?: continue
            coroutineScope.launch(workContext) {
                imageLoader.load(paymentOptionImageUrl)
            }
        }
    }
}

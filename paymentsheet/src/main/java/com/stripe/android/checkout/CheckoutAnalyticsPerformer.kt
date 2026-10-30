@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent
import com.stripe.android.checkout.analytics.CheckoutConfirmationContext
import com.stripe.android.checkout.analytics.CheckoutConfirmationContext.InvocationSource
import com.stripe.android.checkout.analytics.CheckoutConfirmationContext.LinkUi
import com.stripe.android.checkout.analytics.ConfirmOperation
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionState
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionType
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.toConfirmationError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class CheckoutAnalyticsPerformer @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val stateHolder: CheckoutControllerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    context: Context,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val durationProvider: DurationProvider,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
    @IOContext private val workContext: CoroutineContext,
) : CheckoutConfirmationResultReporter {
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context = context,
        defaultProductUsageTokens = setOf("checkout_elements"),
        apiConfigurationProvider = apiConfigurationProvider,
    )

    fun onPaymentElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(InvocationSource.Merchant, paymentSelection)
    }

    fun onExpressCheckoutElementConfirmationStarted(paymentSelection: PaymentSelection) {
        saveConfirmation(InvocationSource.ExpressCheckoutElement, paymentSelection)
    }

    override fun reportConfirmationResult(
        result: ConfirmationHandler.Result,
        response: CheckoutSessionResponse?,
    ) {
        val confirmationContext = savedStateHandle.get<CheckoutConfirmationContext>(CONFIRMATION_CONTEXT_KEY)
            ?: return
        savedStateHandle.remove<CheckoutConfirmationContext>(CONFIRMATION_CONTEXT_KEY)

        val operation = ConfirmOperation(confirmationContext, durationProvider, ::reportEvent)
        val sessionResponse = response ?: stateHolder.state?.checkoutSessionResponse

        when (result) {
            is ConfirmationHandler.Result.Succeeded -> operation.succeeded(sessionResponse)
            is ConfirmationHandler.Result.Failed -> operation.failed(
                sessionResponse,
                result.toConfirmationError() ?: PaymentSheetConfirmationError(result.cause),
            )
            is ConfirmationHandler.Result.Canceled -> operation.canceled(sessionResponse)
        }
    }

    private fun reportEvent(event: CheckoutAnalyticsEvent) {
        val publishableKey = stateHolder.state?.paymentMethodMetadata?.apiConfiguration?.publishableKey
            ?: apiConfigurationProvider.get().publishableKey
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.params,
                    publishableKeyOverride = publishableKey,
                )
            )
        }
    }

    private fun saveConfirmation(
        invocationSource: InvocationSource,
        paymentSelection: PaymentSelection,
    ) {
        val state = stateHolder.state
        val metadata = when (invocationSource) {
            InvocationSource.Merchant -> state?.paymentMethodMetadata
            InvocationSource.ExpressCheckoutElement -> state?.expressCheckoutElementPaymentMethodMetadata
        }
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
        val confirmationContext = CheckoutConfirmationContext(
            paymentSelection = paymentSelection,
            invocationSource = invocationSource,
            activeLinkSession = linkAccount?.isVerified
                ?: (metadata?.linkState?.loginState == LinkState.LoginState.LoggedIn),
            linkUi = paymentSelection.linkUi(linkAccount),
        )
        savedStateHandle[CONFIRMATION_CONTEXT_KEY] = confirmationContext
        durationProvider.start(DurationProvider.Key.CheckoutConfirmation)
        ConfirmOperation(confirmationContext, durationProvider, ::reportEvent).started(
            state?.checkoutSessionResponse
        )
    }

    private fun PaymentSelection.linkUi(linkAccount: LinkAccount?): LinkUi? = when (this) {
        is PaymentSelection.Link -> if (selectedPayment == null) {
            LinkUi.NativePopup
        } else {
            linkAccount?.let { account ->
                val hasVerifiedOtp = account.verificationSessions.any {
                    it.state == SessionState.Verified && (it.type == SessionType.Sms || it.type == SessionType.Email)
                }
                // Match the shared Link UI classification, including seamless sign-in without OTP.
                when {
                    hasVerifiedOtp -> LinkUi.NativeReturning
                    account.isVerified -> LinkUi.NativeSignup
                    else -> null
                }
            }
        }
        is PaymentSelection.New.Card -> LinkUi.InlineSignup.takeIf { linkInput is UserInput.SignUp }
        is PaymentSelection.Saved -> LinkUi.InlineSignup.takeIf { linkInput is UserInput.SignUp }
        else -> null
    }

    private companion object {
        const val CONFIRMATION_CONTEXT_KEY = "CheckoutAnalyticsPerformer.confirmationContext"
    }
}

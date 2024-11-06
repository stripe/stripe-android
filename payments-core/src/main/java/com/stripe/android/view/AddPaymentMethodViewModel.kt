package com.stripe.android.view

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.ApiResultCallback
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.Stripe
import com.stripe.android.analytics.PaymentSessionEventReporter
import com.stripe.android.analytics.PaymentSessionEventReporterFactory
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.core.StripeError
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.i18n.ErrorMessageTranslator
import com.stripe.android.view.i18n.TranslatorManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class AddPaymentMethodViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val stripe: Stripe,
    private val args: AddPaymentMethodActivityStarter.Args,
    private val errorMessageTranslator: ErrorMessageTranslator =
        TranslatorManager.getErrorMessageTranslator(),
    private val eventReporter: PaymentSessionEventReporter =
        PaymentSessionEventReporterFactory.create(application)
) : AndroidViewModel(application) {

    private val productUsage: Set<String> = listOfNotNull(
        AddPaymentMethodActivity.PRODUCT_TOKEN,
        PaymentSession.PRODUCT_TOKEN.takeIf { args.isPaymentSessionActive }
    ).toSet()

    private var formShownEventReported: Boolean
        get() = savedStateHandle.get<Boolean>(FORM_SHOWN_EVENT_REPORTED_KEY) ?: false
        set(value) {
            savedStateHandle[FORM_SHOWN_EVENT_REPORTED_KEY] = value
        }

    private var formInteractedEventReported: Boolean
        get() = savedStateHandle.get<Boolean>(FORM_INTERACTED_EVENT_REPORTED_KEY) ?: false
        set(value) {
            savedStateHandle[FORM_INTERACTED_EVENT_REPORTED_KEY] = value
        }

    init {
        SessionSavedStateHandler.attachTo(this, savedStateHandle)

        if (!formShownEventReported) {
            eventReporter.onFormShown(args.paymentMethodType.code)
            formShownEventReported = true
        }
    }

    internal suspend fun createPaymentMethod(
        params: PaymentMethodCreateParams
    ): Result<PaymentMethod> = suspendCoroutine { continuation ->
        stripe.createPaymentMethod(
            paymentMethodCreateParams = updatedPaymentMethodCreateParams(params),
            callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    continuation.resume(Result.success(result))
                }

                override fun onError(e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        )
    }

    @VisibleForTesting
    internal fun updatedPaymentMethodCreateParams(
        params: PaymentMethodCreateParams
    ) = params.copy(productUsage = productUsage)

    @JvmSynthetic
    internal suspend fun attachPaymentMethod(
        customerSession: CustomerSession,
        paymentMethod: PaymentMethod
    ): Result<PaymentMethod> = suspendCoroutine { continuation ->
        customerSession.attachPaymentMethod(
            paymentMethodId = paymentMethod.id.orEmpty(),
            productUsage = productUsage,
            listener = object : CustomerSession.PaymentMethodRetrievalListener {
                override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
                    continuation.resume(Result.success(paymentMethod))
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    continuation.resume(
                        Result.failure(
                            RuntimeException(
                                errorMessageTranslator.translate(
                                    errorCode,
                                    errorMessage,
                                    stripeError
                                )
                            )
                        )
                    )
                }
            }
        )
    }

    internal fun onFormShown() {
        if (!formShownEventReported) {
            eventReporter.onFormShown(args.paymentMethodType.code)
            formShownEventReported = true
        }
    }

    internal fun onFormInteracted() {
        if (!formInteractedEventReported) {
            eventReporter.onFormInteracted(args.paymentMethodType.code)
            formInteractedEventReported = true
        }
    }

    internal fun onCardNumberCompleted() {
        eventReporter.onCardNumberCompleted()
    }

    internal fun onSaveClicked() {
        eventReporter.onDoneButtonTapped(args.paymentMethodType.code)
    }

    internal class Factory(
        private val stripe: Stripe,
        private val args: AddPaymentMethodActivityStarter.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return AddPaymentMethodViewModel(
                application = extras.requireApplication(),
                savedStateHandle = extras.createSavedStateHandle(),
                stripe = stripe,
                args = args
            ) as T
        }
    }

    private companion object {
        private const val FORM_SHOWN_EVENT_REPORTED_KEY = "FROM_SHOWN_EVENT_REPORTED"
        private const val FORM_INTERACTED_EVENT_REPORTED_KEY = "FROM_INTERACTED_EVENT_REPORTED"
    }
}

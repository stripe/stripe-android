package com.stripe.android.payments.core.analytics

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.frauddetection.FraudDetectionErrorReporter
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.utils.filterNotNullValues
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ErrorReporter : FraudDetectionErrorReporter {

    fun report(
        errorEvent: ErrorEvent,
        stripeException: StripeException? = null,
        additionalNonPiiParams: Map<String, String> = emptyMap(),
    )

    override fun reportFraudDetectionError(error: StripeException) {
        report(
            errorEvent = ExpectedErrorEvent.FRAUD_DETECTION_API_FAILURE,
            stripeException = error,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Prefer using an injected version of [ErrorReporter].
         *
         * This should only be used if you don't already have access to a dagger component.
         */
        fun createFallbackInstance(
            context: Context,
            productUsage: Set<String> = emptySet(),
        ): ErrorReporter {
            return DaggerDefaultErrorReporterComponent
                .builder()
                .context(context.applicationContext)
                .productUsage(productUsage)
                .build()
                .errorReporter
        }

        fun getAdditionalParamsFromError(error: Throwable): Map<String, String> {
            return when (error) {
                is StripeException -> getAdditionalParamsFromStripeException(error)
                else -> getAdditionalParamsFromStripeException(StripeException.create(error))
            }
        }

        fun getAdditionalParamsFromStripeException(stripeException: StripeException): Map<String, String> {
            val statusCode =
                if (stripeException.statusCode == StripeException.DEFAULT_STATUS_CODE) {
                    null
                } else {
                    stripeException.statusCode
                }
            return mapOf(
                "analytics_value" to stripeException.analyticsValue(),
                "status_code" to statusCode?.toString(),
                "request_id" to stripeException.requestId,
                "error_type" to stripeException.stripeError?.type,
                "error_code" to stripeException.stripeError?.code,
            ).filterNotNullValues()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface ErrorEvent : AnalyticsEvent

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ExpectedErrorEvent(override val eventName: String) : ErrorEvent {
        AUTH_WEB_VIEW_FAILURE(
            eventName = "payments.auth_web_view.failure"
        ),
        AUTH_WEB_VIEW_NULL_ARGS(
            eventName = "payments.auth_web_view.null_args"
        ),
        GET_SAVED_PAYMENT_METHODS_FAILURE(
            eventName = "elements.customer_repository.get_saved_payment_methods_failure"
        ),
        GOOGLE_PAY_IS_READY_API_CALL(
            eventName = "elements.google_pay_repository.is_ready_request_api_call_failure"
        ),
        CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_FAILURE(
            eventName = "elements.customer_sheet.elements_session.load_failure"
        ),
        CUSTOMER_SHEET_CUSTOMER_SESSION_ELEMENTS_SESSION_LOAD_FAILURE(
            eventName = "elements.customer_sheet.customer_session.elements_session.load_failure"
        ),
        CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE(
            eventName = "elements.customer_sheet.payment_methods.load_failure"
        ),
        CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_FAILURE(
            eventName = "elements.customer_sheet.payment_methods.refresh_failure"
        ),
        CUSTOMER_SHEET_ADAPTER_NOT_FOUND(
            eventName = "elements.customer_sheet.customer_adapter.not_found"
        ),
        PLACES_FIND_AUTOCOMPLETE_ERROR(
            eventName = "address_element.find_autocomplete.error"
        ),
        PLACES_FETCH_PLACE_ERROR(
            eventName = "address_element.fetch_place.error"
        ),
        LINK_CREATE_CARD_FAILURE(
            eventName = "link.create_new_card.create_payment_details_failure"
        ),
        LINK_SHARE_CARD_FAILURE(
            eventName = "link.create_new_card.share_payment_details_failure"
        ),
        LINK_LOG_OUT_FAILURE(
            eventName = "link.log_out.failure"
        ),
        LINK_NATIVE_FAILED_TO_GET_INTEGRITY_TOKEN(
            eventName = "link.native.failed_to_get_integrity_token"
        ),
        LINK_NATIVE_FAILED_TO_ATTEST_REQUEST(
            eventName = "link.native.failed_to_attest_request"
        ),
        LINK_NATIVE_FAILED_TO_PREPARE_INTEGRITY_MANAGER(
            eventName = "link.native.integrity.preparation_failed"
        ),
        PAYMENT_LAUNCHER_CONFIRMATION_NULL_ARGS(
            eventName = "payments.paymentlauncherconfirmation.null_args"
        ),
        BROWSER_LAUNCHER_ACTIVITY_NOT_FOUND(
            eventName = "payments.browserlauncher.activity_not_found"
        ),
        BROWSER_LAUNCHER_NULL_ARGS(
            eventName = "payments.browserlauncher.null_args"
        ),
        GOOGLE_PAY_FAILED(
            eventName = "google_pay.confirm.error"
        ),
        FRAUD_DETECTION_API_FAILURE(
            eventName = "fraud_detection_data_repository.api_failure"
        ),
        EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER_NULL(
            eventName = "paymentsheet.external_payment_method.confirm_handler_is_null"
        ),
        CUSTOM_PAYMENT_METHOD_CONFIRM_HANDLER_NULL(
            eventName = "paymentsheet.custom_payment_method.confirm_handler_is_null"
        ),
        EXTERNAL_PAYMENT_METHOD_LAUNCHER_NULL(
            eventName = "paymentsheet.external_payment_method.launcher_is_null"
        ),
        CREATE_INTENT_CALLBACK_NULL(
            eventName = "paymentsheet.create_intent_callback.is_null"
        ),
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class UnexpectedErrorEvent(val partialEventName: String) : ErrorEvent {
        AUTH_WEB_VIEW_BLANK_CLIENT_SECRET(
            partialEventName = "payments.auth_web_view.blank_client_secret"
        ),
        MISSING_CARDSCAN_DEPENDENCY(
            partialEventName = "cardscan.missing_dependency"
        ),
        MISSING_HOSTED_VOUCHER_URL(
            partialEventName = "payments.missing_hosted_voucher_url"
        ),
        MISSING_POLLING_AUTHENTICATOR(
            partialEventName = "payments.missing_polling_authenticator"
        ),
        LINK_INVALID_SESSION_STATE(
            partialEventName = "link.signup.failure.invalidSessionState"
        ),
        GOOGLE_PAY_JSON_REQUEST_PARSING(
            partialEventName = "google_pay_repository.is_ready_request_json_parsing_failure"
        ),
        GOOGLE_PAY_UNEXPECTED_CONFIRM_RESULT(
            partialEventName = "google_pay.confirm.unexpected_result"
        ),
        GOOGLE_PAY_MISSING_INTENT_DATA(
            partialEventName = "google_pay.on_result.missing_data"
        ),
        FIND_AUTOCOMPLETE_PREDICTIONS_WITHOUT_DEPENDENCY(
            partialEventName = "address_element.find_autocomplete.without_dependency"
        ),
        FETCH_PLACE_WITHOUT_DEPENDENCY(
            partialEventName = "address_element.fetch_place.without_dependency",
        ),
        LINK_ATTACH_CARD_WITH_NULL_ACCOUNT(
            partialEventName = "link.create_new_card.missing_link_account"
        ),
        LINK_WEB_FAILED_TO_PARSE_RESULT_URI(
            partialEventName = "link.web.result.parsing_failed"
        ),
        LINK_NATIVE_FAILED_TO_ATTEST_SIGNUP_REQUEST(
            partialEventName = "link.native.signup.failed_to_attest_request"
        ),
        PAYMENT_SHEET_AUTHENTICATORS_NOT_FOUND(
            partialEventName = "paymentsheet.authenticators.not_found"
        ),
        PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND(
            partialEventName = "paymentsheet.loader.elements_session.customer.not_found"
        ),
        EXTERNAL_PAYMENT_METHOD_SERIALIZATION_FAILURE(
            partialEventName = "elements.external_payment_methods_serializer.error"
        ),
        PAYMENT_SHEET_NO_PAYMENT_SELECTION_ON_CHECKOUT(
            partialEventName = "paymentsheet.no_payment_selection"
        ),
        PAYMENT_SHEET_INVALID_PAYMENT_SELECTION_ON_CHECKOUT(
            partialEventName = "paymentsheet.invalid_payment_selection"
        ),
        FLOW_CONTROLLER_INVALID_PAYMENT_SELECTION_ON_CHECKOUT(
            partialEventName = "flow_controller.invalid_payment_selection"
        ),
        INTENT_CONFIRMATION_HANDLER_INVALID_PAYMENT_CONFIRMATION_OPTION(
            partialEventName = "intent_confirmation_handler.invalid_payment_confirmation_option"
        ),
        EXTERNAL_PAYMENT_METHOD_UNEXPECTED_RESULT_CODE(
            partialEventName = "paymentsheet.external_payment_method.unexpected_result_code"
        ),
        CVC_RECOLLECTION_UNEXPECTED_PAYMENT_SELECTION(
            partialEventName = "payments.cvc_recollection_unexpected_payment_selection"
        ),
        CUSTOMER_SHEET_ATTACH_CALLED_WITH_CUSTOMER_SESSION(
            partialEventName = "customersheet.customer_session.attach_called"
        ),
        CUSTOMER_SESSION_ON_CUSTOMER_SHEET_ELEMENTS_SESSION_NO_CUSTOMER_FIELD(
            partialEventName = "customersheet.customer_session.elements_session.no_customer_field"
        ),
        EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL(
            partialEventName = "embedded.embedded_sheet_launcher.embedded_state_is_null"
        ),
        ;

        override val eventName: String
            get() = "unexpected_error.$partialEventName"
    }

    /**
     * Success events that correspond to an [ExpectedErrorEvent].
     *
     * This exists so that we can compute failure rates for [ExpectedErrorEvent]s to enable adding meaningful alerts.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class SuccessEvent(override val eventName: String) : ErrorEvent {
        CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_SUCCESS(
            eventName = "elements.customer_sheet.elements_session.load_success"
        ),
        CUSTOMER_SHEET_CUSTOMER_SESSION_ELEMENTS_SESSION_LOAD_SUCCESS(
            eventName = "elements.customer_sheet.customer_session.elements_session.load_success"
        ),
        CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_SUCCESS(
            eventName = "elements.customer_sheet.payment_methods.load_success"
        ),
        GET_SAVED_PAYMENT_METHODS_SUCCESS(
            eventName = "elements.customer_repository.get_saved_payment_methods_success"
        ),
        PLACES_FIND_AUTOCOMPLETE_SUCCESS(
            eventName = "address_element.find_autocomplete.success"
        ),
        PLACES_FETCH_PLACE_SUCCESS(
            eventName = "address_element.fetch_place.success"
        ),
        LINK_CREATE_CARD_SUCCESS(
            eventName = "link.create_new_card.success"
        ),
        LINK_LOG_OUT_SUCCESS(
            eventName = "link.log_out.success"
        ),
        CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_SUCCESS(
            eventName = "elements.customer_sheet.payment_methods.refresh_success"
        ),
        EXTERNAL_PAYMENT_METHODS_LAUNCH_SUCCESS(
            eventName = "paymentsheet.external_payment_method.launch_success"
        ),
        CUSTOM_PAYMENT_METHODS_LAUNCH_SUCCESS(
            eventName = "paymentsheet.custom_payment_method.launch_success"
        ),
        FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING(
            eventName = "paymentsheet.polling_for_create_intent_callback.found"
        ),
    }
}

@Component(modules = [DefaultErrorReporterModule::class])
internal interface DefaultErrorReporterComponent {
    val errorReporter: ErrorReporter

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): DefaultErrorReporterComponent
    }
}

@Module
internal interface DefaultErrorReporterModule {
    @Binds
    fun bindRealErrorReporter(
        errorReporter: RealErrorReporter
    ): ErrorReporter

    @Binds
    fun providesAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    @Binds
    fun providePaymentAnalyticsRequestFactory(
        requestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    companion object {
        @Provides
        fun provideLogger(): Logger {
            return Logger.getInstance(BuildConfig.DEBUG)
        }

        @Provides
        @IOContext
        fun provideIoContext(): CoroutineContext {
            return Dispatchers.IO
        }

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(context: Context): () -> String {
            return { PaymentConfiguration.getInstance(context).publishableKey }
        }
    }
}

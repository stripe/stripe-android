package com.stripe.android.payments.paymentlauncher

import android.app.Application
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.payments.core.authentication.PaymentNextActionHandlerRegistry
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.view.AuthActivityStarterHost

internal object PaymentLauncherViewModelTestFakes {
    internal class FakeStripeRepository : AbsFakeStripeRepository() {
        var confirmPaymentIntentResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        var confirmSetupIntentResult = Result.success(SetupIntentFixtures.SI_SUCCEEDED)
        var retrieveStripeIntentResult: Result<StripeIntent> =
            Result.success(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
        var retrievePaymentIntentResult = Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
        var retrieveSetupIntentResult = Result.success(SetupIntentFixtures.SI_SUCCEEDED)

        val confirmPaymentIntentCalls = Turbine<ConfirmPaymentIntentCall>()
        val confirmSetupIntentCalls = Turbine<ConfirmSetupIntentCall>()
        val retrieveStripeIntentCalls = Turbine<RetrieveIntentCall>()
        val retrievePaymentIntentCalls = Turbine<RetrieveIntentCall>()
        val retrieveSetupIntentCalls = Turbine<RetrieveIntentCall>()

        override suspend fun confirmPaymentIntent(
            confirmPaymentIntentParams: ConfirmPaymentIntentParams,
            options: ApiRequest.Options,
            expandFields: List<String>,
        ): Result<PaymentIntent> {
            confirmPaymentIntentCalls.add(ConfirmPaymentIntentCall(confirmPaymentIntentParams, options, expandFields))
            return confirmPaymentIntentResult
        }

        override suspend fun confirmSetupIntent(
            confirmSetupIntentParams: ConfirmSetupIntentParams,
            options: ApiRequest.Options,
            expandFields: List<String>,
        ): Result<SetupIntent> {
            confirmSetupIntentCalls.add(ConfirmSetupIntentCall(confirmSetupIntentParams, options, expandFields))
            return confirmSetupIntentResult
        }

        override suspend fun retrieveStripeIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>,
        ): Result<StripeIntent> {
            retrieveStripeIntentCalls.add(RetrieveIntentCall(clientSecret, options, expandFields))
            return retrieveStripeIntentResult
        }

        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>,
        ): Result<PaymentIntent> {
            retrievePaymentIntentCalls.add(RetrieveIntentCall(clientSecret, options, expandFields))
            return retrievePaymentIntentResult
        }

        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>,
        ): Result<SetupIntent> {
            retrieveSetupIntentCalls.add(RetrieveIntentCall(clientSecret, options, expandFields))
            return retrieveSetupIntentResult
        }

        fun ensureAllEventsConsumed() {
            confirmPaymentIntentCalls.ensureAllEventsConsumed()
            confirmSetupIntentCalls.ensureAllEventsConsumed()
            retrieveStripeIntentCalls.ensureAllEventsConsumed()
            retrievePaymentIntentCalls.ensureAllEventsConsumed()
            retrieveSetupIntentCalls.ensureAllEventsConsumed()
        }

        data class ConfirmPaymentIntentCall(
            val params: ConfirmPaymentIntentParams,
            val options: ApiRequest.Options,
            val expandFields: List<String>,
        )

        data class ConfirmSetupIntentCall(
            val params: ConfirmSetupIntentParams,
            val options: ApiRequest.Options,
            val expandFields: List<String>,
        )

        data class RetrieveIntentCall(
            val clientSecret: String,
            val options: ApiRequest.Options,
            val expandFields: List<String>,
        )
    }

    internal class FakeNextActionHandlerRegistry : PaymentNextActionHandlerRegistry {
        val handler = FakeNextActionHandler()
        val getNextActionHandlerCalls = Turbine<Any?>()
        val registrationCalls = Turbine<RegistrationCall>()
        val invalidationCalls = Turbine<Unit>()

        @Suppress("UNCHECKED_CAST")
        override fun <Actionable> getNextActionHandler(actionable: Actionable): PaymentNextActionHandler<Actionable> {
            getNextActionHandlerCalls.add(actionable)
            return handler as PaymentNextActionHandler<Actionable>
        }

        override fun onNewActivityResultCaller(
            activityResultCaller: ActivityResultCaller,
            activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>,
        ) {
            registrationCalls.add(RegistrationCall(activityResultCaller, activityResultCallback))
        }

        override fun onLauncherInvalidated() {
            invalidationCalls.add(Unit)
        }

        fun ensureAllEventsConsumed() {
            handler.ensureAllEventsConsumed()
            getNextActionHandlerCalls.ensureAllEventsConsumed()
            registrationCalls.ensureAllEventsConsumed()
            invalidationCalls.ensureAllEventsConsumed()
        }

        data class RegistrationCall(
            val caller: ActivityResultCaller,
            val callback: ActivityResultCallback<PaymentFlowResult.Unvalidated>,
        )
    }

    internal class FakeNextActionHandler : PaymentNextActionHandler<StripeIntent>() {
        val nextActionCalls = Turbine<NextActionCall>()
        val registrationCalls = Turbine<FakeNextActionHandlerRegistry.RegistrationCall>()
        val invalidationCalls = Turbine<Unit>()

        override suspend fun performNextActionOnResumed(
            host: AuthActivityStarterHost,
            actionable: StripeIntent,
            requestOptions: ApiRequest.Options,
        ) {
            nextActionCalls.add(NextActionCall(host, actionable, requestOptions))
        }

        override fun onNewActivityResultCaller(
            activityResultCaller: ActivityResultCaller,
            activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>,
        ) {
            registrationCalls.add(
                FakeNextActionHandlerRegistry.RegistrationCall(activityResultCaller, activityResultCallback)
            )
        }

        override fun onLauncherInvalidated() {
            invalidationCalls.add(Unit)
        }

        fun ensureAllEventsConsumed() {
            nextActionCalls.ensureAllEventsConsumed()
            registrationCalls.ensureAllEventsConsumed()
            invalidationCalls.ensureAllEventsConsumed()
        }

        data class NextActionCall(
            val host: AuthActivityStarterHost,
            val intent: StripeIntent,
            val options: ApiRequest.Options,
        )
    }

    internal class FakeAuthActivityStarterHost : AuthActivityStarterHost {
        val startActivityCalls = Turbine<StartActivityCall>()
        override val statusBarColor: Int? = null
        override val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        override val application: Application = ApplicationProvider.getApplicationContext()

        override fun startActivityForResult(target: Class<*>, extras: Bundle, requestCode: Int) {
            startActivityCalls.add(StartActivityCall(target, extras, requestCode))
        }

        fun ensureAllEventsConsumed() {
            startActivityCalls.ensureAllEventsConsumed()
        }

        data class StartActivityCall(val target: Class<*>, val extras: Bundle, val requestCode: Int)
    }

    internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val requests = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            requests.add(request)
        }

        fun ensureAllEventsConsumed() {
            requests.ensureAllEventsConsumed()
        }
    }
}

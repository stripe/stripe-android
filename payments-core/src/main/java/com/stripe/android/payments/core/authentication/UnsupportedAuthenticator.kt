package com.stripe.android.payments.core.authentication

import com.stripe.android.PaymentRelayStarter
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.getRequestCode
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PaymentAuthenticator] to return if there is no available authenticators. Informs the correct
 * dependency to include for that authenticator.
 */
@Singleton
@JvmSuppressWildcards
internal class UnsupportedAuthenticator @Inject constructor(
    private val paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter
) : PaymentAuthenticator<StripeIntent>() {
    override suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val exception = authenticatable.nextActionData?.let {
            val nextActionType = it::class.java
            StripeException.create(
                IllegalArgumentException(
                    "${nextActionType.simpleName} type is not supported, add " +
                        "${ACTION_DEPENDENCY_MAP[nextActionType]} in build.gradle to support it"
                )
            )
        } ?: run {
            StripeException.create(
                IllegalArgumentException("stripeIntent.nextActionData is null")
            )
        }

        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.ErrorArgs(
                    exception = exception,
                    requestCode = authenticatable.getRequestCode()
                )
            )
    }

    internal companion object {
        val ACTION_DEPENDENCY_MAP = mapOf<Class<out StripeIntent.NextActionData>, String>(
            StripeIntent.NextActionData.WeChatPayRedirect::class.java
                to "com.stripe:stripe-wechatpay:${StripeSdkVersion.VERSION_NAME}"
        )
    }
}

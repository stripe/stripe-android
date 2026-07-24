package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import dagger.Module
import dagger.Provides

/**
 * A [Module] that provides [ApiRequest.Options] from [ApiConfiguration.State].
 * Use this in components that have [ApiConfiguration.State] bound but do not include
 * [PaymentConfigurationModule].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object ApiRequestOptionsModule {
    @Provides
    fun providesApiRequestOptions(
        apiConfiguration: ApiConfiguration.State
    ): ApiRequest.Options = ApiRequest.Options(
        apiKey = apiConfiguration.publishableKey,
        stripeAccount = apiConfiguration.stripeAccountId,
    )
}
